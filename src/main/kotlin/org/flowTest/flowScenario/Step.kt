package org.flowTest.flowScenario

import flowTester.org.example.flowTest.FlowScenario
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.fail
import kotlin.reflect.KClass


interface StepApi<T> {

    suspend operator fun invoke()
    infix fun assertNextElement(value: T)
    infix fun assertNextElement(block: () -> T)

    fun dismissNextValue(): T?
    fun assertAllValuesConsumed()
    fun assertRemainingValuesCount(block: () -> Int)
    fun assertFinishWithTimeout()
    fun <E : Throwable> assertDidThrow(type: () -> KClass<E>)
}

internal class Step<T>(
    private val flowScenario: FlowScenario<T>,
    private val block: suspend Step<T>.() -> Unit
) : StepApi<T> {
    override suspend operator fun invoke() = block()

    override infix fun assertNextElement(value: T): Unit {
        val nextValue = flowScenario.nextValue ?: fail("No more values available.")

        Assertions.assertEquals(value, nextValue)
    }

    override infix fun assertNextElement(block: () -> T) {
        assertNextElement(block())
    }

    override fun dismissNextValue() = flowScenario.nextValue

    override fun assertAllValuesConsumed() {
        if (flowScenario.results.isNotEmpty())
            fail(
                "Not all values got consumed\n" +
                        "Remaining:\n ${flowScenario.results.mapIndexed { index, it -> "$index: ${it.toString()},\n" }}"
            )
    }

    override fun assertRemainingValuesCount(block: () -> Int): Unit =
        Assertions.assertEquals(block(), flowScenario.results.size)

    override fun assertFinishWithTimeout() =
        Assertions.assertTrue(flowScenario.finishedWithTimeout)

    override fun <E : Throwable> assertDidThrow(type: () -> KClass<E>) {
        flowScenario.thrownException ?: fail("Nothing was thrown")

        if (!type().isInstance(flowScenario.thrownException))
            fail("Wrong Throwable type: ${flowScenario.thrownException}")

        flowScenario.thrownException = null
    }
}
