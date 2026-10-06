package org.rsmod.api.player.cheat

import org.rsmod.api.attr.AttributeKey
import org.rsmod.game.entity.Player

public val ADMIN_GOD_MODE_ATTR: AttributeKey<Boolean> = AttributeKey()

public val ADMIN_MAX_HIT_ATTR: AttributeKey<Boolean> = AttributeKey()

/** Map-clock cycle until which the player takes no damage; session-only. */
public val DAMAGE_IMMUNE_UNTIL_ATTR: AttributeKey<Int> = AttributeKey()

/** Set while the player has the Admin menu open; session-only. */
public val ADMIN_MENU_IMMUNE_ATTR: AttributeKey<Boolean> = AttributeKey()

/** Set while the player stands in a safe zone; session-only. */
public val SAFE_ZONE_ATTR: AttributeKey<Boolean> = AttributeKey()

public var Player.adminGodMode: Boolean
    get() = attr[ADMIN_GOD_MODE_ATTR] == true
    set(value) {
        if (value) {
            attr[ADMIN_GOD_MODE_ATTR] = true
        } else {
            attr.remove(ADMIN_GOD_MODE_ATTR)
        }
    }

public var Player.adminMaxHit: Boolean
    get() = attr[ADMIN_MAX_HIT_ATTR] == true
    set(value) {
        if (value) {
            attr[ADMIN_MAX_HIT_ATTR] = true
        } else {
            attr.remove(ADMIN_MAX_HIT_ATTR)
        }
    }

/** True while the player takes no damage at all: god mode, an open Admin menu, a safe zone or a timed immunity. */
public val Player.isDamageImmune: Boolean
    get() =
        adminGodMode ||
            attr[ADMIN_MENU_IMMUNE_ATTR] == true ||
            attr[SAFE_ZONE_ATTR] == true ||
            (attr[DAMAGE_IMMUNE_UNTIL_ATTR] ?: 0) > currentMapClock

/** Grants damage immunity until [cycle] (a map-clock value); `null` removes it. */
public fun Player.setDamageImmuneUntil(cycle: Int?) {
    if (cycle == null) {
        attr.remove(DAMAGE_IMMUNE_UNTIL_ATTR)
    } else {
        attr[DAMAGE_IMMUNE_UNTIL_ATTR] = cycle
    }
}

public val XP_BLOCKED_ATTR: AttributeKey<Boolean> = AttributeKey()

/** While set, no skill gains experience; session-only. */
public var Player.xpBlocked: Boolean
    get() = attr[XP_BLOCKED_ATTR] == true
    set(value) {
        if (value) {
            attr[XP_BLOCKED_ATTR] = true
        } else {
            attr.remove(XP_BLOCKED_ATTR)
        }
    }
