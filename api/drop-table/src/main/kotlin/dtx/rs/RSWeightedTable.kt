package dtx.rs

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.RollOverrides
import dtx.impl.chance.RateBoosts
import dtx.impl.weighted.WeightedRollable
import dtx.impl.weighted.WeightedTable
import dtx.table.TableHooks
import kotlin.math.floor
import kotlin.random.Random

public class RSWeightedTable<T, R>(
    public override val tableIdentifier: String,
    public override val tableEntries: Collection<WeightedRollable<T, R>>,
    private val hooks: TableHooks<T, R> = TableHooks.Default(),
    public val inlineSeparateRolls: List<InlineSeparateRoll<T, R>> = emptyList(),
) : RSTable<T, R>, WeightedTable<T, R>, TableHooks<T, R> by hooks {

    override fun selectEntries(byTarget: T, otherArgs: ArgMap): List<RSWeightEntry<T, R>> = buildList {
        val included = tableEntries.filter { it.includeInRoll(byTarget, otherArgs) }
        val weights = effectiveWeights(included, otherArgs[RollOverrides.probability])
        var total = 0

        included.forEachIndexed { index, it ->
            val upper = total + weights[index]
            val entry = RSWeightEntry(total, upper.toInt(), it.rollable, it.boosted)
            total = entry.rangeEnd
            add(entry)
        }
    }

    /**
     * Overridden entries get exactly their requested share of [OVERRIDE_TOTAL]; the remaining
     * entries split what is left in proportion to their own weights.
     */
    private fun effectiveWeights(
        entries: List<WeightedRollable<T, R>>,
        override: ((Any) -> Double?)?,
    ): List<Double> {
        val probabilities = entries.map { override?.invoke(it)?.coerceIn(0.0, 1.0) }
        if (probabilities.all { it == null }) {
            return entries.map { it.weight }
        }
        val fixed = probabilities.sumOf { it ?: 0.0 }
        val free = entries.indices.filter { probabilities[it] == null }.sumOf { entries[it].weight }
        val remaining = (1.0 - fixed).coerceAtLeast(0.0)
        return entries.mapIndexed { index, entry ->
            val probability = probabilities[index]
            when {
                probability != null && (free <= 0.0 || fixed > 1.0) ->
                    OVERRIDE_TOTAL * probability / fixed.coerceAtLeast(MIN_WEIGHT)
                probability != null -> OVERRIDE_TOTAL * probability
                free <= 0.0 -> 0.0
                else -> OVERRIDE_TOTAL * remaining * entry.weight / free
            }
        }
    }

    public override val maxRoll: Double
        get() = tableEntries.maxOf { it.weight }

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {
        val entries = selectEntries(target, otherArgs)

        if (tableEntries.isEmpty()) {
            return RollResult.Nothing()
        }

        if (tableEntries.size == 1) {
            return tableEntries.first().roll(target, otherArgs)
        }

        val localMax = entries.maxOf { it.rangeEnd }

        if (entries.any { it.boosted }) {
            val multiplier = RateBoosts.multiplierFor(target, otherArgs)
            if (multiplier != 1.0) {
                return selectBoosted(target, otherArgs, entries, localMax, multiplier)
            }
        }

        val baseRoll = Random.nextInt(0, localMax)
        val flatMod = rollModifier(target, 0.0)

        val roll = (baseRoll * flatMod).toInt()

        entries.forEach { entry ->
            if (entry checkWeight roll) {
                return entry.roll(target, otherArgs)
            }
        }

        return RollResult.Nothing()
    }

    private fun selectBoosted(
        target: T,
        otherArgs: ArgMap,
        entries: List<RSWeightEntry<T, R>>,
        total: Int,
        multiplier: Double,
    ): RollResult<R> {
        val roll = Random.nextDouble()
        var cumulative = 0.0
        for (entry in entries) {
            if (!entry.boosted) continue
            val boostedWeight = floor(total / (entry.weight * multiplier).coerceAtLeast(MIN_WEIGHT))
            cumulative += 1.0 / boostedWeight.coerceAtLeast(1.0)
            if (roll < cumulative) {
                return entry.roll(target, otherArgs)
            }
        }

        val rest = entries.filter { !it.boosted }
        val restTotal = rest.sumOf { it.rangeEnd - it.rangeStart }
        if (restTotal <= 0) {
            return RollResult.Nothing()
        }
        var pick = Random.nextInt(restTotal)
        for (entry in rest) {
            pick -= entry.rangeEnd - entry.rangeStart
            if (pick < 0) {
                return entry.roll(target, otherArgs)
            }
        }
        return RollResult.Nothing()
    }

    public companion object {
        private const val MIN_WEIGHT = 0.0001
        private const val OVERRIDE_TOTAL = 1_000_000.0
        private val EmptyTable = RSWeightedTable<Any?, Any?>("", emptyList())

        @Suppress("UNCHECKED_CAST")
        public fun <T, R> Empty(): RSWeightedTable<T, R> = EmptyTable as RSWeightedTable<T, R>
    }
}
