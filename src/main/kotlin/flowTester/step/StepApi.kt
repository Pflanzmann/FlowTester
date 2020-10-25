package flowTester.step

import flowTester.scenario.FlowScenario
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.fail
import kotlin.reflect.KClass


interface StepApi<T> {
    suspend operator fun invoke()

    infix fun nextValueEquals(expected: T)
    infix fun nextValueEquals(expected: () -> T)
    fun dismissValue(number: Int = 1)

    fun allValuesConsumed()
    fun remainingValuesCount(expected: Int)
    fun remainingValuesCount(expected: () -> Int)
    fun finishWithTimeout()
    fun <E : Throwable> didThrow(type: () -> KClass<E>)
    fun <E : Throwable> didThrow(type: KClass<E>)
}

internal class Step<T>(
    private val flowScenario: FlowScenario<T>,
    private val block: suspend Step<T>.() -> Unit,

    val canThrow: Boolean
) : StepApi<T> {

    override suspend operator fun invoke() = block()

    override fun allValuesConsumed() {
        if (flowScenario.results.isNotEmpty())
            fail("Not all values got consumed\n" + "Remaining:\n ${flowScenario.results.mapIndexed { index, it -> "$index: ${it.toString()},\n" }}")
    }

    override fun remainingValuesCount(expected: Int) {
        Assertions.assertEquals(expected, flowScenario.results.size)
    }

    override fun remainingValuesCount(expected: () -> Int) = remainingValuesCount(expected())

    override fun finishWithTimeout() {
        if (!flowScenario.finishedWithTimeout)
            fail("Did not finish with timeout as expected.")
    }

    override fun nextValueEquals(expected: T) {
        val nextValue = flowScenario.nextValue ?: fail("No more values available.")

        Assertions.assertEquals(expected, nextValue)
    }

    override infix fun nextValueEquals(expected: () -> T) = nextValueEquals(expected())

    override fun dismissValue(number: Int) = repeat(number) {
        flowScenario.nextValue
    }

    override fun <E : Throwable> didThrow(type: KClass<E>) {
        val throwable = flowScenario.latestThrowable ?: fail("Nothing was thrown")
        flowScenario.latestThrowable = null

        if (!type.isInstance(throwable))
            fail("Wrong Throwable type: $throwable")
    }

    override fun <E : Throwable> didThrow(type: () -> KClass<E>) = didThrow(type())
}
