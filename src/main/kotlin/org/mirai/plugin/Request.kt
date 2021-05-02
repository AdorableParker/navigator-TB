/*
 * Copyright (c) 2021.
 * 作者: AdorableParker
 * 最后编辑于: 2021/5/2 下午1:55
 */

package org.mirai.plugin

import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.command.isUser
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.utils.MiraiExperimentalApi
import net.mamoe.mirai.utils.info


@MiraiExperimentalApi
@ConsoleExperimentalApi
object Request : SimpleCommand(
    PluginMain, "授权批准",
    description = "加群申请处理"
) {
    @Handler
    suspend fun CommandSenderOnMessage<MessageEvent>.main(groupID: Long, principal: Long) {
        if (isUser() && user.id == MySetting.AdminID) {
            MyPluginData.groupIdList[groupID] = principal
            sendMessage("OK")
            PluginMain.logger.info { MyPluginData.groupIdList.toString() }
        } else {
            sendMessage("权限不足")
        }
    }
}