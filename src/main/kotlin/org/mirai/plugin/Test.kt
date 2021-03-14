/*
 * Copyright (c) 2021.
 * 作者: AdorableParker
 * 最后编辑于: 2021/3/14 下午6:16
 */

package org.mirai.plugin

import net.mamoe.mirai.console.command.MemberCommandSenderOnMessage
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.info
import java.time.LocalDateTime
import kotlin.random.Random


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
    fun MemberCommandSenderOnMessage.main() {
        PluginMain.logger.info {
            "测试命令执行"
        }
        val list = mutableSetOf<Long>()
        bot.groups.forEach { list.add(it.id) }
        PluginMain.logger.info { list.joinToString(",") }
    }

    @Handler
    suspend fun MemberCommandSenderOnMessage.main(seed: Int) {
        PluginMain.logger.info { "测试命令执行" }
        val today = LocalDateTime.now()
        var i = 1
        while (user.id / 10 * i <= 0) i *= 10
        val seeds = (today.year * 1000L + today.dayOfYear) * i + seed
        val listA = listOf(
            "愚者",
            "魔术师", "女祭司", "女皇", "皇帝", "教皇",
            "恋人", "战车", "力量", "隐者", "命运之轮",
            "正义", "倒吊人", "死神", "节制", "恶魔",
            "塔", "星星", "月亮", "太阳", "审判",
            "世界"
        )
        sendMessage(listA.random(Random(seeds)))
    }
}

