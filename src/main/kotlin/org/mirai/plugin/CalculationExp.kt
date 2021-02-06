package org.mirai.plugin

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.commandPrefix
import net.mamoe.mirai.console.command.MemberCommandSenderOnMessage
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi

// 舰船经验计算器
@ConsoleExperimentalApi
object CalculationExp : SimpleCommand(
    PluginMain, "calculationExp", "舰船经验", "经验计算",
    description = "舰船经验计算器"
) {
    override val usage: String = "${commandPrefix}舰船经验 <当前等级> <目标等级> <是否为决战方案> <已有经验>\n <是否决战方案> 参数只接受“true”及其大小写变体"

    /**
     * 完整功能
     * 返回从 当前等级[current_level]到 目标等级[target_level]所需要的经验值报告。
     * 决战方案[special]和 已有经验[existingExp]因素影响计入报告 */
    @Handler
    suspend fun MemberCommandSenderOnMessage.main(
        current_level: Int,
        target_level: Int,
        special: Boolean,
        existingExp: Int
    ) {
        val balance = (current_level until target_level).fold(0, { accExp: Int, level: Int ->
            val result = accExp + calculateParts(level, special)
            result
        }) - existingExp
        sendMessage("当前等级:$current_level,目标等级:$target_level\n是否为决战方案:$special\n已有经验:$existingExp\n最终计算结果: ${if (balance <= 0) "达成目标等级后将溢出 ${0 - balance} EXP" else "还需 $balance EXP 可以达成目标等级"}")
    }

    /**
     * 仅计算已有经验影响
     * 返回从 当前等级[current_level]到 目标等级[target_level]所需要的经验值报告。
     * 已有经验[existingExp] 因素影响计入报告 */
    @Handler
    suspend fun MemberCommandSenderOnMessage.main(current_level: Int, target_level: Int, existingExp: Int) {
        val balance = (current_level until target_level).fold(0, { accExp: Int, level: Int ->
            val result = accExp + calculateParts(level, false)
            result
        }) - existingExp

        if (balance <= 0) {
            sendMessage("当前等级:$current_level,目标等级:$target_level\n已有经验:$existingExp\n最终计算结果: 达成目标等级后将溢出 ${0 - balance} EXP")
        } else {
            sendMessage("当前等级:$current_level,目标等级:$target_level\n已有经验:$existingExp\n最终计算结果: 还需 $balance EXP 可以达成目标等级")
        }
    }
//    此类重载未能实现支持
//    /**
//     * 仅计算决战方案影响
//     * 返回从 当前等级[current_level]到 目标等级[target_level]所需要的经验值报告。
//     * 决战方案[special] 因素影响计入报告 */
//    @Handler
//    suspend fun MemberCommandSenderOnMessage.main(current_level:Int, target_level:Int, special:Boolean) {
//        val balance = (current_level until target_level).fold(0, {
//            accExp:Int ,level:Int -> val result = accExp + calculateParts(level,special)
//            result
//        })
//        if (balance <= 0){
//            sendMessage("当前等级:$current_level,目标等级:$target_level\n是否为决战方案:$special\n最终计算结果: 达成目标等级后将溢出 $balance EXP")
//        }else{
//            sendMessage("当前等级:$current_level,目标等级:$target_level\n是否为决战方案:$special\n最终计算结果: 还需 $balance EXP 可以达成目标等级")
//        }
//    }
    /**
     * 仅基础计算
     * 返回从 当前等级[current_level]到 目标等级[target_level]所需要的经验值报告。
     * 决战方案 默认为 否
     * 已有经验 默认为 0 */
    @Handler // 基础计算
    suspend fun MemberCommandSenderOnMessage.main(current_level: Int, target_level: Int) {
        val balance = (current_level until target_level).fold(0, { accExp: Int, level: Int ->
            val result = accExp + calculateParts(level, false)
            result
        })
        sendMessage("当前等级:$current_level,目标等级:$target_level\n最终计算结果: 需 $balance EXP 可以达成目标等级")
    }

    /**参数不匹配时输出提示 */
    @Handler
    suspend fun MemberCommandSenderOnMessage.main() {
        sendMessage("参数不匹配, 你是否想执行:\n $usage")
    }

    private fun calculateParts(target_level: Int, special: Boolean): Int {
        val totalExp = when (target_level) {
            in 0..40 -> target_level * 10
            in 41..60 -> 400 + (target_level - 40) * 20
            in 61..70 -> 800 + (target_level - 60) * 30
            in 71..80 -> 1100 + (target_level - 70) * 40
            in 81..90 -> 1500 + (target_level - 80) * 50
            in 101..104 -> 7000 + (target_level - 100) * 200
            in 106..110 -> 8500 + (target_level - 105) * 1200
            in 111..115 -> 14500 + (target_level - 110) * 1800
            in 116..119 -> 23500 + (target_level - 115) * 2100
            else -> when (target_level) {
                91 -> 2100
                92 -> 2200
                93 -> 2400
                94 -> 2600
                95 -> 3000
                96 -> 3500
                97 -> 4000
                98 -> 6000
                99 -> 13200
                100 -> 7000
                105 -> 8500
                120 -> return 3000000
                else -> 0
            }
        }
        return if (special) {
            totalExp * if (target_level in 90..99) 13 else 12
        } else {
            totalExp * 10
        }
    }

}