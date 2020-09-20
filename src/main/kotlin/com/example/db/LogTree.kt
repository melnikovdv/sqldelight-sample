package com.example.db

import timber.log.Timber
import timber.log.Tree
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class LogTree : Tree() {

    override fun performLog(priority: Int, tag: String?, throwable: Throwable?, message: String?) {
        val time = TIME_FORMAT.format(Date(System.currentTimeMillis()))
        val level = letterByPriority(priority)
        val thread = prettify(Thread.currentThread())
        val preparedMessage = prepareMessage(throwable, message)
        val preparedTag = printTag(tag)
        println("$time $level $thread $preparedTag $preparedMessage")
    }

    private fun prepareMessage(throwable: Throwable?, message: String?): String {
        val sb = StringBuilder()
        if (message != null && message.isNotBlank()) {
            sb.append(message)
        }

        if (throwable != null) {
            val stacktrace = stackTraceToString(throwable)
            if (stacktrace.isNotBlank()) {
                if (sb.isNotBlank()) {
                    sb.append(" ")
                }
                sb.append(stacktrace)
            }
        }
        return sb.toString()
    }

    private val fqcnIgnore = listOf(
            Timber::class.java.name,
            Tree::class.java.name,
            LogTree::class.java.name
    )

    private fun printTag(tag: String?): String? {
        return tag ?: Throwable().stackTrace
                .first { it.className !in fqcnIgnore }
                .let(::createStackElementTag)
    }

    companion object {
        private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")
        private val TIME_FORMAT = SimpleDateFormat("yy-MM-dd HH:mm:ss.SSS", Locale.ENGLISH)
    }

    /**
     * Extract the tag which should be used for the message from the `element`. By default
     * this will use the class name without any anonymous class suffixes (e.g., `Foo$1`
     * becomes `Foo`).
     *
     * Note: This will not be called if a [manual tag][.tag] was specified.
     */
    private fun createStackElementTag(element: StackTraceElement): String? {
        var tag = element.className.substringAfterLast('.')
        val m = ANONYMOUS_CLASS.matcher(tag)
        if (m.find()) {
            tag = m.replaceAll("")
        }
        return tag
    }

    private fun stackTraceToString(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString().trim()
    }

    private fun prettify(thread: Thread): String {
        val name = if (thread.name != null && thread.name.isNotEmpty()) {
            thread.name
        } else {
            thread.id.toString()
        }
        return String.format("[%s]", name)
    }

    private fun letterByPriority(priority: Int): String? {
        return when {
            priority < Timber.VERBOSE || priority > Timber.ASSERT -> "D"
            priority == Timber.VERBOSE -> "V"
            priority == Timber.DEBUG -> "D"
            priority == Timber.INFO -> "I"
            priority == Timber.WARNING -> "W"
            priority == Timber.ERROR -> "E"
            else -> "A"
        }
    }
}
