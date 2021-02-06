package org.mirai.plugin

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.commandPrefix
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.MemberCommandSenderOnMessage
import net.mamoe.mirai.console.util.ConsoleExperimentalApi

@ConsoleExperimentalApi
object GroupPolicy : CompositeCommand(
    PluginMain, "GroupPolicy", "群策略",
    description = "群功能个性化配置"
) {
    override val usage: String = """
        $commandPrefix $primaryName <目标ID> [设定值]
        目标ID列表：
        *1* 报时模式
        *2* 订阅模式
        *3* 每日提醒模式
        """.trimIndent()

    @SubCommand("报时模式", "1")
    suspend fun MemberCommandSenderOnMessage.tellTime(mode: Int) {
        val dbObject = SQLiteJDBC(PluginMain.resolveDataPath("User.db"))
        when (mode) {
            0 -> {
                dbObject.update("Policy", "group_id", group.id, "TellTimeMode", "0")
                sendMessage("已关闭本群报时")
            }
            1, 2, 3, 4, 5 -> {
                dbObject.update("Policy", "group_id", group.id, "TellTimeMode", mode)
                sendMessage("报时设定到模式$mode")
            }
            else -> {
                tellTime()
            }
        }
        dbObject.closeDB()
    }

    @SubCommand("报时模式", "1")
    suspend fun MemberCommandSenderOnMessage.tellTime() {
        sendMessage(
            """
            无效模式参数，设定失败,请参考以下示范命令
            群策略 报时模式 [模式值]
            ——————————
            模式值 | 说明
            0	    关闭报时
            1	    舰队Collection-中文
            2	    舰队Collection-日文
            3	    明日方舟
            4	    舰队Collection-音频
            5       标准报时
            """.trimIndent()
        )
    }


    @SubCommand("订阅模式", "2")
    suspend fun MemberCommandSenderOnMessage.subscription(mode: String) {
        val i = mode.toIntOrNull(16)
        if (i != null && i >= 0) {
            val dbObject = SQLiteJDBC(PluginMain.resolveDataPath("User.db"))
            dbObject.update("SubscribeInfo", "group_id", group.id, "AzurLane", if (i and 1 == 1) 1.0 else 0.0)
            dbObject.update("SubscribeInfo", "group_id", group.id, "ArKnights", if (i and 2 == 2) 1.0 else 0.0)
            dbObject.update("SubscribeInfo", "group_id", group.id, "FateGrandOrder", if (i and 4 == 4) 1.0 else 0.0)
            dbObject.update("SubscribeInfo", "group_id", group.id, "GenShin", if (i and 8 == 8) 1.0 else 0.0)
            sendMessage("订阅设定到模式$mode")
        } else {
            subscription()
        }
    }

    @SubCommand("订阅模式", "2")
    suspend fun MemberCommandSenderOnMessage.subscription() {
        sendMessage(
            """
            无效模式参数，设定失败,请参考以下示范命令
            群策略 订阅模式 [模式值]
            ——————————
            模式值使用16进制值保存，下面是计算方法及示例(1:开启,0:关闭)
            Mode | AzLn | Akns | FGO | Gesn
            3[3]        1           1       0       0
            3 = 1*1 + 1*2 + 0*4 + 0*8
            
            7[7]        1           1       1       0
            7 = 1*1 + 1*2 + 1*4 + 0*8
            
            A[10]       0           1       0       1
            A = 0*1 + 1*2 + 0*4 + 1*8
            
            F[15]       1           1       1       1
            F = 1*1 + 1*2 + 1*4 + 1*8
            """.trimIndent()
        )
    }

    @SubCommand("每日提醒模式", "3")
    suspend fun MemberCommandSenderOnMessage.dailyReminder(mode: Int) {
        val dbObject = SQLiteJDBC(PluginMain.resolveDataPath("User.db"))
        when (mode) {
            0 -> {
                dbObject.update("Policy", "group_id", group.id, "DailyReminderMode", "0")
                sendMessage("已关闭本群每日提醒")
            }
            1, 2, 3 -> {
                dbObject.update("Policy", "group_id", group.id, "DailyReminderMode", mode)
                sendMessage("报时设定到模式$mode")
            }
            else -> {
                dailyReminder()
            }
        }
        dbObject.closeDB()
    }

    @SubCommand("每日提醒模式", "3")
    suspend fun MemberCommandSenderOnMessage.dailyReminder() {
        sendMessage(
            """
            无效模式参数，设定失败,请参考以下示范命令
            群策略 每日提醒模式 [模式值]
            ——————————
            模式值 | 说明
            0	    关闭每日提醒
            1	    仅开启 AzurLane 每日提醒
            2	    仅开启 FateGrandOrder 每日提醒
            3	    同时开启 AzurLane 与 FateGrandOrder 每日提醒
            """.trimIndent()
        )
    }
}
