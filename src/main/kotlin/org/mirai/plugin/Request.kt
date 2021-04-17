/*
 * Copyright (c) 2021.
 * 作者: AdorableParker
 * 最后编辑于: 2021/4/17 下午3:14
 */

package org.mirai.plugin

import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.command.isUser
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.event.events.MessageEvent


@ConsoleExperimentalApi
object Request : SimpleCommand(
    PluginMain, "授权批准",
    description = "加群申请处理"
) {
    @Handler
    suspend fun CommandSenderOnMessage<MessageEvent>.main(groupID: Long) {
        if (isUser() && user.id == MySetting.AdminID) {
            MyPluginData.groupIdList.add(groupID)
            sendMessage("OK")
        } else {
            sendMessage("权限不足")
        }
    }
}