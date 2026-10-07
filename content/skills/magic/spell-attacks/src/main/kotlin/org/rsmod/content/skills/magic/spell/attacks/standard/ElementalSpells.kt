package org.rsmod.content.skills.magic.spell.attacks.standard

import dev.openrune.types.ItemServerType
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.magicLvl
import org.rsmod.api.spells.attack.SpellAttack
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackMap
import org.rsmod.api.spells.attack.SpellAttackRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getOrNull

private const val HALF_SPOTANIM_SUFFIX = "_half"

/** Wind, water, earth, fire: the order of each tier's spells and of the bombardment projectiles. */
private val ELEMENTS = listOf("wind", "water", "earth", "fire")

/** One projectile path per element, so a bombardment's four spells fly visibly apart. */
private val BOMBARDMENT_PROJANIMS =
    listOf("projanim.magic_spell", "projanim.magic_spell_double", "projanim.magic_spell_bombard_low", "projanim.magic_spell_bombard_high")

class ElementalSpells : SpellAttackMap {
    private class Tier(
        val name: String,
        val spells: List<String>,
        val staffAnim: String,
        val unarmedAnim: String,
        val maxHit: (magicLvl: Int) -> Int,
    )

    /** Graphics and sounds of one element's spell in a tier, named `<element><tier>_*` in the cache. */
    private class ElementFx(spell: String) {
        val launch = "spotanim.${spell}_casting"
        val travel = "spotanim.${spell}_travel"
        val impact = "spotanim.${spell}_impact"
        val castSound = "synth.${spell}_cast_and_fire"
        val hitSound = "synth.${spell}_hit"
    }

    private val tiers =
        listOf(
            Tier(
                name = "strike",
                spells = listOf("obj.01_wind_strike", "obj.05_water_strike", "obj.09_earth_strike", "obj.13_fire_strike"),
                staffAnim = "seq.human_caststrike_staff",
                unarmedAnim = "seq.human_caststrike",
            ) { lvl ->
                when {
                    lvl >= 13 -> 8
                    lvl >= 9 -> 6
                    lvl >= 5 -> 4
                    else -> 2
                }
            },
            Tier(
                name = "bolt",
                spells = listOf("obj.17_wind_bolt", "obj.23_water_bolt", "obj.29_earth_bolt", "obj.35_fire_bolt"),
                staffAnim = "seq.human_caststrike_staff",
                unarmedAnim = "seq.human_caststrike",
            ) { lvl ->
                when {
                    lvl >= 35 -> 12
                    lvl >= 29 -> 11
                    lvl >= 23 -> 10
                    else -> 9
                }
            },
            Tier(
                name = "blast",
                spells = listOf("obj.41_wind_blast", "obj.47_water_blast", "obj.53_earth_blast", "obj.59_fire_blast"),
                staffAnim = "seq.human_caststrike_staff",
                unarmedAnim = "seq.human_caststrike",
            ) { lvl ->
                when {
                    lvl >= 59 -> 16
                    lvl >= 53 -> 15
                    lvl >= 47 -> 14
                    else -> 13
                }
            },
            Tier(
                name = "wave",
                spells = listOf("obj.62_wind_wave", "obj.65_water_wave", "obj.70_earth_wave", "obj.75_fire_wave"),
                staffAnim = "seq.human_castwave_staff",
                unarmedAnim = "seq.human_castwave",
            ) { lvl ->
                when {
                    lvl >= 75 -> 20
                    lvl >= 70 -> 19
                    lvl >= 65 -> 18
                    else -> 17
                }
            },
            Tier(
                name = "surge",
                spells = listOf("obj.81_wind_surge", "obj.85_water_surge", "obj.90_earth_surge", "obj.95_fire_surge"),
                staffAnim = "seq.human_cast_surge",
                unarmedAnim = "seq.human_cast_surge",
            ) { lvl ->
                when {
                    lvl >= 95 -> 24
                    lvl >= 90 -> 23
                    lvl >= 85 -> 22
                    else -> 21
                }
            },
        )

    override fun SpellAttackRepository.register(manager: SpellAttackManager) {
        for (tier in tiers) {
            val tierFx = ELEMENTS.map { ElementFx(it + tier.name) }
            tier.spells.forEachIndexed { element, spell ->
                register(spell = spell, attack = ElementalSpellAttack(manager, tier, tierFx[element], tierFx))
            }
        }
    }

    /**
     * A standard elemental spell. With Elemental bombardment it instead fires every element of its
     * tier at once, each at half size and half the max hit; runes are only paid for the cast spell.
     */
    private class ElementalSpellAttack(
        private val manager: SpellAttackManager,
        private val tier: Tier,
        private val fx: ElementFx,
        private val tierFx: List<ElementFx>,
    ) : SpellAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Spell) {
            cast(target, attack)
        }

        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Spell) {
            cast(target, attack)
        }

        private fun ProtectedAccess.cast(target: PathingEntity, attack: CombatAttack.Spell) {
            val castResult = manager.attemptCast(this, attack)
            if (castResult.isFailure()) {
                return
            }
            player.anim(getOrNull(attack.weapon).castAnim(), priority = 6)
            val maxHit = tier.maxHit(player.magicLvl)
            if (!manager.boostElementalSpells(this)) {
                spotanim(fx.launch, height = 92)
                fire(target, attack, castResult, fx, "projanim.magic_spell", maxHit, suffix = "")
            } else {
                spotanim(fx.launch + HALF_SPOTANIM_SUFFIX, height = 92)
                val halfMaxHit = (maxHit / 2).coerceAtLeast(1)
                tierFx.forEachIndexed { element, elementFx ->
                    val projanim = BOMBARDMENT_PROJANIMS[element]
                    fire(target, attack, castResult, elementFx, projanim, halfMaxHit, HALF_SPOTANIM_SUFFIX)
                }
            }
            manager.continueCombatIfAutocast(this, target)
        }

        private fun ProtectedAccess.fire(
            target: PathingEntity,
            attack: CombatAttack.Spell,
            castResult: MagicRuneManager.CastResult,
            fx: ElementFx,
            projanim: String,
            baseMaxHit: Int,
            suffix: String,
        ) {
            val proj = manager.spawnProjectile(this, target, fx.travel + suffix, projanim)
            val (serverDelay, clientDelay) = proj.durations
            val spell = attack.spell.obj

            val splash = manager.rollSplash(this, target, attack, castResult)
            if (splash) {
                manager.playSplashFx(this, target, clientDelay, fx.castSound, soundRadius = 8)
                manager.queueSplashHit(this, target, spell, clientDelay, serverDelay)
                return
            }

            val damage = manager.rollMaxHit(this, target, attack, castResult, baseMaxHit)
            manager.playHitFx(
                source = this,
                target = target,
                clientDelay = clientDelay,
                castSound = fx.castSound,
                soundRadius = 8,
                hitSpot = fx.impact + suffix,
                hitSpotHeight = 124,
                hitSound = fx.hitSound,
            )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMagicHit(this, target, spell, damage, clientDelay, serverDelay)
        }

        private fun ItemServerType?.castAnim(): String =
            if (this != null && isCategoryType("category.staff")) {
                tier.staffAnim
            } else {
                tier.unarmedAnim
            }
    }
}
