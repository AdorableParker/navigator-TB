/*
 * Copyright (c) 2021.
 * 作者: AdorableParker
 * 最后编辑于: 2021/5/2 下午6:05
 */

package org.mirai.plugin

import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.MemberCommandSenderOnMessage
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.utils.MiraiExperimentalApi

@MiraiExperimentalApi
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
        val keyWord = PluginMain.KEYWORD_SUMMARY.keyword(question, 1).let { if (it.size <= 0) question else it[0] }
//        PluginMain.logger.debug { "*$question*\n$keyWord" }

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

    @SubCommand("EID查询")
    suspend fun MemberCommandSenderOnMessage.eIDMain(EID: Int) {
        val dbObject = SQLiteJDBC(PluginMain.resolveDataPath("AI.db"))
        val entryList =
            dbObject.select("Corpus", "ID", EID, 1)
        dbObject.closeDB()
        val r = when {
            entryList.isEmpty() -> "条目${EID}不存在"
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

    @SubCommand("sudo删除")
    suspend fun MemberCommandSenderOnMessage.sudoMain(EID: Int) {
        val dbObject = SQLiteJDBC(PluginMain.resolveDataPath("AI.db"))
        val entry = dbObject.select("Corpus", "ID", EID, 1)
        if (entry.size > 0) for (row in entry) {
            if (user.id == MySetting.AdminID) {
                dbObject.delete("Corpus", "ID", "$EID")
                sendMessage("问题:${row["question"]}\n回答:${row["answer"]}\n条目ID:${row["ID"]}\n条目已删除")
                break
            } else sendMessage("权限不足")
        } else sendMessage("没有该条目")
        dbObject.closeDB()
    }

    suspend fun dialogue(subject: Group, content: String, atMe: Boolean = false) {
        val keyWord = PluginMain.KEYWORD_SUMMARY.keyword(content, 1).let { if (it.size <= 0) content else it[0] }
        val wordList = PluginMain.LEXER.scan(content)
//        PluginMain.logger.debug { "*$content*\n$keyWord" }

        val dbObject = SQLiteJDBC(PluginMain.resolveDataPath("AI.db"))
        val rList = if (keyWord.isNullOrBlank()) {
            dbObject.select("Corpus", "answer", content, 0)
        } else {
            dbObject.select("Corpus", "keys", keyWord, 0)
        }
        dbObject.closeDB()
        val r = mutableListOf<String>()
        var jaccardMax = 0.6
        for (i in rList) {
            val formID = i["fromGroup"].toString().toLong()
            if (formID != subject.id && formID != 0L) continue
            val a = mutableListOf<String>()
            val b = mutableListOf<String>()
            val s = PluginMain.LEXER.scan(i["question"].toString())

            s.toList().forEach { a.add(it.toString()) }
            wordList.toList().forEach { b.add(it.toString()) }

            val jaccardIndex = (a intersect b).size.toDouble() / (a union b).size.toDouble()
            when {
                jaccardIndex > jaccardMax -> {
                    jaccardMax = jaccardIndex
                    r.clear()
                    r.add(i["answer"] as String)
                }
                jaccardIndex == jaccardMax -> r.add(i["answer"] as String)
                jaccardIndex < jaccardMax -> continue
            }
        }
        if (r.size > 0) {
            subject.sendMessage(r.random())
            return
        }
        if (atMe) {
            subject.sendMessage("( -`_´- ) (似乎并没有听懂... ")
        }
    }
}