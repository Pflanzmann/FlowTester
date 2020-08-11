package org.flowTest

import flowTester.org.example.flowTest.FlowScenario
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.fail


interface StepApi<T> {

    suspend operator fun invoke()
    infix fun assertNextElement(block: () -> T)
    fun dismissNextValue(): T?
    fun assertAllValuesConsumed()
    fun assertRemainingValuesCount(block: () -> Int)
    fun assertFinishWithTimeout()
    fun <E : Throwable> assertDidThrow(type: () -> Class<E>)
}

internal class Step<T>(
    private val flowScenario: FlowScenario<T>,
    private val block: suspend Step<T>.() -> Unit
) : StepApi<T> {
    override suspend operator fun invoke() = block()

    override infix fun assertNextElement(block: () -> T) {
        val nextValue = flowScenario.nextValue ?: fail("No more values available")
        val blockResult = block()

        Assertions.assertEquals(blockResult, nextValue)
    }

    override fun assertAllValuesConsumed(): Unit =
        Assertions.assertEquals(0, flowScenario.results.size)

    override fun dismissNextValue() = flowScenario.nextValue
    override fun assertRemainingValuesCount(block: () -> Int): Unit =
        Assertions.assertEquals(block(), flowScenario.results.size)

    override fun assertFinishWithTimeout() =
        Assertions.assertTrue(flowScenario.finishedWithTimeout)

    override fun <E : Throwable> assertDidThrow(type: () -> Class<E>) {
        flowScenario.thrownException ?: fail("Nothing was thrown")

        if (!type().isInstance(flowScenario.thrownException))
            fail("Wrong Throwable type: ${flowScenario.thrownException}")

        flowScenario.thrownException = null
    }
}
