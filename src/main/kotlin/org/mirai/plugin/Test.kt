package org.mirai.plugin

import net.mamoe.mirai.console.command.MemberCommandSenderOnMessage
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.utils.info
import java.text.SimpleDateFormat

@ConsoleExperimentalApi
object Test : SimpleCommand(
    PluginMain, "Test", "测试",
    description = "功能测试命令"
) {
    @Handler
    suspend fun MemberCommandSenderOnMessage.main(i: Long) {
        PluginMain.logger.info { "测试命令执行" }
        val time = SimpleDateFormat("YY-MM-dd HH:mm").format(i)
        sendMessage(time)
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