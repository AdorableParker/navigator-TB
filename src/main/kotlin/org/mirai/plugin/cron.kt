/*
 * Copyright (c) 2021.
 * 作者: AdorableParker
 * 最后编辑于: 2021/3/23 下午10:26
 */

package org.mirai.plugin

import kotlinx.coroutines.delay
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.utils.info
import net.mamoe.mirai.utils.warning
import java.time.LocalDateTime

data class MyTime(var hours: Int, var minute: Int) {
    fun getNextInteger(starting: MyTime, anyDay: Boolean = false): Long {
        return if (anyDay) {
            val h = starting.hours - hours
            val m = starting.minute - minute
            val final = when {
                h < 0 -> (24 + h) * 60 + m
                h == 0 -> if (m < 0) 24 * 60 + m else m
                else -> h * 60 + m
            }
            final * 60_000L
        } else {
            if (starting.hours == 0) ((minute / 10 + 1) * 10 - minute) * 60_000L else (60 - minute) * 60_000L
        }
    }

    fun getSleepStamp(): Long {
        return (hours * 60 + minute) * 60_000L
    }
}

class CronJob(private val jobExplain: String, private val calibration: Int) {
    private var flag = false
    private var calibrationCountdown = 0
    private var jobList = mutableListOf<suspend () -> Unit>()

    @ConsoleExperimentalApi
    suspend fun start(period: MyTime) {
        val startTime = MyTime(
            LocalDateTime.now().hour,
            LocalDateTime.now().minute
        ).getNextInteger(period)
        PluginMain.logger.info { "$jobExplain start sleep: $startTime ms" }
        flag = true
        delay(startTime)
        while (flag) {
            PluginMain.logger.info { "执行作业：$jobExplain" }
            kotlin.runCatching {
                for (job in jobList) {
                    job()
                }
            }.onSuccess {
                PluginMain.logger.info { "$jobExplain 执行完毕" }
            }.onFailure {
                PluginMain.logger.warning { "$jobExplain 执行异常" }
            }
            if (calibrationCountdown >= calibration) {
                PluginMain.logger.info { "$jobExplain 执行校准" }
                calibrationCountdown = 0
                val nextTime = MyTime(
                    LocalDateTime.now().hour,
                    LocalDateTime.now().minute
                ).getNextInteger(period)
                PluginMain.logger.info { "$jobExplain 到下次执行睡眠$nextTime" }
                delay(nextTime)
            } else {
                delay(period.getSleepStamp())
                calibrationCountdown++      // 计数增加
            }
        }
    }

    @ConsoleExperimentalApi
    suspend fun start(period: MyTime, starting: MyTime) {
        val startTime = MyTime(
            LocalDateTime.now().hour,
            LocalDateTime.now().minute
        ).getNextInteger(starting, true)
        PluginMain.logger.info { "$jobExplain start sleep: $startTime ms" }
        flag = true
        delay(startTime)
        while (flag) {
            PluginMain.logger.info { "执行作业：$jobExplain" }
            for (job in jobList) {
                job()
            }
            PluginMain.logger.info { "$jobExplain 执行完毕" }
            if (calibrationCountdown >= 120) {
                PluginMain.logger.info { "$jobExplain 执行校准" }
                calibrationCountdown = 0
                val nextTime = MyTime(
                    LocalDateTime.now().hour,
                    LocalDateTime.now().minute
                ).getNextInteger(starting, true)
                PluginMain.logger.info { "$jobExplain 到下次执行睡眠$nextTime" }
                delay(nextTime)
            } else {
                delay(period.getSleepStamp())
                calibrationCountdown++      // 计数增加
            }
        }
    }

    fun addJob(job: suspend () -> Unit) {
        jobList.add(job)
    }
}