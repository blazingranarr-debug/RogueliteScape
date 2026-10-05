package org.rsmod.api.net.rsprot.handlers

import dev.openrune.ServerCacheManager
import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.locs.OpLoc6
import org.rsmod.api.player.events.interact.LocExamineEvent
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player

class OpLoc6Handler @Inject constructor(private val eventBus: EventBus) : MessageHandler<OpLoc6> {
    override fun handle(player: Player, message: OpLoc6) {
        val type = ServerCacheManager.getObject(message.id) ?: return
        player.mes(type.desc, ChatType.LocExamine)
        eventBus.publish(LocExamineEvent(player, type))
    }
}
