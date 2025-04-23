package com.example.trivia_game.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Logger {
    private const val TAG = "TriviaGame"
    private lateinit var logFile: File
    private var isInitialized = false
    private const val MAX_LOG_AGE_DAYS = 7



    fun initialize(context: Context) {
        if (!isInitialized) {

            val logDir = File(context.getExternalFilesDir(null), "logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            //cleanup old log files older than 90 days
            cleanupOldLogs(logDir)

            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            logFile = File(logDir, "trivia_game_log_$timestamp.txt")
            isInitialized = true

            //log app start
            log("Application started")
        }
    }




    private fun cleanupOldLogs(logDir: File) {
        try {
            val currentTime = System.currentTimeMillis()
            //convert days to milliseconds
            val maxAge = MAX_LOG_AGE_DAYS * 24 * 60 * 60 * 1000L

            logDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.startsWith("trivia_game_log_")) {
                    val fileAge = currentTime - file.lastModified()
                    if (fileAge > maxAge) {
                        if (file.delete()) {
                            Log.i(TAG, "Deleted old log file: ${file.name}")
                        } else {
                            Log.e(TAG, "Failed to delete old log file: ${file.name}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old logs", e)
        }
    }

    fun log(message: String, throwable: Throwable? = null) {
        if (!isInitialized) {
            Log.e(TAG, "Logger not initialized!")
            return
        }
        if (throwable != null) {
            Log.e(TAG, "💥 $message", throwable)
        } else {
            Log.d(TAG, "📱 $message")
        }

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val logMessage = "$timestamp: $message"

        //log to file
        try {
            FileWriter(logFile, true).use { writer ->
                writer.append(logMessage).append('\n')
                throwable?.let {
                    writer.append("Stack trace:\n")
                    writer.append(it.stackTraceToString()).append('\n')
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to log file", e)
        }

        //also log to android system log
        if (throwable != null) {
            Log.e(TAG, logMessage, throwable)
        } else {
            Log.i(TAG, logMessage)
        }
    }

    fun getLogFilePath(): String {
        return logFile.absolutePath
    }
}