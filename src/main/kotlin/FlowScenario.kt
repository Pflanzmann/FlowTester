package tice.helper

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.fail
import java.util.*


class FlowScenario<T>(private val flow: Flow<T>) {
    internal val results = LinkedList<T>()
    internal val steps = mutableMapOf<Int, Step<T>?>()
    internal var currentValue = 0

    internal val nextValue: T?
        get() = results.poll()

    var timeOut: Long = Long.MAX_VALUE

    var finishedWithTimeout = false
    var thrownException: Exception? = null

    suspend fun startScenario(timeOut: Long = this.timeOut) {
        try {
            withTimeoutOrNull(timeOut) {
                flow.collect {
                    results.add(it)
                    steps[currentValue]?.invoke().also { steps.remove(currentValue) }
                    currentValue++
                }
            } ?: run { finishedWithTimeout = true }
        } catch (e: Exception) {
            thrownException = e
        }

        steps[currentValue]?.invoke().also { steps.remove(currentValue) }
    }


    fun stepAt(position: Int, step: suspend Step<T>.() -> Unit): Unit {
        steps[position] = Step(this, step)
    }

    fun stepAt(vararg positions: Int, step: suspend Step<T>.() -> Unit) {
        positions.forEach {
            steps[it] = Step(this, step)
        }
    }

    fun getFlowTestResult(): FlowTestResult<T> {
        return FlowTestResult(this)
    }
}

class Step<T>(private val flowScenario: FlowScenario<T>, private val block: suspend Step<T>.() -> Unit) {
    suspend operator fun invoke() = block()

    infix fun assertNextElement(block: () -> T) {
        val nextValue = flowScenario.nextValue ?: fail("No more values available")

        if (block()!! != nextValue)
            fail("") else Unit
    }

    fun dismissNextValue() = flowScenario.nextValue
    fun assertNoMoreValues(): Unit = Assertions.assertEquals(0, flowScenario.results.size)
    fun assertRemainingValuesCount(block: () -> Int): Unit = Assertions.assertEquals(block(), flowScenario.results.size)
}


suspend inline infix fun <T> Flow<T>.testScenarioWithTimeout(block: FlowScenario<T>.() -> Unit): FlowTestResult<T> {
    val flowTest = FlowScenario<T>(this)
    flowTest.block()
    flowTest.startScenario()

    return FlowTestResult(flowTest)
}
