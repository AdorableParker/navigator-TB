/*
 * Copyright (c) 2021.
 * 作者: AdorableParker
 * 最后编辑于: 2021/5/2 下午1:55
 */

package org.mirai.plugin

import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.isUser
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.utils.MiraiExperimentalApi

@ConsoleExperimentalApi
@MiraiExperimentalApi
object AssetDataAccess : CompositeCommand(
    PluginMain, "AssetDataAccess", "写入资产",
    description = "资产数据库写入操作"
) {
    override val usage: String = "${CommandManager.commandPrefix}写入资产 [建造时间|和谐名]"

    @SubCommand("建造时间")
    suspend fun CommandSenderOnMessage<MessageEvent>.main(
        shipName: String,
        alias: String,
        time: String,
        limitedTime: Boolean
    ) {
        if (isUser() && user.id == MySetting.AdminID) {
            val limited = if (limitedTime) "1.0" else "0.0"
            val dbObject = SQLiteJDBC(PluginMain.resolveDataPath("AssetData.db"))
            dbObject.insert(
                "AzurLane_construct_time",
                arrayOf("OriginalName", "Alias", "Time", "LimitedTime"),
                arrayOf(shipName, alias, time, limited)
            )
            dbObject.closeDB()
            sendMessage("写入完成")
        } else {
            sendMessage("权限不足")
        }
    }

    @SubCommand("和谐名")
    suspend fun CommandSenderOnMessage<MessageEvent>.main(shipName: String, alias: String) {
        if (isUser() && user.id == MySetting.AdminID) {
            val dbObject = SQLiteJDBC(PluginMain.resolveDataPath("AssetData.db"))
            dbObject.insert(
                "AzurLane_construct_time",
                arrayOf("code", "name"),
                arrayOf(shipName, alias)
            )
            dbObject.closeDB()
            sendMessage("写入完成")
        } else {
            sendMessage("权限不足")
        }
    }
}