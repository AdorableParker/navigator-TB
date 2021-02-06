package org.mirai.plugin

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.commandPrefix
import net.mamoe.mirai.console.command.MemberCommandSenderOnMessage
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi

@ConsoleExperimentalApi
object ShipMap : SimpleCommand(
    PluginMain, "ShipMap", "打捞定位",
    description = "碧蓝航线舰船打捞定位"
) {
    override val usage = "${commandPrefix}打捞定位 <船名|地图坐标>"
    private val dataDir = PluginMain.resolveDataPath("AssetData.db")

    @Handler
    suspend fun MemberCommandSenderOnMessage.main(uncheckedIndex: String) {
        val pretreatmentIndex = uncheckedIndex.replace("—", "-").toCharArray()
        pretreatmentIndex.forEachIndexed { index, char ->
            if (char.isLowerCase()) pretreatmentIndex[index] = char.toUpperCase()
        }
        val treatedIndex = String(pretreatmentIndex)
        val index = Regex("\\d*?-\\d").find(treatedIndex)?.value
        index?.let { sendMessage(mapTOName(index)) } ?: sendMessage(nameToMap(treatedIndex))
    }

    @Handler
    suspend fun MemberCommandSenderOnMessage.main() {
        sendMessage("参数不匹配, 你是否想执行:\n $usage")
    }

    private fun nameToMap(index: String): String {
        val db = SQLiteJDBC(dataDir)
        val result = db.select("ship_map", listOf("OriginalName", "Alias"), listOf(index, index), "OR", 4)
        db.closeDB()
        if (result.isEmpty()) return "没有或尚未收录名字包含有 $index 的主线图可打捞舰船"
        val report = mutableListOf("名字包含有 $index 的可打捞舰船有:")
        result.sortWith(
            compareBy(
                { if (it["Special"].toString() == "null") 0 else it["Special"].toString().length },
                { it["Rarity"].toString().length },
                { it["OriginalName"].toString().length },
                { it["Alias"].toString().length },
                { it["OriginalName"].toString() },
                { it["Alias"].toString() }
            )
        )
        for (row in result) {
            report.add("船名：${row["OriginalName"]}[${row["Alias"]}]-${row["Rarity"]}\t可打捞地点:")
            if (row["Special"].toString() != "null") {
                report.add("${row["Special"]}")
                continue
            }
            for (chapter in row.filterKeys { it?.contains("Chapter") ?: false }) {
                val k = chapter.key as String
                val v = chapter.value as Int
                if (v == 0) continue
                report.add(
                    (if (v and 1 == 1) "${k.substring(7)}-1\t" else "") +
                        (if (v and 2 == 2) "${k.substring(7)}-2\t" else "") +
                        (if (v and 4 == 4) "${k.substring(7)}-3\t" else "") +
                        if (v and 8 == 8) "${k.substring(7)}-4\t" else ""
                )
            }
        }
        return report.joinToString("\n")
    }

    private fun mapTOName(index: String): String {
        val coordinate = index.split("-")
        val db = SQLiteJDBC(dataDir)
        val site = 1 shl coordinate[1].toInt() - 1
        val result = db.select("ship_map", "Chapter${coordinate[0]} & $site", site, 1)
        db.closeDB()
        if (result.isEmpty()) return "没有记录主线图坐标为 $index 的地图"
        val report = mutableListOf("可在 $index 打捞的舰船有:")
        result.sortWith(
            compareBy(
                { it["Rarity"].toString() },
                { it["OriginalName"].toString().length },
                { it["Alias"].toString().length },
                { it["OriginalName"].toString() },
                { it["Alias"].toString() }
            )
        )
        for (row in result) {
            report.add("${row["OriginalName"]}[${row["Alias"]}]-${row["Rarity"]}")
        }
        return report.joinToString("\n")
    }
}