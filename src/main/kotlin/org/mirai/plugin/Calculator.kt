package org.mirai.plugin

import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.MemberCommandSenderOnMessage
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.math.PI

@ConsoleExperimentalApi
object Calculator : SimpleCommand(
    PluginMain, "Calculator", "计算器", "计算",
    description = "计算器"
) {
    override val usage = "${CommandManager.commandPrefix}计算 <中缀表达式>"

    @Handler
    suspend fun MemberCommandSenderOnMessage.main(express: String) {
        sendMessage(analysis(express))
    }

    private fun charToNum(list: MutableList<Char>) = list.joinToString("").toBigDecimalOrNull()
    private fun comparePriority(o1: Char, o2: Char): Boolean = getPriorityValue(o1) > getPriorityValue(o2)
    private fun getPriorityValue(str: Char): Int = when (str) {
        '+', '-' -> 1
        '*', '/', '%' -> 2
        else -> 0
    }

    private fun operation(back: BigDecimal, first: BigDecimal, sign: Char) = when (sign) {
        '+' -> first.add(back)
        '-' -> first.subtract(back)
        '*' -> first.multiply(back)
        '/' -> first.divide(back, 24, RoundingMode.HALF_UP)
        '%' -> first.remainder(back)
        else -> null
    }

    private fun analysis(rpn: String): String {
        val stack = Stack<Char>()
        val stackRPN = Stack<BigDecimal>()
        val list: MutableList<Char> = ArrayList()
        for (i in rpn.indices) {
            when {
                (rpn[i].toString()).matches(Regex("""[\d.]""")) -> list.add(rpn[i])
                rpn[i] == '(' -> stack.push(rpn[i])
                rpn[i] == ')' -> {
                    while ('(' != stack.lastElement()) {
                        list.add(stack.pop())
                    }
                    stack.pop()
                }
                (rpn[i].toString()).matches(Regex("""[+\-*/%π]""")) -> {
                    if (list.isEmpty()) {
                        when (rpn[i]) {
                            '-' -> {
                                stackRPN.push(BigDecimal(-1.0));stack.push('*')
                            }
                            'π' -> stackRPN.push(BigDecimal(PI))
                        }
                        continue
                    }
                    charToNum(list)?.let { stackRPN.push(it) } ?: let { return "错误,'${list.joinToString("")}'不是有效的数字" }
                    list.clear()
                    if (rpn[i] == 'π') {
                        stackRPN.push(operation(BigDecimal(3.14159), stackRPN.pop(), '*'))
                        continue
                    }
                    if (stack.isEmpty()) {
                        stack.push(rpn[i])
                        continue
                    }

                    while (!stack.isEmpty() &&
                        stack.lastElement() != '(' &&
                        !comparePriority(rpn[i], stack.lastElement())
                    ) {
                        val sign = stack.pop()
                        operation(stackRPN.pop(), stackRPN.pop(), sign)?.let { stackRPN.push(it) }
                            ?: let { return "错误,意外的运算符 '$sign',预期得到'+', '-', '*', '/', '%', '(', ')'" }
                    }
                    stack.push(rpn[i])
                }
            }
        }
        if (list.isNotEmpty()) {
            charToNum(list)?.let { stackRPN.push(it) } ?: let { return "Error,illegal Number" }
            list.clear()
        }
        while (!stack.isEmpty()) {
            val sign = stack.pop()
            operation(stackRPN.pop(), stackRPN.pop(), sign)?.let { stackRPN.push(it) }
                ?: let { return "错误,意外的运算符 '$sign',预期得到'+', '-', '*', '/', '%', '(', ')'" }
        }
        return stackRPN.pop().toString()
    }
}