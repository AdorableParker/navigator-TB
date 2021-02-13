/*
 * Copyright (c) 2021.
 * 作者: AdorableParker
 * 最后编辑于: 2021/2/14 上午3:11
 */

package org.mirai.plugin

import net.mamoe.mirai.console.command.MemberCommandSenderOnMessage
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.PermissionDeniedException

@ConsoleExperimentalApi
object AutoBanned : SimpleCommand(
    PluginMain, "AutoBanned", "自助禁言", "睡眠套餐",
    description = "用于解决群员的自闭需求"
) {
    @Handler
    suspend fun MemberCommandSenderOnMessage.main(durationSeconds: Int) {
        try {
            if (durationSeconds != 0) {
                user.mute(durationSeconds)
                sendMessage("您的套餐已到，请注意查收。")
            }
        } catch (error: PermissionDeniedException) {
            sendMessage("嘤嘤嘤，在本群权限不足")
        }
    }
}