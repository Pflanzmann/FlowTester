package flowTester.step

import flowTester.scenario.FlowScenario
import flowTester.scenario.FlowScenarioApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.fail
import kotlin.reflect.KClass


interface StepApi<T> {
    val pollValue: T
    val stepValue: T

    suspend operator fun invoke()

    fun dismissValue(number: Int = 1)

    fun allValuesConsumed(): Boolean
    fun numberOfUnconsumedValues(): Int
    fun finishWithTimeout(): Boolean
    fun consumedAllSteps(): Boolean
    fun <E : Throwable> didThrow(type: KClass<E>): Boolean
    fun <E : Throwable> didThrow(type: () -> KClass<E>): Boolean
    fun didThrow(expectedThrowable: Throwable): Boolean
}

internal open class Step<T>(
    private val flowScenario: FlowScenario<T>,
    private val block: suspend Step<T>.() -> Unit,

    val shouldThrow: Boolean
) : StepApi<T> {
    override val pollValue: T
        get() = flowScenario.pollValue

    override val stepValue: T
        get() = flowScenario.latestValue

    override suspend operator fun invoke() = block()

    override fun allValuesConsumed(): Boolean = flowScenario.results.isNotEmpty()

    override fun numberOfUnconsumedValues(): Int = flowScenario.results.size

    override fun finishWithTimeout(): Boolean = flowScenario.finishedWithTimeout

    override fun dismissValue(number: Int) = repeat(number) {
        flowScenario.pollValue
    }

    override fun consumedAllSteps(): Boolean {
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
