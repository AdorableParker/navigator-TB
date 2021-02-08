/*
 * Copyright (c) 2021.
 * 作者: AdorableParker
 * 最后编辑于: 2021/2/8 上午8:03
 */

package org.mirai.plugin

import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.MemberCommandSenderOnMessage
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi

@ConsoleExperimentalApi
object Construction : SimpleCommand(
    PluginMain, "Construction", "建造时间",
    description = "碧蓝航线建造时间查询"
) {
    override val usage: String = "${CommandManager.commandPrefix}建造时间 <时间|船名>"
    private val dataDir = PluginMain.resolveDataPath("AssetData.db")

    @Handler
    suspend fun MemberCommandSenderOnMessage.main(uncheckedIndex: String) {
        val pretreatmentIndex = uncheckedIndex.replace('：', ':').toCharArray()
        pretreatmentIndex.forEachIndexed { index, char ->
            if (char.isLowerCase()) pretreatmentIndex[index] = char.toUpperCase()
        }
        val treatedIndex = String(pretreatmentIndex)
        val index = Regex("\\d:\\d\\d").find(treatedIndex)?.value
        index?.let { sendMessage(timeToName(index)) } ?: sendMessage(nameToTime(treatedIndex))
    }

    @Handler
    suspend fun MemberCommandSenderOnMessage.main() {
        sendMessage("参数不匹配, 你是否想执行:\n $usage")
    }

    private fun timeToName(index: String): String {
        val db = SQLiteJDBC(dataDir)
        val result = db.select("AzurLane_construct_time", "Time", index, 3)
        db.closeDB()
        if (result.isEmpty()) return "没有或尚未收录建造时间为 $index 的可建造舰船"
        val report = mutableListOf("建造时间为 $index 的舰船有:")
        result.sortBy { it["OriginalName"].toString().length }
        result.sortWith(
            compareBy(
                { if (it["LimitedTime"] == 0.0) 1 else 0 },
                { it["OriginalName"].toString().length },
                { it["Alias"].toString().length },
                { it["OriginalName"].toString() },
                { it["Alias"].toString() }
            )
        )
        for (row in result) {
            report.add("船名：${row["OriginalName"]}[${row["Alias"]}]\t${if (row["LimitedTime"] == 1.0) "限时" else "\t常驻"}")
        }
        db.closeDB()
        return report.joinToString("\n")
    }

    private fun nameToTime(index: String): String {
        val db = SQLiteJDBC(dataDir)
        val result =
            db.select("AzurLane_construct_time", listOf("OriginalName", "Alias"), listOf(index, index), "OR", 4)
        db.closeDB()
        if (result.isEmpty()) return "没有或尚未收录名字包含有 $index 的可建造舰船"
        val report = mutableListOf("名字包含有 $index 的可建造舰船有:")
        result.sortWith(
            compareBy(
                { if (it["LimitedTime"] == 0.0) 1 else 0 },
                { it["Time"].toString().split(":")[0] },
                { it["Time"].toString().split(":")[1] },
                { it["Time"].toString().split(":")[2] },
                { it["OriginalName"].toString().length },
                { it["Alias"].toString().length },
                { it["OriginalName"].toString() },
                { it["Alias"].toString() }
            )
        )
        for (row in result) {
            report.add("船名：${row["OriginalName"]}[${row["Alias"]}]\t建造时间：${row["Time"]} ${if (row["LimitedTime"] == 1.0) "限时" else "常驻"}")
        }
        return report.joinToString("\n")
    }
}