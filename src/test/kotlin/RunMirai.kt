/*
 * Copyright (c) 2021.
 * 作者: AdorableParker
 * 最后编辑于: 2021/4/17 下午3:14
 */

package org.example.mirai.plugin

import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.console.MiraiConsole
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.enable
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.load
import net.mamoe.mirai.console.terminal.MiraiConsoleTerminalLoader
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.utils.debug
import org.mirai.plugin.PluginMain

@ConsoleExperimentalApi
suspend fun main() {
    MiraiConsoleTerminalLoader.startAsDaemon()

    PluginMain.load()
    PluginMain.enable()

    val bot = MiraiConsole.addBot(963795439, "8541.jet@0") {
        fileBasedDeviceInfo()
    }.alsoLogin()
    bot.logger.debug { "bot状态：${bot.isOnline}" }

    MiraiConsole.job.join()
}