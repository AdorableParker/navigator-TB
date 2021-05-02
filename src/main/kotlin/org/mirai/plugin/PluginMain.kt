/*
 * Copyright (c) 2021.
 * 作者: AdorableParker
 * 最后编辑于: 2021/5/2 下午6:05
 */

package org.mirai.plugin

import com.mayabot.nlp.module.summary.KeywordSummary
import com.mayabot.nlp.segment.Lexers.coreBuilder
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent
import net.mamoe.mirai.event.events.BotLeaveEvent
import net.mamoe.mirai.event.events.NudgeEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.MiraiExperimentalApi
import net.mamoe.mirai.utils.info
import net.mamoe.mirai.utils.warning
import java.io.File
import java.io.InputStream
import java.lang.Thread.sleep
import java.text.SimpleDateFormat
import java.time.LocalDateTime


data class Dynamic(val timestamp: Long?, val text: String?, val imageStream: InputStream?)

@MiraiExperimentalApi
@ConsoleExperimentalApi
object PluginMain : KotlinPlugin(JvmPluginDescription.loadFromResource()) {

    // 分词功能
    val LEXER = coreBuilder()
        .withPos() //词性标注功能
        .withPersonName() // 人名识别功能
//        .withNer() // 命名实体识别
        .build()

    // 关键词提取
    val KEYWORD_SUMMARY = KeywordSummary()

    //    KeywordSummary
    val VOTES: MutableMap<Long, VoteUser> = mutableMapOf()

    override fun onEnable() {
        MySetting.reload() // 从数据库自动读
        MyPluginData.reload()

        CalculationExp.register()   // 经验计算器
        WikiAzurLane.register()     // 碧蓝Wiki
        Construction.register()     // 建造时间
        ShipMap.register()          // 打捞地图
        SendDynamic.register()      // 动态查询
        GroupPolicy.register()      // 群策略
        Roster.register()           // 碧蓝和谐名
        Calculator.register()       // 计算器
        AutoBanned.register()       // 自助禁言
        CrowdVerdict.register()     // 众裁
        SauceNAO.register()         // 搜图
        Request.register()          // 加群操作
        Test.register()             // 测试
        AI.register()               // 图灵数据库增删改查
        Tarot.register()            // 塔罗
        Birthday.register()         // 舰船下水日
        Music.register()            // 点歌姬
        AssetDataAccess.register()  // 资源数据库处理
//        MyHelp.register()           // 帮助功能
        CommandManager.registerCommand(MyHelp, true) // 帮助功能,需要覆盖内建指令

        // 动态更新
        PluginMain.launch {
            val job1 = CronJob("动态更新", 120)
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
                            val img = k?.toExternalResource()
                            for (groupInfo in groupList) {
//                                PluginMain.logger.info { "开始推送至群:${groupInfo["group_id"]}" }
                                val groupID = groupInfo["group_id"] as Int
                                val group = Bot.getInstance(MySetting.BotID).getGroup(groupID.toLong())
                                if (group == null || group.botMuteRemaining > 0) {
                                    continue
                                }
                                img?.let { group.sendImage(it) }
                                j?.let { group.sendMessage(PlainText("$it\n发布时间:$time")) }
                            }
                            img.use { } // PluginMain.logger.debug("关闭图片资源") }
                        } else k?.use { }
                    }
                }

            }
//            job1.start(MyTime(0, 2))
            job1.start(MyTime(0, 6))
        }
        // 报时
        PluginMain.launch {
            val job2 = CronJob("报时", 3)
            job2.addJob {
                val time = LocalDateTime.now().hour
                val dbObject = SQLiteJDBC(resolveDataPath("AssetData.db"))
                val scriptList = dbObject.select("script", "House", time, 1)
                dbObject.closeDB()

                val userDbObject = SQLiteJDBC(resolveDataPath("User.db"))
                val groupList = userDbObject.select("Policy", "TellTimeMode", 0, 5)
                userDbObject.closeDB()

                val script = mutableMapOf<Int, List<MutableMap<String?, Any?>>>()
                for (groupPolicy in groupList) {
                    val groupID = groupPolicy["group_id"] as Int
                    val group = Bot.getInstance(MySetting.BotID).getGroup(groupID.toLong())
                    if (group == null || group.botMuteRemaining > 0) continue

                    val groupMode = groupPolicy["TellTimeMode"] as Int
                    if (groupMode == -1) {
                        group.sendMessage("现在${time}点咯")
                        continue
                    }

                    if (script.containsKey(groupMode).not()) {
                        script[groupPolicy["TellTimeMode"] as Int] =
                            scriptList.filter { it["mode"] == groupPolicy["TellTimeMode"] }
                    }
                    val outScript = script[groupMode]?.random()?.get("content") as String

                    if (groupMode % 2 == 0) {      //偶数
                        val path = PluginMain.resolveDataPath("./报时语音/$outScript")
                        val voice = File("$path").toExternalResource().use {
                            group.uploadVoice(it)
                        }
                        voice.let { group.sendMessage(it) }
                    } else {                      //奇数
                        group.sendMessage(outScript)
                    }
                }
            }
            job2.start(MyTime(1, 0))
//            job2.start(MyTime(0, 1))  // 测试时开启

        }
        // 每日提醒
        PluginMain.launch {
            val job3 = CronJob("每日提醒", 3)
            job3.addJob {
                val dbObject = SQLiteJDBC(resolveDataPath("User.db"))
                val groupList = dbObject.select("Policy", "DailyReminderMode", 0, 5)
                dbObject.closeDB()
                val script = mapOf(
                    1 to arrayListOf(
                        "Ciallo～(∠・ω< )⌒★今天是周一哦,今天开放的是「战术研修」「商船护送」，困难也记得打呢。",
                        "Ciallo～(∠・ω< )⌒★今天是周二哦,今天开放的是「战术研修」「海域突进」，困难也记得打呢。",
                        "Ciallo～(∠・ω< )⌒★今天是周三哦,今天开放的是「战术研修」「斩首行动」，困难也记得打呢。",
                        "Ciallo～(∠・ω< )⌒★今天是周四哦,今天开放的是「战术研修」「商船护送」，困难也记得打呢。",
                        "Ciallo～(∠・ω< )⌒★今天是周五哦,今天开放的是「战术研修」「海域突进」，困难也记得打呢。",
                        "Ciallo～(∠・ω< )⌒★今天是周六哦,今天开放的是「战术研修」「斩首行动」，困难也记得打呢。",
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
                    if (group == null || group.botMuteRemaining > 0) {
                        continue
                    }
                    when (groupPolicy["DailyReminderMode"]) {
                        1 -> script[1]?.get(LocalDateTime.now().dayOfWeek.value - 1)?.let { group.sendMessage(it) }
                        2 -> script[2]?.get(LocalDateTime.now().dayOfWeek.value - 1)?.let { group.sendMessage(it) }
                        3 -> {
                            script[1]?.get(LocalDateTime.now().dayOfWeek.value - 1)?.let { group.sendMessage(it) }
                            script[2]?.get(LocalDateTime.now().dayOfWeek.value - 1)?.let { group.sendMessage(it) }
                        }
                        else -> PluginMain.logger.warning { "未知的模式" }
                    }
                }
            }
            job3.start(MyTime(24, 0), MyTime(21, 0))
//            job3.start(MyTime(0, 3))
        }

        // 入群审核
        this.globalEventChannel().subscribeAlways<BotInvitedJoinGroupRequestEvent> {
            PluginMain.logger.info { "\nGroupName:${it.groupName}\nGroupID：${it.groupId}\nList:${MyPluginData.groupIdList}" }
            if (MyPluginData.groupIdList.contains(it.groupId)) {
                it.accept()
                val dbObject = SQLiteJDBC(resolveDataPath("User.db"))
                dbObject.insert("Policy", arrayOf("group_id"), arrayOf("${it.groupId}"))
                dbObject.insert("SubscribeInfo", arrayOf("group_id"), arrayOf("${it.groupId}"))
                dbObject.insert(
                    "Responsible", arrayOf("group_id", "principal_ID"), arrayOf(
                        "${it.groupId}", "${
                            MyPluginData.groupIdList.remove(
                                it.groupId
                            )
                        }"
                    )
                )
                dbObject.closeDB()
                PluginMain.logger.info { "PASS" }
            } else {
                it.ignore()
                PluginMain.logger.info { "FAIL" }
            }
        }

        // 退群处理
        this.globalEventChannel().subscribeAlways<BotLeaveEvent.Kick> {
            val dbObject = SQLiteJDBC(resolveDataPath("User.db"))
            val pR = dbObject.selectOne("Responsible", "group_id", it.groupId, 1)
            dbObject.delete("Policy", "group_id", it.groupId.toString())
            dbObject.delete("SubscribeInfo", "group_id", it.groupId.toString())
            dbObject.closeDB()
            PluginMain.logger.warning { "###\n事件—被移出群:\n- 群ID：${it.groupId}\n- 相关群负责人：${pR["principal_ID"]}\n###" }
        }
//        BotNudgedEvent
        this.globalEventChannel().subscribeAlways<NudgeEvent> {
            if (this.target == bot && this.from != bot) {
                if ((1..5).random() <= 4) {
                    subject.sendMessage(arrayOf("指挥官，请不要做出这种行为", "这只是全息交互界面", "指挥官，请专心于工作", "全息投影是不会被接触到的").random())
                } else {
                    this.from.nudge().sendTo(subject)
                    subject.sendMessage("戳回去")
                }
            }
        }

        // 聊天触发
        this.globalEventChannel().subscribeGroupMessages(priority = EventPriority.LOWEST) {
            atBot {
                if (group.botMuteRemaining > 0) return@atBot

                val filterMessageList: List<Message> = message.filter { it !is At }
                val filterMessageChain: MessageChain = filterMessageList.toMessageChain()
                AI.dialogue(subject, filterMessageChain.content.trim(), true)
            }
            atBot().not().invoke {
                if (group.botMuteRemaining > 0) return@invoke
                val dbObject = SQLiteJDBC(resolveDataPath("User.db"))
                val groupInfo = dbObject.select("Policy", "group_id", group.id, 1)
                dbObject.closeDB()
                val numerator = groupInfo[0]["TriggerProbability"] as Int
                val v = (1..100).random() <= numerator
//                PluginMain.logger.info { "不at执行这里,$v" }
                if (v) AI.dialogue(subject, message.content.trim())
            }
        }

        logger.info { "Hi: ${MySetting.name},启动完成" } // 输出一条日志.
    }

    override fun onDisable() {
//        PluginMain.launch{ announcement("正在关闭") } // 关闭太快发不出来
        sleep(3 * 1000)
        CalculationExp.unregister() // 经验计算器
        WikiAzurLane.unregister()   // 碧蓝Wiki
        Construction.unregister()   // 建造时间
        ShipMap.unregister()        // 打捞地图
        SendDynamic.unregister()    // 动态查询
        GroupPolicy.unregister()    // 群策略
        Test.unregister()           // 测试
        Roster.unregister()         // 碧蓝和谐名
        Calculator.unregister()     // 计算器
        AutoBanned.unregister()     // 自助禁言
        CrowdVerdict.unregister()   // 众裁
        SauceNAO.unregister()       // 搜图
        Request.unregister()        // 加群操作
        AI.unregister()             // 图灵数据库增删改查
        Tarot.unregister()          // 塔罗
        MyHelp.unregister()           // 帮助功能
        Birthday.unregister()       // 舰船下水日
        Music.unregister()          // 点歌姬
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
    val tellTimeMode: MutableMap<Int, String> by value(
        mutableMapOf(
            1 to "舰队Collection-中文",
            3 to "舰队Collection-日文",
            5 to "明日方舟",
            2 to "舰队Collection-音频",
            4 to "千恋*万花-音频(芳乃/茉子/丛雨/蕾娜)-音频"
        )
    )
    val groupIdList: MutableMap<Long, Long> by value(
        mutableMapOf()
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
    @ValueDescription("名字")
    val name by value("领航员-TB")

    @ValueDescription("Bot 账号")
    val BotID by value(123456L)

//    @ValueDescription("公告群群号")
//    val AnnouncementGroupID by value(123456L)

    @ValueDescription("SauceNAO 的 API Key")
    val SauceNAOKey by value("")

    @ValueDescription("超级管理员账号")
    val AdminID by value(123456L)

    //    @ValueDescription("数量") // 注释写法, 将会保存在 MySetting.yml 文件中.
//    var count by value(0)
//    val nested by value<MyNestedData>() // 嵌套类型是支持的
}