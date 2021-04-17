/*
 * Copyright (c) 2021.
 * 作者: AdorableParker
 * 最后编辑于: 2021/4/17 下午3:14
 */

package org.mirai.plugin

import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.MemberCommandSenderOnMessage
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi

@ConsoleExperimentalApi
object MyHelp : SimpleCommand(
    PluginMain, "help", "帮助", "菜单",
    description = "帮助命令"
) {
    override val usage: String = "${CommandManager.commandPrefix}help"

    @Handler
    suspend fun MemberCommandSenderOnMessage.main() {
        val helpDocs = mutableListOf<String>()
        CommandManager.allRegisteredCommands.forEach {
            if (it.owner == PluginMain) {
                helpDocs.add("主命令名:${it.primaryName}\t别名：${it.secondaryNames.joinToString(",")}\n说明:${it.description}")
            }
        }
        sendMessage(helpDocs.joinToString("\n"))
//        val commandList = PluginMain.allNames
//        sendMessage("${}")
    }
}