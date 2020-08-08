package tice.helper

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions
import java.util.*

class FlowTest<T> {
    val results = LinkedList<T>()

    val nextValue: T
        get() = results.pollFirst()

    infix fun assertNextEquals(block: () -> T) = Assertions.assertEquals(block(), nextValue)

    fun assertNoMoreValues(): Unit = Assertions.assertTrue(results.size == 0)
}

suspend inline infix fun <T> Flow<T>.testWithTimeout(block: FlowTest<T>.() -> Unit) {
    val flowTest = FlowTest<T>()

    try {
        withTimeout(5000L) {
            collect {
                flowTest.results.add(it)
            }
        }
    } catch (e: TimeoutCancellationException) {
        flowTest.block()
    }
}
