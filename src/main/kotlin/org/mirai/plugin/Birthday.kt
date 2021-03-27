/*
 * Copyright (c) 2021.
 * 作者: AdorableParker
 * 最后编辑于: 2021/3/27 下午1:01
 */

package org.mirai.plugin

import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.MemberCommandSenderOnMessage
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@ConsoleExperimentalApi
object Birthday : SimpleCommand(
    PluginMain, "舰娘生日", "历史今天",
    description = "历史今日下水舰船"
) {
    override val usage: String = "${CommandManager.commandPrefix}舰娘生日"

    @Handler
    suspend fun MemberCommandSenderOnMessage.main() {
        if (group.botMuteRemaining > 0) return

//        PluginMain.logger.info { "测试命令执行" }
        val today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("M月dd日"))
//        val today = "2月25日"
        val dbObject = SQLiteJDBC(PluginMain.resolveDataPath("AssetData.db"))
        val r = dbObject.select("ShipBirthday", "LaunchDay", today)
        dbObject.closeDB()
        if (r.isEmpty()) {
            sendMessage("今天生日的舰娘没有记载哦")
            return
        }
        for (i in r) {
            sendMessage("${i["LaunchYear"]}年的今天,${i["Nationality"]}${i["ShipType"]}${i["Name"]}下水")
//            sendMessage("${i["LaunchYear"]}年的今天,${i["Nationality"]}${i["ShipType"]}${i["Name"]}下水于$")
        }
//        if ((0..100).random(Random(seeds)) >= 50) {
//            File(PluginMain.resolveDataPath(r["uprightImg"].toString()).toString()).toExternalResource().use {
//                sendMessage(PlainText("判定！顺位-$brand\n牌面含义关键词:${r["Upright"]}") + group.uploadImage(it))
//            }
//        } else {
//            File(PluginMain.resolveDataPath(r["invertImg"].toString()).toString()).toExternalResource().use {
//                sendMessage(PlainText("判定！逆位-$brand\n牌面含义关键词:${r["Reversed"]}") + group.uploadImage(it))
//            }
//        }
    }
}