package flowTester.step

import flowTester.scenario.FlowScenarioApi
import flowTester.scenario.FlowScenarioApiInternal
import kotlin.reflect.KClass

internal open class Step<T>(
    private val flowScenario: FlowScenarioApiInternal<T>,
    private val block: suspend StepApi<T>.() -> Unit
) : StepApiInternal<T> {

    override val pollValue: T
        get() = flowScenario.pollValue

    override val popValue: T
        get() = flowScenario.popValue

    override suspend operator fun invoke() = block()

    override fun allValuesConsumed(): Boolean = flowScenario.valueCount == 0

    override fun numberOfUnconsumedValues(): Int = flowScenario.valueCount

    override fun finishWithTimeout(): Boolean = flowScenario.finishedWithTimeout

    override fun dismissValue(number: Int) = repeat(number) {
        flowScenario.pollValue
    }

    override fun usedAllSteps(): Boolean {
        return (flowScenario.take != FlowScenarioApi.TAKE_WITHOUT_LIMIT && flowScenario.take != flowScenario.numberOfConsumedSteps)
    }

    override fun <E : Throwable> didThrow(type: KClass<E>): Boolean {
        val throwable = flowScenario.consumeThrowable()

        return !type.isInstance(throwable)
    }

    override fun <E : Throwable> didThrow(type: () -> KClass<E>): Boolean = didThrow(type())

    override fun didThrow(expectedThrowable: Throwable): Boolean {
        val throwable = flowScenario.consumeThrowable()

        return expectedThrowable == throwable
    }
}
