package flowTester.scenario

import flowTester.exception.ScenarioCheckException
import flowTester.exception.StepDoubleAssignmentException
import flowTester.scenario.FlowScenarioApi.Companion.MAX_TIMEOUT
import flowTester.scenario.FlowScenarioApi.Companion.TAKE_WITHOUT_LIMIT
import flowTester.step.Step
import flowTester.step.StepApi
import flowTester.step.StepApiInternal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withTimeoutOrNull

internal class FlowScenario<T>(private val flow: Flow<T>) : FlowScenarioApiInternal<T> {

    override var timeOut: Long = MAX_TIMEOUT
    override var take: Int = TAKE_WITHOUT_LIMIT
    override var verifyAllSteps: Boolean = false
    override var verifyAllValues: Boolean = false
    override var allowUncaughtThrowable: Boolean = false

    private val steps = mutableMapOf<Int, StepApiInternal<T>?>()
    private var endStep: StepApiInternal<T>? = null
    private var startStep: StepApiInternal<T>? = null

    private val results = ArrayDeque<T>()

    private var latestThrowable: Throwable? = null
    private var lastStepPosition = -1

    override val valueCount: Int
        get() = results.size

    override val pollValue: T
        get() = results.removeFirst()

    override val popValue: T
        get() = results.removeLast()

    override var finishedWithTimeout = false
    override var numberOfConsumedSteps = 0

    override fun consumeThrowable(): Throwable? {
        val throwable = latestThrowable
        latestThrowable = null
        return throwable
    }

    override fun beforeAll(step: suspend StepApi<T>.() -> Unit) {
        this.startStep = Step(this, step)
    }

    override fun afterAll(step: suspend StepApi<T>.() -> Unit) {
        this.endStep = Step(this, step)
    }

    override fun doAt(position: Int, step: suspend StepApi<T>.() -> Unit) {
        steps[position]?.let { throw StepDoubleAssignmentException() }
        steps[position] = Step(this, step)
        lastStepPosition = position
    }

    override fun doAt(vararg positions: Int, step: suspend StepApi<T>.() -> Unit) {
        positions.forEach { position ->
            doAt(position, step)
        }
        positions.maxOrNull()?.let { lastStepPosition = it }
    }

    override fun then(step: suspend StepApi<T>.() -> Unit) {
        lastStepPosition++
        doAt(lastStepPosition, step)
    }

    suspend fun startScenario() {
        startStep?.invoke()

        withTimeoutOrNull(timeOut) {
            flow.catch { throwable ->
                latestThrowable = throwable
            }.take(take).collectIndexed { index, value ->
                numberOfConsumedSteps++

                results.add(value)
                steps[index]?.let {
                    steps.remove(index)
                    it.invoke()
                }
            }
        } ?: run { finishedWithTimeout = true }

        endStep?.invoke()

        standardChecks()
    }

    private fun standardChecks() {
        if (verifyAllSteps && steps.isNotEmpty()) {
            val valueString = steps.map { it.key.toString() }.joinToString(separator = ",")
            throw ScenarioCheckException("Not all steps got invoked \nRemaining positions: [$valueString]\n")
        }

        if (verifyAllValues && results.isNotEmpty()) {
            val valueString = results.map { it.toString() + "\n" }
            throw ScenarioCheckException("Not all values got consumed \nRemaining values: $valueString\n")
        }

        if (!allowUncaughtThrowable)
            latestThrowable?.let { throw ScenarioCheckException("Uncaught Throwable: ", latestThrowable) }

        if (timeOut == MAX_TIMEOUT && finishedWithTimeout)
            throw ScenarioCheckException("Finished with timout but shouldn't")
    }
}


