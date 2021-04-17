/*
 * Copyright (c) 2021.
 * 作者: AdorableParker
 * 最后编辑于: 2021/4/17 下午3:14
 */

package org.mirai.plugin

import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.MemberCommandSenderOnMessage
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.isOperator
import org.mirai.plugin.MyPluginData.tellTimeMode

@ConsoleExperimentalApi
object GroupPolicy : CompositeCommand(
    PluginMain, "GroupPolicy", "群策略",
    description = "群功能个性化配置"
) {
    override val usage: String = """
        ${CommandManager.commandPrefix}群策略 <目标ID> [设定值]
        目标ID列表：
        *1* 报时模式
        *2* 订阅模式
        *3* 每日提醒模式
        *4* 教学许可
        *5* 对话概率
        """.trimIndent()

    @SubCommand("报时模式")
    suspend fun MemberCommandSenderOnMessage.tellTime(mode: Int) {
        if (permissionCheck(user)) {
            sendMessage("权限不足")
            return
        }
        val dbObject = SQLiteJDBC(PluginMain.resolveDataPath("User.db"))
        if (tellTimeMode.containsKey(mode)) {
            dbObject.update("Policy", "group_id", group.id, "TellTimeMode", mode)
            sendMessage("报时设定到模式 ${tellTimeMode[mode]}")
        } else {
            if (mode == 0) {
                dbObject.update("Policy", "group_id", group.id, "TellTimeMode", 0)
                sendMessage("已关闭本群报时")
            } else {
                dbObject.update("Policy", "group_id", group.id, "TellTimeMode", -1)
                sendMessage("未知的模式，报时设定到标准模式")
            }
        }
        dbObject.closeDB()
    }

    @SubCommand("报时模式")
    suspend fun MemberCommandSenderOnMessage.tellTime() {
        val info: MutableList<String> = mutableListOf()
        tellTimeMode.forEach {
            info.add("${it.key}\t    ${it.value}")
        }
        sendMessage(
            "无效模式参数，设定失败,请参考以下示范命令\n群策略 报时模式 [模式值]\n——————————\n模式值 | 说明\n${info.joinToString("\n")}\n-\t    标准报时"
        )
    }

    @SubCommand("订阅模式")
    suspend fun MemberCommandSenderOnMessage.subscription(mode: String) {
        if (permissionCheck(user)) {
            sendMessage("权限不足")
            return
        }
        val i = mode.toIntOrNull(16)
        if (i != null && i >= 0) {
            val dbObject = SQLiteJDBC(PluginMain.resolveDataPath("User.db"))
            dbObject.update("SubscribeInfo", "group_id", group.id, "AzurLane", if (i and 1 == 1) 1.0 else 0.0)
            dbObject.update("SubscribeInfo", "group_id", group.id, "ArKnights", if (i and 2 == 2) 1.0 else 0.0)
            dbObject.update("SubscribeInfo", "group_id", group.id, "FateGrandOrder", if (i and 4 == 4) 1.0 else 0.0)
            dbObject.update("SubscribeInfo", "group_id", group.id, "GenShin", if (i and 8 == 8) 1.0 else 0.0)
            sendMessage("订阅设定到模式$mode")
            dbObject.closeDB()
        } else {
            subscription()
        }
    }

    @SubCommand("订阅模式")
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

    @SubCommand("每日提醒模式")
    suspend fun MemberCommandSenderOnMessage.dailyReminder(mode: Int) {
        if (permissionCheck(user)) {
            sendMessage("权限不足")
            return
        }
        val dbObject = SQLiteJDBC(PluginMain.resolveDataPath("User.db"))
        when (mode) {
            0 -> {
                dbObject.update("Policy", "group_id", group.id, "DailyReminderMode", "0")
                sendMessage("已关闭本群每日提醒")
            }
            1, 2, 3 -> {
                dbObject.update("Policy", "group_id", group.id, "DailyReminderMode", mode)
                sendMessage("每日提醒设定到模式$mode")
            }
            else -> {
                dailyReminder()
            }
        }
        dbObject.closeDB()
    }

    @SubCommand("每日提醒模式")
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

    @SubCommand("教学许可")
    suspend fun MemberCommandSenderOnMessage.teaching(switch: Int) {
        if (permissionCheck(user)) {
            sendMessage("权限不足")
            return
        }
        val dbObject = SQLiteJDBC(PluginMain.resolveDataPath("User.db"))
        if (switch > 0) {
            dbObject.update("Policy", "group_id", group.id, "Teaching", 1.0)
            sendMessage("已开启本群教学模式")
        } else {
            dbObject.update("Policy", "group_id", group.id, "Teaching", 0.0)
            sendMessage("已关闭本群教学模式")
        }
        dbObject.closeDB()
    }

    @SubCommand("教学许可")
    suspend fun MemberCommandSenderOnMessage.teaching() {
        sendMessage(
            """
            无效模式参数，设定失败,请参考以下示范命令
            群策略 教学许可 [模式值]
            ——————————
            模式值 | 说明
            > 0	    开启教学功能
            ≯ 0     关闭教学功能
            """.trimIndent()
        )
    }

    @SubCommand("对话概率")
    suspend fun MemberCommandSenderOnMessage.triggerProbability(value: Int) {
        if (permissionCheck(user)) {
            sendMessage("权限不足")
            return
        }
        val dbObject = SQLiteJDBC(PluginMain.resolveDataPath("User.db"))
        dbObject.update("Policy", "group_id", group.id, "TriggerProbability", value)
        dbObject.closeDB()
        sendMessage("本群对话概率调整到$value%")
    }

    @SubCommand("对话概率")
    suspend fun MemberCommandSenderOnMessage.triggerProbability() {
        sendMessage(
            """
            无效模式参数，设定失败,请参考以下示范命令
            群策略 对话概率 [概率值]
            ——————————
            概率值最高为100(必定触发),最低为0(绝不触发)
            #若无可回答内容，任何情况下都不会触发
            """.trimIndent()
        )
    }

    private fun permissionCheck(user: Member): Boolean {
        return user.permission.isOperator().not()
    }
}
