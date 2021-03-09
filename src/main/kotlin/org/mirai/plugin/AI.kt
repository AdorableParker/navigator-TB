/*
 * Copyright (c) 2021.
 * 作者: AdorableParker
 * 最后编辑于: 2021/3/9 下午7:10
 */

package org.mirai.plugin

import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.MemberCommandSenderOnMessage
import net.mamoe.mirai.console.util.ConsoleExperimentalApi

@ConsoleExperimentalApi
object AI : CompositeCommand(
    PluginMain, "AI",
    description = "AI功能"
) {
    @SubCommand("教学")
    suspend fun MemberCommandSenderOnMessage.main(question: String, answer: String) {
        val userDBObject = SQLiteJDBC(PluginMain.resolveDataPath("User.db"))
        val info = userDBObject.select("Policy", "group_id", group.id, 1)
        if (info[0]["Teaching"] == 0.0) {
            userDBObject.closeDB()
            sendMessage("本群禁止教学,请联系管理员开启")
            return
        }
        userDBObject.closeDB()
        val keyWord = PluginMain.KEYWORD_SUMMARY.keyword(question, 1)[0]
        val dbObject = SQLiteJDBC(PluginMain.resolveDataPath("AI.db"))
        val entry = dbObject.select(
            "Corpus",
            listOf("answer", "question", "keys", "fromGroup"),
            listOf(answer, question, keyWord, "${group.id}"),
            "AND",
            0
        )
        if (entry.isNotEmpty()) {
            sendMessage("问题:$question\n回答:$answer\n该条目已存在，条目ID:${entry[0]["ID"]}")
            dbObject.closeDB()
            return
        }
        dbObject.insert(
            "Corpus",
            arrayOf("answer", "question", "keys", "fromGroup"),
            arrayOf(
                "'$answer'", "'$question'", "'$keyWord'",
                "${group.id}"
            )
        )
        val entryID = dbObject.select(
            "Corpus",
            listOf("answer", "question", "keys", "fromGroup"),
            listOf(answer, question, keyWord, "${group.id}"),
            "AND"
        )[0]["ID"]

        dbObject.closeDB()
        sendMessage("问题:$question\n回答:$answer\n条目已添加，条目ID:$entryID")
    }

    @SubCommand("查询")
    suspend fun MemberCommandSenderOnMessage.main(key: String) {
        val dbObject = SQLiteJDBC(PluginMain.resolveDataPath("AI.db"))
        val entryList =
            dbObject.executeStatement("""SELECT * FROM Corpus WHERE answer GLOB "*$key*" OR question GLOB "*$key*" OR keys GLOB "*$key*";""")
        dbObject.closeDB()
        val r = when {
            entryList.isEmpty() -> "问答包含关键词${key}的条目不存在"
            entryList.size >= 10 -> {
                val report = mutableListOf("问答包含关键词${key}的条目过多(超过十条)，仅提供本群关键词，控制权限:完全控制")
                for (row in entryList) {
                    if (row["fromGroup"].toString().toLong() == group.id) {
                        report.add("问题:${row["question"]}\n回答:${row["answer"]}\n条目ID:${row["ID"]}")
                    }
                }
                report.joinToString("\n")
            }
            entryList.size >= 20 -> "问答包含关键词${key}的条目过多(超过二十条)，请提供更加详细的关键词"
            else -> {
                val report = mutableListOf("条目清单:")
                for (row in entryList) {
                    when (row["fromGroup"].toString().toLong()) {
                        group.id -> report.add("问题:${row["question"]}\n回答:${row["answer"]}\n条目ID:${row["ID"]}\n控制权限:完全控制")
                        0L -> report.add("问题:${row["question"]}\n回答:${row["answer"]}\n条目ID:${row["ID"]}\n控制权限:只读权限")
                        else -> report.add("问题:隐藏\t回答:隐藏\n条目ID:${row["ID"]}\n控制权限:不可操作")
                    }
                }
                report.joinToString("\n")
            }
        }
        sendMessage(r)
    }

    @SubCommand("删除")
    suspend fun MemberCommandSenderOnMessage.main(EID: Int) {
        val dbObject = SQLiteJDBC(PluginMain.resolveDataPath("AI.db"))
        val entry = dbObject.select("Corpus", "ID", EID, 1)
        if (entry.size > 0) for (row in entry) {
            if (row["fromGroup"].toString().toLong() == group.id) {
                dbObject.delete("Corpus", "ID", "$EID")
                sendMessage("问题:${row["question"]}\n回答:${row["answer"]}\n条目ID:${row["ID"]}\n条目已删除")
                break
            } else sendMessage("该条目本群无权删除")
        } else sendMessage("没有该条目")
        dbObject.closeDB()
    }
}