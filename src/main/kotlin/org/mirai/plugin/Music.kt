/*
 * Copyright (c) 2021.
 * 作者: AdorableParker
 * 最后编辑于: 2021/4/17 下午3:14
 */

package org.mirai.plugin

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.MemberCommandSenderOnMessage
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.MusicKind
import net.mamoe.mirai.message.data.MusicShare
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.utils.debug
import net.mamoe.mirai.utils.warning
import org.jsoup.Jsoup

@ConsoleExperimentalApi
object Music : SimpleCommand(
    PluginMain, "music", "点歌",
    description = "点歌姬"
) {
    override val usage: String = "${CommandManager.commandPrefix}$primaryName [平台值:默认网易云]\n1\t网易云\n2\tQQ\n3\t咪咕"

    @Handler
    suspend fun MemberCommandSenderOnMessage.main(musicName: String) {
        runCatching {
            val rMessage = get163MusicCard(musicName)
            sendMessage(rMessage)
        }.onFailure {
            PluginMain.logger.warning { it.toString() }
            sendMessage("点歌失败，未知的失败原因")
        }
    }

    @Handler
    suspend fun MemberCommandSenderOnMessage.main(musicName: String, type: Int) {
        runCatching {
            val rMessage = when (type) {
                1 -> get163MusicCard(musicName)
                2 -> getQQMusicCard(musicName)
                3 -> getMGMusicCard(musicName)
                else -> PlainText("不认识的搜索源")
            }
            sendMessage(rMessage)
        }.onFailure {
            PluginMain.logger.warning { it.toString() }
            sendMessage("点歌失败，未知的失败原因")
        }
    }

    private fun get163MusicCard(musicName: String): Message {
        val doc = Jsoup.connect("https://music.163.com/api/search/get/web?type=1&s=$musicName")
            .ignoreContentType(true)
            .execute().body().toString()
        val jsonObj = Parser.default().parse(StringBuilder(doc)) as JsonObject
        if (jsonObj.int("code") != 200)
            return PlainText("搜索歌曲异常\nHTTP状态码：${jsonObj.int("code")}")

        val musicList = jsonObj.obj("result")
        if (musicList.isNullOrEmpty() || musicList.int("songCount")!! <= 0)
            return PlainText("搜索结果列表为空")

        val musicInfo = musicList.array<JsonObject>("songs")?.get(0)
        if (musicInfo.isNullOrEmpty()) return PlainText("获取歌曲信息失败")

        val title = musicInfo.string("name")!!
        val musicID = musicInfo.long("id")
        val pictureUrl = get163PIUrl("https://music.163.com/api/search/get/web?type=1000&s=$musicName").let {
            if (it.isNullOrBlank()) "https://p2.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg" else it
        }

        return MusicShare(
            MusicKind.NeteaseCloudMusic,
            title,
            "歌曲信息 来源于 领航员-TB 智障搜索",                // 内容:会显示在title下面
            "https://music.163.com/#/song?id=$musicID",
            pictureUrl,                                             // 图片链接
            "http://music.163.com/song/media/outer/url?id=$musicID.mp3",
            ""                                              // 摘要:不知道有什么用
        )
    }

    @Suppress("UNREACHABLE_CODE") // Mark： 有无法到达的代码
    private fun getQQMusicCard(musicName: String): Message {
        return PlainText("你搜索的内容是“$musicName”，但是这个引擎还没做好")
        val doc = Jsoup.connect("https://c.y.qq.com/soso/fcgi-bin/client_search_cp?format=json&w=$musicName")
            .ignoreContentType(true)
            .execute().body().toString()
        val jsonObj = Parser.default().parse(StringBuilder(doc)) as JsonObject
        if (jsonObj.int("code") != 0)
            return PlainText("搜索歌曲异常\n状态码：${jsonObj.int("code")}")

        val song = jsonObj.obj("data")?.obj("song")
        if (song.isNullOrEmpty() || song.int("curnum")!! <= 0)
            return PlainText("搜索结果列表为空")

        val musicInfo = song.array<JsonObject>("list")?.get(0)
        if (musicInfo.isNullOrEmpty()) return PlainText("获取歌曲信息失败")

        val title = musicInfo.string("songname")!!
        val musicID = musicInfo.long("songid")
        val jumpUrl = musicInfo.string("songurl")!!
        val pictureUrl =
            getQQPIUrl("https://c.y.qq.com/soso/fcgi-bin/client_search_cp?format=json&t=8&w=$musicName").let {
                if (it.isNullOrBlank()) "https://p2.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg" else it
            }

        return MusicShare(
            MusicKind.QQMusic,
            title,
            "歌曲信息 来源于 领航员-TB 智障搜索",                // 内容:会显示在title下面
            jumpUrl,
            pictureUrl,                                             // 图片链接
            "http://music.163.com/song/media/outer/url?id=$musicID.mp3",
            ""                                              // 摘要:不知道有什么用
        )
    }

    private fun getMGMusicCard(musicName: String): Message {
        val doc = Jsoup.connect("https://m.music.migu.cn/v3/search?type=song&keyword=$musicName")
            .ignoreContentType(true)
            .execute().body().toString()
        val jsonObj = Parser.default().parse(StringBuilder(doc)) as JsonObject
        PluginMain.logger.debug { "执行到这里" }
        if (jsonObj.boolean("success") != true)
            return PlainText("搜索歌曲失败")

        val musicInfo = jsonObj.array<JsonObject>("musics")?.get(0)
        if (musicInfo.isNullOrEmpty()) return PlainText("获取歌曲信息失败")

        val title = musicInfo.string("singerName")!!
        val musicUrl = musicInfo.string("mp3")!!
        val pictureUrl = musicInfo.string("cover")!!
        val musicID = musicInfo.long("id")

        return MusicShare(
            MusicKind.MiguMusic,
            title,
            "歌曲信息 来源于 领航员-TB 智障搜索",                // 内容:会显示在title下面
            "https://m.music.migu.cn/v3/music/song/$musicID",
            pictureUrl,                                             // 图片链接
            musicUrl,
            ""                                              // 摘要:不知道有什么用
        )
    }

    private fun get163PIUrl(url: String): String? {
        kotlin.runCatching {
            val jsonObj = Parser.default().parse(
                StringBuilder(
                    Jsoup.connect(url).ignoreContentType(true).execute().body().toString()
                )
            ) as JsonObject
            return jsonObj.obj("result")?.array<JsonObject>("playlists")?.get(0)?.string("coverImgUrl")
        }.onFailure {
            PluginMain.logger.warning { "获取图片时异常\n$it" }
        }
        return "https://p2.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg"
    }

    private fun getQQPIUrl(url: String): String? {
        kotlin.runCatching {
            val jsonObj = Parser.default().parse(
                StringBuilder(
                    Jsoup.connect(url).ignoreContentType(true).execute().body().toString()
                )
            ) as JsonObject
            return jsonObj.obj("result")?.array<JsonObject>("playlists")?.get(0)?.string("coverImgUrl")
        }.onFailure {
            PluginMain.logger.warning { "获取图片时异常\n$it" }
        }
        return "https://p2.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg"
    }
}