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
object Roster : SimpleCommand(
    PluginMain, "Roster", "船名查询", "和谐名",
    description = "碧蓝航线船名查询"
) {
    override val usage: String = "${CommandManager.commandPrefix}船名查询 <船名>"
    private val dataDir = PluginMain.resolveDataPath("AssetData.db")

    @Handler
    suspend fun MemberCommandSenderOnMessage.main() {
        sendMessage("参数不匹配, 你是否想执行:\n $usage")
    }

    @Handler
    suspend fun MemberCommandSenderOnMessage.main(shipName: String) {
        val i = shipName.toCharArray()
        i.forEachIndexed { index, char ->
            if (char.isLowerCase()) i[index] = char.toUpperCase()
        }
        val treated = String(i)
        val dbObject = SQLiteJDBC(dataDir)
        val roster = dbObject.select("Roster", listOf("code", "name"), listOf(treated, treated), "OR", 4)
        dbObject.closeDB()
        if (roster.isEmpty()) {
            sendMessage("没有或尚未收录名字包含有 $shipName 的舰船");return
        }
        roster.sortWith(compareBy { it["name"].toString() })
        val report = mutableListOf("名字包含有 $shipName 的舰船有:")
        for (row in roster) {
            report.add("原名：${row["name"]}\t和谐名：${row["code"]}")
        }
        sendMessage(report.joinToString("\n"))
    }
}