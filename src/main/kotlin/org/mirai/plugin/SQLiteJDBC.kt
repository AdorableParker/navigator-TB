/*
 * Copyright (c) 2021.
 * 作者: AdorableParker
 * 最后编辑于: 2021/3/7 上午9:54
 */

package org.mirai.plugin

import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.utils.info
import net.mamoe.mirai.utils.warning
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement
import java.util.*
import kotlin.system.exitProcess


@ConsoleExperimentalApi
class SQLiteJDBC(DbPath: Path) {
    private var c: Connection? = null

    init {
        try {
            Class.forName("org.sqlite.JDBC")
            c = DriverManager.getConnection("jdbc:sqlite:$DbPath")
            c?.autoCommit = false
        } catch (e: Exception) {
            PluginMain.logger.warning(e.javaClass.name + ": " + e.message)
            exitProcess(0)
        }
    }

    private var stmt: Statement? = null

    /**
     * 创建表
     * 以[sql]作为SQL语句创建表
     */
    fun createTable(sql: String) {
        if (executeSQL(sql) < 0) {
            PluginMain.logger.warning { "执行SQL创建表操作异常" }
        }
    }

    /**
     * 插入
     * 以[column]作为目标字段名，[value]作为目标值插入目标表[table]内
     */
    fun insert(table: String, column: Array<String>, value: Array<String>) {
        val sql = "INSERT INTO $table " +
                "${column.joinToString(",", "(", ")")} VALUES " +
                "${value.joinToString(",", "(", ")")};"
        PluginMain.logger.info { sql }
        if (executeSQL(sql) < 0) {
            PluginMain.logger.warning { "执行SQL插入操作异常" }
        }
    }


    /**
     * 更改
     * 将目标表[table]内的所有记录中字段[column]的值等于[value]的记录的字段[key]的值更改为[data]
     *
     * UPDATE $table SET $Array.key = $Array.data WHERE $column = $value;
     **/
    fun update(table: String, column: String, value: String, key: Array<String>, data: Array<String>) {
        val sql = "UPDATE $table SET " +
            "${key.joinToString(",", "(", ")")} = " +
            "${data.joinToString(",", "(", ")")} WHERE " +
            "$column = $value;"
        if (executeSQL(sql) < 0) {
            PluginMain.logger.warning { "执行SQL更改操作异常" }
        }
    }

    /**
     * 更改
     * 将目标表[table]内的所有记录中字段[column]的值等于[value]的记录的字段[key]的值更改为[data]
     *
     * UPDATE $table SET $key = $data WHERE $column = $value;
     **/
    fun update(table: String, column: String, value: Any, key: String, data: Any) {
        val sql = "UPDATE $table SET $key = $data WHERE $column = $value;"
        if (executeSQL(sql) < 0) {
            PluginMain.logger.warning { "执行SQL更改操作异常" }
        }
    }

    /**
     * 删除
     * 删除目标表[table]内所有记录中字段[column]的值等于[value]的记录
     */
    fun delete(table: String, column: String, value: String) {
        val sql = "DELETE FROM $table WHERE $column = $value;"
        if (executeSQL(sql) < 0) {
            PluginMain.logger.warning { "执行SQL删除操作异常" }
        }
    }

    fun delete(table: String, column: List<String>, value: List<String>, conjunction: String) {
        val valueIterator = value.iterator()
        val determiner: MutableList<String> = ArrayList()
        column.forEach { determiner.add("$it = ${valueIterator.next()}") }
        val sql = "DELETE FROM $table WHERE ${determiner.joinToString(" $conjunction ")};"
        if (executeSQL(sql) < 0) {
            PluginMain.logger.warning { "执行SQL删除操作异常" }
        }
    }

    /**
     * 读取并返回目标表[table]内的所有记录中字段[column]
     * 等于或包含[value]的所有数据 (不需要额外引号)
     * [mods]参数决定匹配模式
     * 0 -> 全等字符串
     * 1 -> 不做处理
     * 2 -> 尾部为value的字符串
     * 3 -> 头部为value的字符串
     * 4 -> 包含有value的字符串
     * 5 -> 不等于value，value不做处理
     */
    fun select(
        table: String,  // 目标表名
        column: String,  // 限制字段
        value: Any,  // 限制值
        mods: Int = 0
    ): MutableList<MutableMap<String?, Any?>> {
        val resultList: MutableList<MutableMap<String?, Any?>> = ArrayList()

        val sql: String = when (mods) {
            1 -> "SELECT * FROM $table WHERE $column = $value;"
            2 -> "SELECT * FROM $table WHERE $column GLOB '*$value';"
            3 -> "SELECT * FROM $table WHERE $column GLOB '$value*';"
            4 -> "SELECT * FROM $table WHERE $column GLOB '*$value*';"
            5 -> "SELECT * FROM $table WHERE $column != $value;"
            else -> "SELECT * FROM $table WHERE $column = '$value';"
        }
        try {
            stmt = c?.createStatement()
            val rs: ResultSet? =
                stmt?.executeQuery(sql)
            if (rs != null) {
                val metadata = rs.metaData
                val columnCount = metadata.columnCount
                while (rs.next()) {
                    val row: MutableMap<String?, Any?> = mutableMapOf()
                    for (i in 1..columnCount) {
                        row[metadata.getColumnName(i)] = rs.getObject(i)
                    }
                    resultList.add(row)
                }
                rs.close()
            }
            stmt?.close()
        } catch (e: java.lang.Exception) {
            PluginMain.logger.warning { e.javaClass.name + ": " + e.message }
            exitProcess(0)
        }
        return resultList
    }

    /**
     * 读取并返回目标表[table]内的所有记录中字段[column]
     * 等于或包含[value]的所有数据
     *
     * 多个条件之间使用连接词[conjunction]连接
     * [mods]参数决定匹配模式
     * @param [table] 目标表名
     * @param [column] 限制字段
     * @param [value] 限制值(不需要额外引号)
     * @param [conjunction] 连接词
     * @param [mods] 匹配模式(0:全等字符串,1:不做处理,2:尾部为value的字符串,3:头部为value的字符串,4:包含有value的字符串)
     */
    fun select(
        table: String,  // 目标表名
        column: List<String>,  // 限制字段
        value: List<String>,  // 限制值
        conjunction: String,  // 连接词
        mods: Int = 0  // 全等匹配
    ): MutableList<MutableMap<String?, Any?>> {
        val resultList: MutableList<MutableMap<String?, Any?>> = ArrayList()
        val valueIterator = value.iterator()
        val determiner: MutableList<String> = ArrayList()
        when (mods) {
            1 -> column.forEach { determiner.add("$it = ${valueIterator.next()}") }
            2 -> column.forEach { determiner.add("$it GLOB '*${valueIterator.next()}'") }
            3 -> column.forEach { determiner.add("$it GLOB '${valueIterator.next()}*'") }
            4 -> column.forEach { determiner.add("$it GLOB '*${valueIterator.next()}*'") }
            else -> column.forEach { determiner.add("$it = '${valueIterator.next()}'") }
        }
        val sql = "SELECT * FROM $table WHERE ${determiner.joinToString(" $conjunction ")};"
        try {
            stmt = c?.createStatement()
            val rs: ResultSet? = stmt?.executeQuery(sql)
            if (rs != null) {
                val metadata = rs.metaData
                val columnCount = metadata.columnCount
                while (rs.next()) {
                    val row: MutableMap<String?, Any?> = mutableMapOf()
                    for (i in 1..columnCount) {
                        row[metadata.getColumnName(i)] = rs.getObject(i)
                    }
                    resultList.add(row)
                }
                rs.close()
            }
            stmt?.close()
        } catch (e: java.lang.Exception) {
            PluginMain.logger.warning { e.javaClass.name + ": " + e.message }
            exitProcess(0)
        }
        return resultList
    }

    fun executeStatement(sql: String): MutableList<MutableMap<String?, Any?>> {
        val resultList: MutableList<MutableMap<String?, Any?>> = ArrayList()
        try {
            stmt = c?.createStatement()
            val rs: ResultSet? = stmt?.executeQuery(sql)
            if (rs != null) {
                val metadata = rs.metaData
                val columnCount = metadata.columnCount
                while (rs.next()) {
                    val row: MutableMap<String?, Any?> = mutableMapOf()
                    for (i in 1..columnCount) {
                        row[metadata.getColumnName(i)] = rs.getObject(i)
                    }
                    resultList.add(row)
                }
                rs.close()
            }
            stmt?.close()
        } catch (e: java.lang.Exception) {
            PluginMain.logger.warning { e.javaClass.name + ": " + e.message }
            exitProcess(0)
        }
        return resultList
    }


    /**
     * 关闭数据库
     */
    fun closeDB() {
        c?.close()
    }

    /**
     * 用于执行非查询语句
     */
    private fun executeSQL(sql: String): Int {
        return try {
            stmt = c?.createStatement()
            val r = stmt?.executeUpdate(sql)
            stmt?.close()
            c?.commit()
            r ?: -1
        } catch (e: java.lang.Exception) {
            PluginMain.logger.warning { e.javaClass.name + ": " + e.message }
            -1
        }
    }
}