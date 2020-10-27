package flowTester.step

import flowTester.scenario.FlowScenario
import flowTester.scenario.FlowScenarioApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.fail
import kotlin.reflect.KClass

interface EndStepApi<T> {
    suspend operator fun invoke()

    fun allValuesConsumed()
    fun remainingValuesCount(expected: Int)
    fun remainingValuesCount(expected: () -> Int)
    fun finishWithTimeout()
    fun consumedEnough()
    fun <E : Throwable> didThrow(type: () -> KClass<E>)
    fun <E : Throwable> didThrow(type: KClass<E>)
}

internal class EndStep<T>(
    private val flowScenario: FlowScenario<T>,
    private val block: suspend EndStep<T>.() -> Unit
) : EndStepApi<T> {

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

    override fun consumedEnough() {
        if (flowScenario.take != FlowScenarioApi.TAKE_WITHOUT_LIMIT)
            Assertions.assertEquals(flowScenario.take, flowScenario.numberOfConsumes)
        else {
            fail("There was no limit to take.")
        }
    }

    override fun <E : Throwable> didThrow(type: KClass<E>) {
        val throwable = flowScenario.latestThrowable ?: fail("Nothing was thrown")
        flowScenario.latestThrowable = null

        if (!type.isInstance(throwable))
            fail("Wrong Throwable type: $throwable")
    }

    override fun <E : Throwable> didThrow(type: () -> KClass<E>) = didThrow(type())
}
