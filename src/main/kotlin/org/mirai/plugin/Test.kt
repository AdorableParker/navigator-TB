/*
 * Copyright (c) 2021.
 * 作者: AdorableParker
 * 最后编辑于: 2021/3/9 下午7:10
 */

package org.mirai.plugin

import net.mamoe.mirai.console.command.MemberCommandSenderOnMessage
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.info


@ConsoleExperimentalApi
object Test : SimpleCommand(
    PluginMain, "Test", "测试",
    description = "功能测试命令"
) {
    @Handler
    suspend fun MemberCommandSenderOnMessage.main(string: String) {
        PluginMain.logger.info { "测试命令执行" }
        sendMessage("This is test,input is $string")
    }

    @Handler
    suspend fun MemberCommandSenderOnMessage.main(image: Image) {
        PluginMain.logger.info { "测试命令执行" }
        sendMessage("this is test,Image downloadURL is ${image.queryUrl()}")
    }

    @Handler
    suspend fun MemberCommandSenderOnMessage.main() {
        CacheData.ActivatedList.add(group.id)
        sendMessage("群号:${group.id} 状态:已激活")
    }
}

