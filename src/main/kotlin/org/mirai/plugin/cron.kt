package org.mirai.plugin

import kotlinx.coroutines.delay
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.utils.info
import java.time.LocalDateTime

data class MyTime(var hours: Int, var minute: Int) {
    fun getNextInteger(flag: MyTime): Long {
        return if (flag.hours == 0) ((minute / 10 + 1) * 10 - minute) * 60_000L else (60 - minute) * 60_000L
//        return if (minute/10*10>=60)((10-minute % 10)*60_000L) else
    }

    fun getSleepStamp(): Long {
        return (hours * 60 + minute) * 60_000L
    }
//        return if (minute % 10 >0){
//            if(minute + 10 - minute % 10<=60){
//                10 - minute % 10
//                minute = min-60
//                hours++
//                if(hours>=24){
//                    hours = 0
//                }
//        }else{
//            0
//        }
//
//    }else{
//    0
//    }
}

class CronJob(explain: String) {
    private val jobExplain = explain
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
                ).getNextInteger(period)
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