package org.mirai.plugin

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.commandPrefix
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.MemberCommandSenderOnMessage
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.info
import org.jsoup.Jsoup.connect
import org.mirai.plugin.MyPluginData.nameOfDynamic
import org.mirai.plugin.MyPluginData.timeStampOfDynamic
import org.mirai.plugin.MySetting.BotID
import org.mirai.plugin.MySetting.saveName
import java.io.File
import java.io.InputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.time.LocalDateTime

data class Dynamic(val timestamp: Long?, val text: String?, val imageURL: InputStream?)

@ConsoleExperimentalApi
object PluginMain : KotlinPlugin(JvmPluginDescription.loadFromResource()) {
    override fun onEnable() {
        MySetting.reload() // 从数据库自动读取配置实例
        MyPluginData.reload()
        logger.info { "Hi: ${MySetting.name}" } // 输出一条日志.

//        MySetting.count++ // 对 Setting 的改动会自动在合适的时间保存
        CalculationExp.register() // 注册指令
        WikiAzurLane.register()
        Construction.register()
        ShipMap.register()
        SendDynamic.register()
        GroupPolicy.register()
        Test.register()

        GlobalScope.launch {
            val job1 = CronJob("动态更新")
            job1.addJob {
                for (list in timeStampOfDynamic) {
                    val (i, j, k) = SendDynamic.getDynamic(list.key, flag = true)
                    if (i != null) {
                        val time = SimpleDateFormat("YY-MM-DD hh:mm").format(i)
                        val dbObject = SQLiteJDBC(resolveDataPath("User.db"))
                        val groupList = nameOfDynamic[list.key]?.let { dbObject.select("SubscribeInfo", it, 1.0, 1) }
                        dbObject.closeDB()
                        if (groupList != null) {
                            for (groupInfo in groupList) {
                                val groupID = groupInfo["group_id"] as Int
                                val group = Bot.getInstance(BotID).getGroup(groupID.toLong())
                                k?.let { group?.sendImage(it) }
                                j?.let { group?.sendMessage("$it\n发布时间:$time") }
                            }
                        }
                    }
                }

            }
            job1.start(MyTime(0, 6))
        }
        GlobalScope.launch {
            val job2 = CronJob("报时")
            job2.addJob {
                val time = LocalDateTime.now().hour
                val dbObject = SQLiteJDBC(resolveDataPath("AssetData.db"))
                val scriptList = dbObject.select("script", "House", time, 1)
                dbObject.closeDB()
                val script1 = scriptList.filter { it["mode"] == 1 }
                val script2 = scriptList.filter { it["mode"] == 2 }
                val script3 = scriptList.filter { it["mode"] == 3 }
                val script4 = scriptList.filter { it["mode"] == 4 }
                val userDbObject = SQLiteJDBC(resolveDataPath("User.db"))
                val groupList = userDbObject.select("Policy", "TellTimeMode", 0, 5)
                userDbObject.closeDB()
                for (groupPolicy in groupList) {
                    val groupID = groupPolicy["group_id"] as Int
                    val group = Bot.getInstance(BotID).getGroup(groupID.toLong())
                    when (groupPolicy["TellTimeMode"]) {
                        1 -> group?.sendMessage(script1.random()["content"] as String)
                        2 -> group?.sendMessage(script2.random()["content"] as String)
                        3 -> group?.sendMessage(script3.random()["content"] as String)
                        4 -> {
                            val path = PluginMain.resolveDataPath("./报时语音/${script4.random()["content"] as String}")
                            val voice = File("$path").toExternalResource().use {
                                group?.uploadVoice(it)
                            }
                            voice?.let { group?.sendMessage(it) }
                        }
                        else -> group?.sendMessage("现在${time}点咯")
                    }
                }
            }
            job2.start(MyTime(1, 0))
        }
    }

    override fun onDisable() {
        CalculationExp.unregister() // 取消注册指令
        WikiAzurLane.unregister()
        Construction.unregister()
        ShipMap.unregister()
        SendDynamic.unregister()
        GroupPolicy.unregister()
        Test.unregister()
        GlobalScope.cancel()
    }
}

// 定义插件数据
// 插件
object MyPluginData : AutoSavePluginData("TB_Data") { // "name" 是保存的文件名 (不带后缀)
    var timeStampOfDynamic: MutableMap<Int, Long> by value(
        mutableMapOf(
            233114659 to 1L,
            161775300 to 1L,
            233108841 to 1L,
            401742377 to 1L
        )
    )
    var nameOfDynamic: MutableMap<Int, String> by value(
        mutableMapOf(
            233114659 to "AzurLane",
            161775300 to "ArKnights",
            233108841 to "FateGrandOrder",
            401742377 to "GenShin"
        )
    )
//    var long: Long by value(0L) // 允许 var
//    var int by value(0) // 可以使用类型推断, 但更推荐使用 `var long: Long by value(0)` 这种定义方式.

//     带默认值的非空 map.
//     notnullMap[1] 的返回值总是非 null 的 MutableMap<Int, String>
//    var notnullMap by value<MutableMap<Int, MutableMap<Int, String>>>().withEmptyDefault()

//     可将 MutableMap<Long, Long> 映射到 MutableMap<Bot, Long>.
//    val botToLongMap: MutableMap<Bot, Long> by value<MutableMap<Long, Long>>().mapKeys(Bot::getInstance, Bot::id)
}

// 定义一个配置. 所有属性都会被追踪修改, 并自动保存.
// 配置是插件与用户交互的接口, 但不能用来保存插件的数据.
/** [saveName] 是保存的文件名 (不带后缀)
 *
 */
object MySetting : AutoSavePluginConfig("TB_Setting") {
    val name by value("领航员-TB")
    val BotID by value(123456L)

//    @ValueDescription("数量") // 注释写法, 将会保存在 MySetting.yml 文件中.
//    var count by value(0)
//    val nested by value<MyNestedData>() // 嵌套类型是支持的
}

//@Serializable
//data class MyNestedData(
//    val list: List<String> = listOf()
//)


// 舰船经验计算器
@ConsoleExperimentalApi
object CalculationExp : SimpleCommand(
    PluginMain, "calculationExp", "舰船经验", "经验计算",
    description = "舰船经验计算器"
) {
    override val usage: String = "${commandPrefix}舰船经验 <当前等级> <目标等级> <是否为决战方案> <已有经验>\n <是否决战方案> 参数只接受“true”及其大小写变体"

    /**
     * 完整功能
     * 返回从 当前等级[current_level]到 目标等级[target_level]所需要的经验值报告。
     * 决战方案[special]和 已有经验[existingExp]因素影响计入报告 */
    @Handler
    suspend fun MemberCommandSenderOnMessage.main(
        current_level: Int,
        target_level: Int,
        special: Boolean,
        existingExp: Int
    ) {
        val balance = (current_level until target_level).fold(0, { accExp: Int, level: Int ->
            val result = accExp + calculateParts(level, special)
            result
        }) - existingExp
        sendMessage("当前等级:$current_level,目标等级:$target_level\n是否为决战方案:$special\n已有经验:$existingExp\n最终计算结果: ${if (balance <= 0) "达成目标等级后将溢出 ${0 - balance} EXP" else "还需 $balance EXP 可以达成目标等级"}")
    }

    /**
     * 仅计算已有经验影响
     * 返回从 当前等级[current_level]到 目标等级[target_level]所需要的经验值报告。
     * 已有经验[existingExp] 因素影响计入报告 */
    @Handler
    suspend fun MemberCommandSenderOnMessage.main(current_level: Int, target_level: Int, existingExp: Int) {
        val balance = (current_level until target_level).fold(0, { accExp: Int, level: Int ->
            val result = accExp + calculateParts(level, false)
            result
        }) - existingExp

        if (balance <= 0) {
            sendMessage("当前等级:$current_level,目标等级:$target_level\n已有经验:$existingExp\n最终计算结果: 达成目标等级后将溢出 ${0 - balance} EXP")
        } else {
            sendMessage("当前等级:$current_level,目标等级:$target_level\n已有经验:$existingExp\n最终计算结果: 还需 $balance EXP 可以达成目标等级")
        }
    }
//    此类重载未能实现支持
//    /**
//     * 仅计算决战方案影响
//     * 返回从 当前等级[current_level]到 目标等级[target_level]所需要的经验值报告。
//     * 决战方案[special] 因素影响计入报告 */
//    @Handler
//    suspend fun MemberCommandSenderOnMessage.main(current_level:Int, target_level:Int, special:Boolean) {
//        val balance = (current_level until target_level).fold(0, {
//            accExp:Int ,level:Int -> val result = accExp + calculateParts(level,special)
//            result
//        })
//        if (balance <= 0){
//            sendMessage("当前等级:$current_level,目标等级:$target_level\n是否为决战方案:$special\n最终计算结果: 达成目标等级后将溢出 $balance EXP")
//        }else{
//            sendMessage("当前等级:$current_level,目标等级:$target_level\n是否为决战方案:$special\n最终计算结果: 还需 $balance EXP 可以达成目标等级")
//        }
//    }
    /**
     * 仅基础计算
     * 返回从 当前等级[current_level]到 目标等级[target_level]所需要的经验值报告。
     * 决战方案 默认为 否
     * 已有经验 默认为 0 */
    @Handler // 基础计算
    suspend fun MemberCommandSenderOnMessage.main(current_level: Int, target_level: Int) {
        val balance = (current_level until target_level).fold(0, { accExp: Int, level: Int ->
            val result = accExp + calculateParts(level, false)
            result
        })
        sendMessage("当前等级:$current_level,目标等级:$target_level\n最终计算结果: 需 $balance EXP 可以达成目标等级")
    }

    /**参数不匹配时输出提示 */
    @Handler
    suspend fun MemberCommandSenderOnMessage.main() {
        sendMessage("参数不匹配, 你是否想执行:\n $usage")
    }

    private fun calculateParts(target_level: Int, special: Boolean): Int {
        val totalExp = when (target_level) {
            in 0..40 -> target_level * 10
            in 41..60 -> 400 + (target_level - 40) * 20
            in 61..70 -> 800 + (target_level - 60) * 30
            in 71..80 -> 1100 + (target_level - 70) * 40
            in 81..90 -> 1500 + (target_level - 80) * 50
            in 101..104 -> 7000 + (target_level - 100) * 200
            in 106..110 -> 8500 + (target_level - 105) * 1200
            in 111..115 -> 14500 + (target_level - 110) * 1800
            in 116..119 -> 23500 + (target_level - 115) * 2100
            else -> when (target_level) {
                91 -> 2100
                92 -> 2200
                93 -> 2400
                94 -> 2600
                95 -> 3000
                96 -> 3500
                97 -> 4000
                98 -> 6000
                99 -> 13200
                100 -> 7000
                105 -> 8500
                120 -> return 3000000
                else -> 0
            }
        }
        return if (special) {
            totalExp * if (target_level in 90..99) 13 else 12
        } else {
            totalExp * 10
        }
    }

}

// 碧蓝几大基本榜单查询
@ConsoleExperimentalApi
object WikiAzurLane : CompositeCommand(
    PluginMain, "WikiAzurLane", "碧蓝wiki", // "primaryName" 是主指令名
    description = "碧蓝几大基本榜单查询"
) {
    override val usage: String =
        "$commandPrefix $primaryName <榜单ID>\n" +
            "榜单ID列表：\n" +
            "*1* 强度榜,强度主榜\n" +
            "*2* 强度副榜\n" +
            "*3* 装备榜\n" +
            "*4* P站榜,社保榜"

    @SubCommand("强度榜", "强度主榜", "1")
    suspend fun MemberCommandSenderOnMessage.strengthRanking() {
//        TB.logger.info { "获取图片" }
        val imageStream = getWikiImg("PVE用舰船综合性能强度榜", 1)
        if (imageStream != null) {
            subject.sendImage(imageStream)
        } else {
            sendMessage("访问Wiki失败惹,这一定是塞壬的阴谋\nε(┬┬﹏┬┬)3")
        }
    }

    @SubCommand("强度副榜", "2")
    suspend fun MemberCommandSenderOnMessage.strengthDeputyRanking() {
        val imageStream = getWikiImg("PVE用舰船综合性能强度榜", 2)
        if (imageStream != null) {
            subject.sendImage(imageStream)
        } else {
            sendMessage("访问Wiki失败惹,这一定是塞壬的阴谋\nε(┬┬﹏┬┬)3")

        }
    }

    @SubCommand("装备榜", "3")
    suspend fun MemberCommandSenderOnMessage.equipmentRanking() {
        val imageStream = getWikiImg("装备一图榜", 0)
        if (imageStream != null) {
            subject.sendImage(imageStream)
        } else {
            sendMessage("访问Wiki失败惹,这一定是塞壬的阴谋\nε(┬┬﹏┬┬)3")
        }
    }

    @SubCommand("P站榜", "社保榜", "4")
    suspend fun MemberCommandSenderOnMessage.pixivRanking() {
        val imageStream = getWikiImg("P站搜索结果一览榜（社保榜）", 0)
        if (imageStream != null) {
            subject.sendImage(imageStream)
        } else {
            sendMessage("访问Wiki失败惹,这一定是塞壬的阴谋\nε(┬┬﹏┬┬)3")
        }
    }

    private fun getWikiImg(index: String, sub: Int): InputStream? {
        val doc = connect("https://wiki.biligame.com/blhx/$index").get()
        val links = doc.select("div#mw-content-text").select(".mw-parser-output").select("img[src]")
        val url = URL(links[sub].attr("abs:src"))
//        return try {  // 等发现确实有可能异常了再说
        return url.openConnection().getInputStream()
//        }catch(err:java.io.IOException){
//            null
//        }
    }
}

@ConsoleExperimentalApi
object Construction : SimpleCommand(
    PluginMain, "Construction", "建造时间",
    description = "碧蓝航线建造时间查询"
) {
    override val usage: String = "${commandPrefix}建造时间 <时间|船名>"
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

@ConsoleExperimentalApi
object Test : SimpleCommand(
    PluginMain, "Test", "测试",
    description = "功能测试命令"
) {
    @Handler
    suspend fun MemberCommandSenderOnMessage.main() {
        PluginMain.logger.info{"测试命令执行"}
        val path = PluginMain.resolveDataPath("./报时语音/苍龙/苍龙-1.amr")
        val voice = File("$path").toExternalResource().use {
            subject.uploadVoice(it)
        }
        sendMessage(voice)
//        val v: Voice =  ExternalResource.toExternalResource("雾岛/雾岛-0.mp3")
//        ExternalResource.uploadAsVoice()

//        val dbObject = SQLiteJDBC(TB.resolveDataPath("User.db"))
//        dbObject.insert(
//            "COMPANY",
//            arrayOf("ID", "NAME", "AGE", "ADDRESS", "SALARY"),
//            arrayOf("1", "'Paul'", "32", "'California'", "20000.00")
//        )
//        dbObject.update("COMPANY", "NAME", "'Paul'", arrayOf("'AGE'", "'SALARY'"), arrayOf("40", "54000.00"))
//        val r = dbObject.select("AzurLane_construct_time", "OriginalName", "小",false)
//        dbObject.select("COMPANY", "NAME", "a", false)
//        dbObject.delete("COMPANY", "ADDRESS", "'Texas'")
//        dbObject.closeDB()
//        if (r.isEmpty()) {
//            sendMessage("查询结果为空")
//        } else {
//            sendMessage("${r[0].keys}")
//            for (row in r) {
//                sendMessage("${row.values}")
//            }
//        }
    }
}

@ConsoleExperimentalApi
object SendDynamic : CompositeCommand(
    PluginMain, "SendDynamic", "动态查询",
    description = "B站动态查询"
) {
    override val usage: String =
        "$commandPrefix $primaryName <目标ID> [回溯条数]\n" +
            "目标ID列表：\n" +
            "*1* 小加加,火星加,碧蓝公告\n" +
            "*2* 阿米娅,方舟公告,罗德岛线报\n" +
            "*3* 呆毛王,FGO公告,月球人公告\n" +
            "*4* 派蒙,原神公告,冒险家情报\n" +
            "*5* UID,其他"

    @SubCommand("小加加", "火星加", "碧蓝公告", "1")
    suspend fun MemberCommandSenderOnMessage.azurLane() {
        val (timeStamp, text, images) = getDynamic(233114659)
        val time = SimpleDateFormat("YY-MM-DD hh:mm").format(timeStamp)
//        if (images?.isEmpty() == true){
//            TB.logger.info{"有图"}
//            text?.let { sendMessage("$it\n发布时间:$time") }
//            for(img in images){
//                subject.sendImage(img)
//            }
//            val image = ExternalResource.
//            val chain = buildMessageChain{
//                text?.let { PlainText("$it\n附图：\n发布时间:$time") }
//                images[0].toExternalResource()
//            sendMessage(chain)
//            }
//        for (image in images){
//            val s = image.toExternalResource()
//
//        }
//        }else{
        images?.let { subject.sendImage(it) }
        text?.let { sendMessage("$it\n发布时间:$time") }
//        }
//        sendMessage(getDynamic(233114659))
    }

    @SubCommand("小加加", "火星加", "碧蓝公告", "1")
    suspend fun MemberCommandSenderOnMessage.azurLane(index: Int) {
        if(index >= 10){
            sendMessage("最多只能往前10条哦\n的(￣﹃￣)")
            return
        }else if(index < 0){
            sendMessage("未来的事情我怎么会知道\n=￣ω￣=")
            return
        }
        val (timeStamp, text, images) = getDynamic(233114659, index)
        val time = SimpleDateFormat("YY-MM-DD hh:mm").format(timeStamp)
//        if (images?.isEmpty() == true){
//            val f = images[0].toExternalResource()
//            val chain = buildMessageChain{
//                text?.let { PlainText("$it\n发布时间:$time") }
//                add(subject.uploadImage(f))
//            }
//            f.close()
//            sendMessage(chain)
//        for (image in images){
//            val s = image.toExternalResource()
//
//        }
//        }else{
        images?.let { subject.sendImage(it) }
        text?.let { sendMessage("$it\n发布时间:$time") }
//        }
    }

    @SubCommand("阿米娅", "方舟公告", "罗德岛线报", "2")
    suspend fun MemberCommandSenderOnMessage.arKnights() {
        val (timeStamp, text, images) = getDynamic(161775300)
        val time = SimpleDateFormat("YY-MM-DD hh:mm").format(timeStamp)
        images?.let { subject.sendImage(it) }
        text?.let { sendMessage("$it\n发布时间:$time") }
    }

    @SubCommand("阿米娅", "方舟公告", "罗德岛线报", "2")
    suspend fun MemberCommandSenderOnMessage.arKnights(index: Int) {
        if(index >= 10){
            sendMessage("最多只能往前10条哦\n的(￣﹃￣)")
            return
        }else if(index < 0){
            sendMessage("未来的事情我怎么会知道\n=￣ω￣=")
            return
        }
        val (timeStamp, text, images) = getDynamic(161775300, index)
        val time = SimpleDateFormat("YY-MM-DD hh:mm").format(timeStamp)
        images?.let { subject.sendImage(it) }
        text?.let { sendMessage("$it\n发布时间:$time") }
    }

    @SubCommand("呆毛王", "FGO公告", "月球人公告", "3")
    suspend fun MemberCommandSenderOnMessage.fateGrandOrder() {
        val (timeStamp, text, images) = getDynamic(233108841)
        val time = SimpleDateFormat("YY-MM-DD hh:mm").format(timeStamp)
        images?.let { subject.sendImage(it) }
        text?.let { sendMessage("$it\n发布时间:$time") }
    }

    @SubCommand("呆毛王", "FGO公告", "月球人公告", "3")
    suspend fun MemberCommandSenderOnMessage.fateGrandOrder(index: Int) {
        if(index >= 10){
            sendMessage("最多只能往前10条哦\n的(￣﹃￣)")
            return
        }else if(index < 0){
            sendMessage("未来的事情我怎么会知道\n=￣ω￣=")
            return
        }
        val (timeStamp, text, images) = getDynamic(233108841, index)
        val time = SimpleDateFormat("YY-MM-DD hh:mm").format(timeStamp)
        images?.let { subject.sendImage(it) }
        text?.let { sendMessage("$it\n发布时间:$time") }
    }

    @SubCommand("派蒙", "原神公告", "冒险家情报", "4")
    suspend fun MemberCommandSenderOnMessage.genShin() {
        val (timeStamp, text, images) = getDynamic(401742377)
        val time = SimpleDateFormat("YY-MM-DD hh:mm").format(timeStamp)
        images?.let { subject.sendImage(it) }
        text?.let { sendMessage("$it\n发布时间:$time") }
    }

    @SubCommand("派蒙", "原神公告", "冒险家情报", "4")
    suspend fun MemberCommandSenderOnMessage.genShin(index: Int) {
        if(index >= 10){
            sendMessage("最多只能往前10条哦\n的(￣﹃￣)")
            return
        }else if(index < 0){
            sendMessage("未来的事情我怎么会知道\n=￣ω￣=")
            return
        }
        val (timeStamp, text, images) = getDynamic(401742377, index)
        val time = SimpleDateFormat("YY-MM-DD hh:mm").format(timeStamp)
        images?.let { subject.sendImage(it) }
        text?.let { sendMessage("$it\n发布时间:$time") }
    }

    @SubCommand("UID", "其他", "5")
    suspend fun MemberCommandSenderOnMessage.other(uid: Int) {
        val (timeStamp, text, images) = getDynamic(uid)
        val time = SimpleDateFormat("YY-MM-DD hh:mm").format(timeStamp)
        images?.let { subject.sendImage(it) }
        text?.let { sendMessage("$it\n发布时间:$time") }
    }

    @SubCommand("UID", "其他", "5")
    suspend fun MemberCommandSenderOnMessage.other(uid: Int, index: Int) {
        if(index >= 10){
            sendMessage("最多只能往前10条哦\n(￣﹃￣)")
            return
        }else if(index < 0){
            sendMessage("未来的事情我怎么会知道\n=￣ω￣=")
            return
        }
        val (timeStamp, text, images) = getDynamic(uid, index)
        val time = SimpleDateFormat("YY-MM-DD hh:mm").format(timeStamp)
        images?.let { subject.sendImage(it) }
        text?.let { sendMessage("$it\n发布时间:$time") }
    }

    fun getDynamic(uid: Int, index: Int = 0, flag: Boolean = false): Dynamic {
        val doc = connect("https://api.vc.bilibili.com/dynamic_svr/v1/dynamic_svr/space_history?host_uid=$uid")
            .ignoreContentType(true)
            .execute().body().toString()
        val jsonObj = Parser.default().parse(StringBuilder(doc)) as JsonObject
        val desc = jsonObj.obj("data")
            ?.array<JsonObject>("cards")?.get(index)
            ?.obj("desc")
        val timestamp = desc?.long("timestamp")?.times(1000)
        if (flag) {
            PluginMain.logger.info { "开始判断时间" }
            timestamp?.let {
                val oldTime = timeStampOfDynamic[uid] ?: 0
                if (oldTime >= it) return Dynamic(null, null, null)
                PluginMain.logger.info { "准备更新数据" }
                timeStampOfDynamic[uid] = it
                PluginMain.logger.info { "数据更新完毕" }
            }
        }
        val typeCode = desc?.int("type")
        val cardStr = jsonObj.obj("data")
            ?.array<JsonObject>("cards")?.get(index)
            ?.string("card")
        val card = Parser.default().parse(StringBuilder(cardStr)) as JsonObject
        when (typeCode) {
            // 无效数据
            0 -> return Dynamic(timestamp, "没有相关动态信息", null)
            // 转发
            1 -> return Dynamic(timestamp, "转发并评论：${card.obj("item")?.string("content")}", null)
            // 含图动态
            2 -> {
                val description = card.obj("item")?.string("description")   // 描述
                val imgSrc = card.obj("item")?.array<JsonObject>("pictures")?.string("img_src")?.toTypedArray()
//                TB.logger.info{"${imgSrc?.get(0)}"}
                if (imgSrc != null) {
//                    val images = mutableListOf<InputStream>()
//                    for(url in imgSrc){
//                        images.add(URL(url).openConnection().getInputStream())
//                    }
//                    return Dynamic(timestamp,description,images)
                    return Dynamic(timestamp, description, URL(imgSrc[0]).openConnection().getInputStream())
                }
                return Dynamic(timestamp, description, null)
            }
            // 无图动态
            4 -> return Dynamic(timestamp, "更新动态：${card.obj("item")?.string("content")}", null)
            // 视频
            8 -> {
                val dynamic = card.string("dynamic") // 描述
                val imgSrc = card.string("pic")      //封面图片
                return Dynamic(timestamp, dynamic, URL(imgSrc).openConnection().getInputStream())
//                return Dynamic(timestamp,dynamic, mutableListOf(URL(imgSrc).openConnection().getInputStream()))
            }
            // 专栏
            64 -> {
                val title = card.string("title")       // 标题
                val summary = card.string("summary")   // 摘要
                val imgSrc = card.string("banner_url") // 封面图片
                return Dynamic(
                    timestamp,
                    "专栏标题:$title\n专栏摘要：\n$summary…",
                    URL(imgSrc).openConnection().getInputStream()
                )
//                return Dynamic(timestamp,"专栏标题:$title\n专栏摘要：\n$summary…", mutableListOf(URL(imgSrc).openConnection().getInputStream()))
            }
            // 卡片
            2048 -> {
                val title = card.obj("sketch")?.string("title")          // 标题
                val context = card.obj("vest")?.string("content")        // 内容
                val targetURL = card.obj("sketch")?.string("target_url") // 相关链接
                return Dynamic(timestamp, "动态标题:$title\n动态内容：\n$context\n相关链接:\n$targetURL", null)
            }
            // 未知类型
            else -> {
                PluginMain.logger.warning("错误信息:未知的类型码 $typeCode ")
                return Dynamic(timestamp, "是未知的动态类型,无法解析", null)
            }
        }
    }
}

@ConsoleExperimentalApi
object GroupPolicy : CompositeCommand(
    PluginMain, "GroupPolicy", "群策略",
    description = "群功能个性化配置"
) {
    override val usage: String =
        "$commandPrefix $primaryName <目标ID> [设定值]\n" +
            "目标ID列表：\n" +
            "*1* 报时模式\n" +
            "*2* 订阅模式\n"

    @SubCommand("报时模式","1")
    suspend fun MemberCommandSenderOnMessage.tellTime(mode:Int) {
        sendMessage("报时设定到模式$mode")
    // TODO:尚未实现
    }

    @SubCommand("订阅模式","2")
    suspend fun MemberCommandSenderOnMessage.subscription(mode:Int) {
        sendMessage("订阅设定到模式$mode")
    // TODO:尚未实现
    }
}