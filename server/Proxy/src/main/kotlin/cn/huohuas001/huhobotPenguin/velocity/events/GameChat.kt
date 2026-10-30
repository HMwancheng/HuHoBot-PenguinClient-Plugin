package cn.huohuas001.huhobotPenguin.velocity.events

import cn.huohuas001.bot.QClient
import cn.huohuas001.bot.tools.containsRegexMatch
import cn.huohuas001.huhobotPenguin.velocity.HuHoBotVelocity
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.player.PlayerChatEvent
import java.util.concurrent.ConcurrentHashMap

class GameChat(private val plugin: HuHoBotVelocity) {
    /** 已发送进服通知的玩家，退服时据此决定是否补发退服通知。 */
    private val announcedPlayers = ConcurrentHashMap.newKeySet<String>()

    @Subscribe
    fun onPlayerChat(event: PlayerChatEvent) {
        QClient.broadcastGameMessage(event.player.username, event.message)
    }

    @Subscribe
    fun onPlayerJoin(event: PostLoginEvent) {
        val name = event.player.username
        // 命中配置中的脚本命名规则时不播报；退服侧因未登记也不会补发。
        if (containsRegexMatch(name, plugin.getPlayerEventFormat().filterNameRegex)) return
        // 探针等短连接触发 PostLoginEvent 后会立即断开，延迟到满足最短在线时长再确认发送。
        plugin.submitLater(plugin.getPlayerEventFormat().minOnlineSeconds * 20L) {
            if (plugin.getOnlineList().contains(name)) {
                announcedPlayers.add(name)
                QClient.broadcastPlayerJoin(name)
            }
        }
    }

    @Subscribe
    fun onPlayerQuit(event: DisconnectEvent) {
        if (!plugin.getPlayerEventFormat().alwaysForward &&
            event.loginStatus != DisconnectEvent.LoginStatus.SUCCESSFUL_LOGIN
        ) return
        // 只有成功发送过进服通知的玩家才补发退服通知，短连接不会产生任何通知。
        if (announcedPlayers.remove(event.player.username)) {
            QClient.broadcastPlayerQuit(event.player.username)
        }
    }
}