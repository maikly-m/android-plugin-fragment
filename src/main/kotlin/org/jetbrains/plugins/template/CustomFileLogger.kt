package org.jetbrains.plugins.template

import groovy.lang.Singleton
import java.io.File
import java.io.IOException
import java.util.logging.ConsoleHandler
import java.util.logging.FileHandler
import java.util.logging.Formatter
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

class CustomFileLogger {
    companion object {
        private val instance: CustomFileLogger = CustomFileLogger()

        fun getInstance(): CustomFileLogger {
            return instance
        }
    }

    private val logger: Logger = Logger.getLogger(CustomFileLogger::class.java.name)
    private val logOn = true

    init {
        try {
            if (logOn){
                // 暂时不添加
                // 获取根目录的 "test" 文件夹路径
                val logDir = File(System.getProperty("user.home"), "test_")
                if (!logDir.exists()) {
                    logDir.mkdirs()  // 创建文件夹（如果不存在）
                }
                // 创建一个日志文件并添加 FileHandler
                val logFile = File(logDir,"my_test_plugin.log")
                val fileHandler = FileHandler(logFile.absolutePath, false)
                // 设置日志处理器和格式
                fileHandler.formatter = CustomLogFormatter()
                // 使用 FileHandler 输出日志到文件

                logger.addHandler(fileHandler)
            }
            // 可选：同时将日志输出到控制台
//            val consoleHandler = ConsoleHandler()
//            logger.addHandler(consoleHandler)
            // 设置日志等级
            logger.level = Level.ALL
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun logInfo(message: String) {
        if (!logOn){
            return
        }
        logger.info(message)
    }

    fun logDebug(message: String) {
        if (!logOn){
            return
        }
        logger.fine(message)
    }

    fun logError(message: String, throwable: Throwable) {
        if (!logOn){
            return
        }
        logger.log(Level.SEVERE, message, throwable)
    }

    fun logWarn(message: String) {
        if (!logOn){
            return
        }
        logger.warning(message)
    }


    inner class CustomLogFormatter : Formatter() {
        override fun format(record: LogRecord): String {
            // 自定义日志格式
            val logMessage = StringBuilder()

            // 时间戳：格式化时间
            logMessage.append("[${java.time.LocalDateTime.now()}] ")

            // 日志级别
            logMessage.append("[${record.level}] ")

            // 日志来源（类名）
            logMessage.append("[${record.loggerName}] ")

            // 日志消息
            logMessage.append("${record.message}\n")

            // 如果日志包含异常信息，则打印堆栈信息
            if (record.thrown != null) {
                logMessage.append("Exception: ${record.thrown.message}\n")
                record.thrown.printStackTrace()
            }

            return logMessage.toString()
        }
    }
}
