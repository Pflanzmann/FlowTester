package flowTester.step

import flowTester.scenario.FlowScenario
import flowTester.scenario.FlowScenarioApi
import kotlin.reflect.KClass

internal open class Step<T>(
    private val flowScenario: FlowScenario<T>,
    private val block: suspend Step<T>.() -> Unit
) : StepApi<T> {
    override val pollValue: T
        get() = flowScenario.pollValue

    override val popValue: T
        get() = flowScenario.popValue

    suspend operator fun invoke() = block()

    override fun allValuesConsumed(): Boolean = flowScenario.valueCount == 0

    override fun numberOfUnconsumedValues(): Int = flowScenario.valueCount

    override fun finishWithTimeout(): Boolean = flowScenario.finishedWithTimeout

    override fun dismissValue(number: Int) = repeat(number) {
        flowScenario.pollValue
    }

    override fun usedAllSteps(): Boolean {
        return (flowScenario.take != FlowScenarioApi.TAKE_WITHOUT_LIMIT && flowScenario.take != flowScenario.numberOfConsumes)
    }

    override fun <E : Throwable> didThrow(type: KClass<E>): Boolean {
        val throwable = flowScenario.latestThrowable
        flowScenario.latestThrowable = null

        return !type.isInstance(throwable)
    }

    override fun <E : Throwable> didThrow(type: () -> KClass<E>): Boolean = didThrow(type())

    override fun didThrow(expectedThrowable: Throwable): Boolean {
        val throwable = flowScenario.latestThrowable
        flowScenario.latestThrowable = null

        return throwable == flowScenario.latestThrowable
    }
}
