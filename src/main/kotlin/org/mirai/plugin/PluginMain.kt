/*
 * Copyright (c) 2021.
 * 作者: AdorableParker
 * 最后编辑于: 2021/2/14 上午3:11
 */

package org.mirai.plugin

import com.mayabot.nlp.module.summary.KeywordSummary
import com.mayabot.nlp.segment.Lexers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.info
import net.mamoe.mirai.utils.warning
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.LocalDateTime


data class Dynamic(val timestamp: Long?, val text: String?, val imageURL: InputStream?)

@ConsoleExperimentalApi
object PluginMain : KotlinPlugin(JvmPluginDescription.loadFromResource()) {
    val LEXER = Lexers.coreBuilder()
//            .withPos() //词性标注功能
        .withPersonName() // 人名识别功能
        .withNer() // 命名实体识别
        .build()
    val KEYWORD_SUMMARY = KeywordSummary()

    val VOTELIST: MutableMap<Long, VoteUser> = mutableMapOf()

    override fun onEnable() {
        MySetting.reload() // 从数据库自动读
        MyPluginData.reload()
        logger.info { "Hi: ${MySetting.name}" } // 输出一条日志.

//        MySetting.count++ // 对 Setting 的改动会自动在合适的时间保存
        CalculationExp.register() // 注册指令
        WikiAzurLane.register()
        Construction.register()
        ShipMap.register()
        SendDynamic.register()
        GroupPolicy.register()
        Roster.register()
        Calculator.register()
        AutoBanned.register()
        CrowdVerdict.register()
        Test.register()
        // 动态更新
        PluginMain.launch {
            val job1 = CronJob("动态更新")
            job1.addJob {
                for (list in MyPluginData.timeStampOfDynamic) {
                    val (i, j, k) = SendDynamic.getDynamic(list.key, flag = true)
                    if (i != null) {
                        val time = SimpleDateFormat("YY-MM-dd HH:mm").format(i)
                        val dbObject = SQLiteJDBC(resolveDataPath("User.db"))
                        val groupList =
                            MyPluginData.nameOfDynamic[list.key]?.let { dbObject.select("SubscribeInfo", it, 1.0, 1) }
                        dbObject.closeDB()
                        if (groupList != null) {
                            for (groupInfo in groupList) {
                                val groupID = groupInfo["group_id"] as Int
                                val group = Bot.getInstance(MySetting.BotID).getGroup(groupID.toLong())
                                k?.let { group?.sendImage(it) }
                                j?.let { group?.sendMessage("$it\n发布时间:$time") }
                            }
                        }
                    }
                }

            }
            job1.start(MyTime(0, 6))
        }
        // 报时
        PluginMain.launch {
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
                    val group = Bot.getInstance(MySetting.BotID).getGroup(groupID.toLong())
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
        // 每日提醒
        PluginMain.launch {
            val job3 = CronJob("每日提醒")
            job3.addJob {
                val dbObject = SQLiteJDBC(resolveDataPath("User.db"))
                val groupList = dbObject.select("Policy", "DailyReminderMode", 0, 5)
                dbObject.closeDB()
                val script = mapOf(
                    1 to arrayListOf(
                        "Ciallo～(∠・ω< )⌒★今天是周一哦,今天开放的是「战术研修」「斩首行动」，困难也记得打呢。",
                        "Ciallo～(∠・ω< )⌒★今天是周二哦,今天开放的是「战术研修」「商船护送」，困难也记得打呢。",
                        "Ciallo～(∠・ω< )⌒★今天是周三哦,今天开放的是「战术研修」「海域突进」，困难也记得打呢。",
                        "Ciallo～(∠・ω< )⌒★今天是周四哦,今天开放的是「战术研修」「斩首行动」，困难也记得打呢。",
                        "Ciallo～(∠・ω< )⌒★今天是周五哦,今天开放的是「战术研修」「商船护送」，困难也记得打呢。",
                        "Ciallo～(∠・ω< )⌒★今天是周六哦,今天开放的是「战术研修」「海域突进」，困难也记得打呢。",
                        "Ciallo～(∠・ω< )⌒★今天是周日哦,每日全部模式开放，每周两次的破交作战记得打哦，困难模式也别忘了。"
                    ),
                    2 to arrayListOf(
                        "Ciallo～(∠・ω< )⌒★晚上好,Master,今天是周一, 今天周回本开放「弓阶修炼场」,「收集火种(枪杀)」。",
                        "Ciallo～(∠・ω< )⌒★晚上好,Master,今天是周二, 今天周回本开放「枪阶修炼场」,「收集火种(剑骑)」。",
                        "Ciallo～(∠・ω< )⌒★晚上好,Master,今天是周三, 今天周回本开放「狂阶修炼场」,「收集火种(弓术)」。",
                        "Ciallo～(∠・ω< )⌒★晚上好,Master,今天是周四, 今天周回本开放「骑阶修炼场」,「收集火种(枪杀)」。",
                        "Ciallo～(∠・ω< )⌒★晚上好,Master,今天是周五, 今天周回本开放「术阶修炼场」,「收集火种(剑骑)」。",
                        "Ciallo～(∠・ω< )⌒★晚上好,Master,今天是周六, 今天周回本开放「杀阶修炼场」,「收集火种(弓术)」。",
                        "Ciallo～(∠・ω< )⌒★晚上好,Master,今天是周日, 今天周回本开放「剑阶修炼场」,「收集火种(All)」。"
                    )
                )
                for (groupPolicy in groupList) {
                    val groupID = groupPolicy["group_id"] as Int
                    val group = Bot.getInstance(MySetting.BotID).getGroup(groupID.toLong())
                    when (groupPolicy["DailyReminderMode"]) {
                        1 -> script[1]?.get(LocalDateTime.now().dayOfWeek.value - 1)?.let { group?.sendMessage(it) }
                        2 -> script[2]?.get(LocalDateTime.now().dayOfWeek.value - 1)?.let { group?.sendMessage(it) }
                        3 -> {
                            script[1]?.get(LocalDateTime.now().dayOfWeek.value - 1)?.let { group?.sendMessage(it) }
                            script[2]?.get(LocalDateTime.now().dayOfWeek.value - 1)?.let { group?.sendMessage(it) }
                        }
                        else -> PluginMain.logger.warning { "未知的模式" }
                    }
                }
            }
            job3.start(MyTime(24, 0), MyTime(21, 0))
//            job3.start(MyTime(0, 3))
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
        Roster.unregister()
        Calculator.unregister()
        AutoBanned.unregister()
        CrowdVerdict.unregister()
        PluginMain.cancel()
    }
}

// 定义插件数据
// 插件
object MyPluginData : AutoSavePluginData("TB_Data") { // "name" 是保存的文件名 (不带后缀)
    val timeStampOfDynamic: MutableMap<Int, Long> by value(
        mutableMapOf(
            233114659 to 1L,
            161775300 to 1L,
            233108841 to 1L,
            401742377 to 1L
        )
    )
    val nameOfDynamic: MutableMap<Int, String> by value(
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

object MySetting : AutoSavePluginConfig("TB_Setting") {
    val name by value("领航员-TB")
    val BotID by value(123456L)

//    @ValueDescription("数量") // 注释写法, 将会保存在 MySetting.yml 文件中.
//    var count by value(0)
//    val nested by value<MyNestedData>() // 嵌套类型是支持的
}

