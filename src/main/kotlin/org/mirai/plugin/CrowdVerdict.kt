/*
 * Copyright (c) 2021.
 * 作者: AdorableParker
 * 最后编辑于: 2021/3/15 下午7:09
 */

package org.mirai.plugin

import net.mamoe.mirai.console.command.MemberCommandSenderOnMessage
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.utils.info
import java.time.LocalDateTime
import java.time.ZoneOffset

data class VoteUser(var vote: Int = 0, val idList: MutableList<Long> = mutableListOf(), val startTime: Long) {
    fun poll(i: Int, voter: Long): Int {
        idList.add(voter)
        vote += i
        return vote
    }
}

@ConsoleExperimentalApi
object CrowdVerdict : SimpleCommand(
    PluginMain, "CrowdVerdict", "众裁",
    description = "众裁禁言"
) {
    @Handler
    suspend fun MemberCommandSenderOnMessage.main(target: Member) {
        val name = if (target.nameCard == "") target.nick else target.nameCard
        val nameB = if (user.nameCard == "") user.nick else user.nameCard
        val t = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        val voteUser = if (PluginMain.VOTES[target.id] != null &&
            t - PluginMain.VOTES[target.id]!!.startTime <= 600
        ) {
            PluginMain.VOTES[target.id]
        } else {
            sendMessage("${nameB}发起了对${name}的众裁")
            PluginMain.VOTES[target.id] = VoteUser(startTime = t)
            PluginMain.VOTES[target.id]
        }
        if (voteUser == null) {
            sendMessage("众裁系统异常")
            return
        }
        if (voteUser.idList.indexOf(user.id) == -1) {
            sendMessage("达成进度(${voteUser.idList.size}/10)\n通过后,被裁决者将会被禁言${voteUser.poll(2, user.id)}分钟")
        } else {
            sendMessage("你已经投过票了")
        }
        if (voteUser.idList.size >= 10) {
            runCatching {
                target.mute(voteUser.vote * 60)
            }.onSuccess {
                PluginMain.VOTES.remove(target.id)
                sendMessage("众裁通过,$name 被执行裁决")
            }.onFailure {
                sendMessage("执行失败,请检查权限")
            }
        }
        PluginMain.logger.info { "${PluginMain.VOTES}" }
    }

    @Handler
    suspend fun MemberCommandSenderOnMessage.main(target: Member, vote: Int) {
        if (vote <= 0) {
            sendMessage("反对无效")
            return
        }
        val name = if (target.nameCard == "") target.nick else target.nameCard
        val nameB = if (user.nameCard == "") user.nick else user.nameCard
        val t = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        val voteUser = if (PluginMain.VOTES[target.id] != null &&
            t - PluginMain.VOTES[target.id]!!.startTime <= 600
        ) {
            PluginMain.VOTES[target.id]
        } else {
            sendMessage("${nameB}发起了对${name}的众裁")
            PluginMain.VOTES[target.id] = VoteUser(startTime = t)
            PluginMain.VOTES[target.id]
        }
        if (voteUser == null) {
            sendMessage("众裁系统异常")
            return
        }
        PluginMain.logger.info { "${voteUser.idList}" }
        if (voteUser.idList.indexOf(user.id) == -1) {
            if (vote >= 2) {
                runCatching {
                    user.mute(vote * 30)
                }.onSuccess {
                    sendMessage(
                        "${nameB}加码,达成进度(${voteUser.idList.size}/10)\n众裁通过后,被裁决者将会被禁言${
                            voteUser.poll(
                                vote,
                                user.id
                            )
                        }分钟"
                    )
                }.onFailure {
                    sendMessage("执行失败,请检查权限")
                }
            } else {
                sendMessage("达成进度(${voteUser.idList.size}/10)\n通过后,被裁决者将会被禁言${voteUser.poll(vote, user.id)}分钟")
            }
        } else {
            sendMessage("你已经投过票了")
        }
        if (voteUser.idList.size >= 10) {
            sendMessage("众裁通过，$name 被执行裁决")
            runCatching {
                target.mute(voteUser.vote * 60)
            }.onSuccess {
                PluginMain.VOTES.remove(target.id)
                sendMessage("众裁通过,$name 被执行裁决")
            }.onFailure {
                sendMessage("执行失败,请检查权限")
            }
        }
        PluginMain.logger.info { "${PluginMain.VOTES}" }
    }
}