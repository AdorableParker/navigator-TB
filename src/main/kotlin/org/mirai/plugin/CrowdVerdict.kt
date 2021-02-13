/*
 * Copyright (c) 2021.
 * 作者: AdorableParker
 * 最后编辑于: 2021/2/14 上午3:11
 */

package org.mirai.plugin

import net.mamoe.mirai.console.command.MemberCommandSenderOnMessage
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.utils.info

data class VoteUser(var vote: Int = 0, val idList: MutableList<Long> = mutableListOf()) {

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
    suspend fun MemberCommandSenderOnMessage.main(uid: Long) {
        PluginMain.logger.info { "测试命令执行" }
        val crowdReferees = group.members[uid]
        if (crowdReferees == null) {
            sendMessage("本群查无此人")
            return
        }
        val name = if (crowdReferees.nameCard == "null") crowdReferees.nameCard else crowdReferees.nick
        val nameB = if (user.nameCard == "null") user.nameCard else user.nick

        val voteUser = if (PluginMain.VOTELIST[uid] != null) PluginMain.VOTELIST[uid] else {
            sendMessage("${nameB}发起了对于 $name 的众裁")
            PluginMain.VOTELIST[uid] = VoteUser()
            PluginMain.VOTELIST[uid]
        }
        if (voteUser == null) {
            sendMessage("众裁系统异常")
            return
        }
        PluginMain.logger.info { "${voteUser.idList}" }
        if (voteUser.idList.indexOf(user.id) == -1) {
            sendMessage("众裁票数+1,目前票数${voteUser.poll(1, user.id)}")
        } else {
            sendMessage("你已经投过票了")
        }
        if (voteUser.idList.size >= 10) {
            sendMessage("众裁通过，$name 被执行裁决")
            crowdReferees.mute(voteUser.vote * 60)
        }
    }

    @Handler
    suspend fun MemberCommandSenderOnMessage.main(uid: Long, vote: Int) {
        PluginMain.logger.info { "测试命令执行" }
        val crowdReferees = group.members[uid]
        if (crowdReferees == null) {
            sendMessage("本群查无此人")
            return
        }
        val name = if (crowdReferees.nameCard == "null") crowdReferees.nameCard else crowdReferees.nick
        val nameB = if (user.nameCard == "null") user.nameCard else user.nick

        val voteUser = if (PluginMain.VOTELIST[uid] != null) PluginMain.VOTELIST[uid] else {
            sendMessage("${nameB}发起了对于 $name 的众裁")
            PluginMain.VOTELIST[uid] = VoteUser()
            PluginMain.VOTELIST[uid]
        }
        if (voteUser == null) {
            sendMessage("众裁系统异常")
            return
        }
        PluginMain.logger.info { "${voteUser.idList}" }
        if (voteUser.idList.indexOf(user.id) == -1) {
            sendMessage("众裁票数+1,目前票数${voteUser.poll(vote, user.id)}")
        } else {
            sendMessage("你已经投过票了")
        }
        if (voteUser.idList.size >= 10) {
            sendMessage("众裁通过，$name 被执行裁决")
            crowdReferees.mute(voteUser.vote * 60)
        }
    }
}