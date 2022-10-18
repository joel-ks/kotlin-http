package jks.utilities

import java.util.ArrayDeque

class AutoCloseScope : AutoCloseable {
    private val resourceStack = ArrayDeque<AutoCloseable>()

    fun <T : AutoCloseable> T.closeWhenDone(): T {
        resourceStack.addLast(this)
        return this
    }

    override fun close() {
        var exception: Exception? = null

        while (resourceStack.size > 0) {
            try {
                resourceStack.removeLast().close()
            } catch (e: Exception) {
                if (exception == null) exception = e
                else exception.addSuppressed(e)
            }
        }

        if (exception != null) throw exception
    }
}

fun using(block: AutoCloseScope.() -> Unit) {
    AutoCloseScope().use(block)
}
