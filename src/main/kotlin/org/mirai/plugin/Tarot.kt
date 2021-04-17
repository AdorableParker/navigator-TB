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
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.io.File
import java.time.LocalDateTime
import kotlin.random.Random


@ConsoleExperimentalApi
object Tarot : SimpleCommand(
    PluginMain, "DailyTarot", "每日塔罗",
    description = "塔罗占卜"
) {
    override val usage: String = "${CommandManager.commandPrefix}每日塔罗"

    @Handler
    suspend fun MemberCommandSenderOnMessage.main() {
        if (group.botMuteRemaining > 0) return

//        PluginMain.logger.info { "测试命令执行" }
        val today = LocalDateTime.now()
        var i = 1
        while (user.id / 10 * i <= 0) i *= 10
        val seeds = (today.year * 1000L + today.dayOfYear) * i + user.id
        val brand = listOf(
            "The Fool(愚者)",
            "The Magician(魔术师)", "The High Priestess(女祭司)", "The Empress(女王)", "The Emperor(皇帝)", "The Hierophant(教皇)",
            "The Lovers(恋人)", "The Chariot(战车)", "Strength(力量)", "The Hermit(隐者)", "Wheel of Fortune(命运之轮)",
            "Justice(正义)", "The Hanged Man(倒吊人)", "Death(死神)", "Temperance(节制)", "The Devil(恶魔)",
            "The Tower(塔)", "The Star(星星)", "The Moon(月亮)", "The Sun(太阳)", "Judgement(审判)",
            "The World(世界)"
        ).random(Random(seeds))
//        PluginMain.logger.info{brand}
        val dbObject = SQLiteJDBC(PluginMain.resolveDataPath("AssetData.db"))
        val r = dbObject.select("Tarot", "Brand", brand)[0]
        dbObject.closeDB()
        if ((0..100).random(Random(seeds)) >= 50) {
            File(PluginMain.resolveDataPath(r["uprightImg"].toString()).toString()).toExternalResource().use {
                sendMessage(PlainText("判定！顺位-$brand\n牌面含义关键词:${r["Upright"]}") + group.uploadImage(it))
            }
        } else {
            File(PluginMain.resolveDataPath(r["invertImg"].toString()).toString()).toExternalResource().use {
                sendMessage(PlainText("判定！逆位-$brand\n牌面含义关键词:${r["Reversed"]}") + group.uploadImage(it))
            }
        }
    }
}