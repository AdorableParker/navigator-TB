/*
 * Copyright (c) 2021.
 * 作者: AdorableParker
 * 最后编辑于: 2021/3/7 上午9:55
 */

package org.mirai.plugin

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.MemberCommandSenderOnMessage
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.info
import org.jsoup.Jsoup
import org.mirai.plugin.MySetting.SauceNAOKey


@ConsoleExperimentalApi
object SauceNAO : SimpleCommand(
    PluginMain, "SauceNAO", "搜图",
    description = "以图搜图"
) {
    override val usage: String = "${CommandManager.commandPrefix}搜图 [图片]"

    @Handler
    suspend fun MemberCommandSenderOnMessage.main(image: Image) {
        sendMessage("开始查询，请稍后...")
        val jsonObjString = getJSON(image.queryUrl())
        if (jsonObjString == null) {
            sendMessage("远端服务器超时或无响应,请稍后再试")
            return
        }
        val jsonObj = Parser.default().parse(StringBuilder(jsonObjString)) as JsonObject
        val uid = jsonObj.obj("header")?.string("user_id")?.toInt()
        if (uid == null || uid <= 0) {
            sendMessage("远端服务器运行异常,请稍后再试")
            return
        }
        val status = jsonObj.obj("header")?.int("status") ?: 1
        when {
            status < 0 -> {
                sendMessage("远端服务器获取图片或解析图片格式失败")
                return
            }
            status > 0 -> {
                sendMessage("远端服务器搜索数据库失败,请稍后再试")
                return
            }
            else -> {
            }
        }
        val numberOfResults = jsonObj.obj("header")?.int("results_requested")
        if (numberOfResults == null || numberOfResults <= 0) {
            sendMessage("无匹配结果")
            return
        }
        val results = jsonObj.array<JsonObject>("results")?.get(0)
        if (results == null) {
            sendMessage("无效结果返回")
            return
        }
        val similarity = results.obj("header")?.string("similarity")?.toFloat()
        val source = when (results.obj("header")?.int("index_id")) {
            0 -> "h-mags"
            1 -> "h-anime"
            2 -> "hcg"
            5 -> "pixiv"
            6 -> "pixiv历史记录"
            8 -> "seige"
            9 -> "danbooru"
            10 -> "drawr"
            11 -> "nijie"
            12 -> "yande.re"
            16 -> "FAKKU"
            18 -> "H-MISC安全"
            19 -> "2d_market"
            20 -> "medibang"
            21 -> "Anime"
            22 -> "H-Anime"
            23 -> "Movies"
            24 -> "Shows"
            25 -> "gelbooru"
            26 -> "konachan"
            27 -> "sankaku"
            28 -> "anime-pictures"
            29 -> "e621"
            30 -> "idol complex"
            31 -> "半次元插画版面"
            32 -> "半次元COS版面"
            33 -> "portalgraphics"
            34 -> "dA"
            35 -> "pawoo"
            36 -> "madokami"
            37 -> "mangadex"
            38 -> "H-Misc非安全"
            39 -> "ArtStation"
            40 -> "FurAffinity"
            41 -> "Twitter"
            42 -> "Furry Network"
            else -> "未知索引"
        }
        val extUrls = results.obj("data")?.array<String>("ext_urls")?.joinToString()
        val data = results.obj("data")?.toJsonString(true)
        val remain = jsonObj.obj("header")?.int("long_remaining").toString()
        if (similarity != null && data != null) {
            sendMessage(writeReport(similarity, source, extUrls, data, remain))
        } else {
            sendMessage("应该不会运行到这里，如果看见这句话请联系管理员")
        }
    }

    @ConsoleExperimentalApi
    private fun getJSON(img: String): String? {
        val url = if (SauceNAOKey == "你的Key") "https://saucenao.com/search.php?output_type=2&numres=1&db=999&url=$img"
        else "https://saucenao.com/search.php?output_type=2&numres=1&db=999&api_key=$SauceNAOKey&url=$img"
        runCatching {
            Jsoup.connect(url)
                .ignoreContentType(true)
                .execute()
                .body()
                .toString()
        }.onSuccess {
            return it
        }.onFailure {
            PluginMain.logger.info { "$it" }
            return null
        }
        return null
    }

    private fun writeReport(similarity: Float, source: String, extUrls: String?, data: String = "", v: String): String {
        return if (extUrls != null) "相似度：$similarity\n来源平台：$source\n相关链接：$extUrls\n24小时内剩余搜索次数：$v"
        else "相似度：$similarity\n来源平台：$source\n相关链接：未能获得\n详细源数据：$data\n24小时内剩余搜索次数：$v"
    }
}