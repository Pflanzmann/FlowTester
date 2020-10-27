package flowTester.scenario

import flowTester.scenario.FlowScenarioApi.Companion.MAX_TIMEOUT
import flowTester.scenario.FlowScenarioApi.Companion.TAKE_WITHOUT_LIMIT
import flowTester.step.Step
import flowTester.step.StepApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeoutOrNull

interface FlowScenarioApi<T> {
    companion object {
        const val MAX_TIMEOUT = Long.MAX_VALUE
        const val TAKE_WITHOUT_LIMIT = Int.MAX_VALUE
    }

    var timeOut: Long
    var verifyAllSteps: Boolean
    var verifyAllValues: Boolean
    var allowUncaughtThrowable: Boolean
    var take: Int

    fun doAt(position: Int, step: suspend StepApi<T>.() -> Unit)
    fun doAt(vararg positions: Int, step: suspend StepApi<T>.() -> Unit)
    fun beforeAll(step: suspend StepApi<T>.() -> Unit)
    fun afterAll(step: suspend StepApi<T>.() -> Unit)
    fun then(step: suspend StepApi<T>.() -> Unit)
}

internal class FlowScenario<T>(private val flow: Flow<T>) : FlowScenarioApi<T> {
    class StepDoubleAssignmentException : Throwable()
    class ScenarioCheckException(message: String? = null, throwable: Throwable? = null) : Throwable(message, throwable)

    override var timeOut: Long = MAX_TIMEOUT
    override var take: Int = TAKE_WITHOUT_LIMIT
    override var verifyAllSteps: Boolean = false
    override var verifyAllValues: Boolean = false
    override var allowUncaughtThrowable: Boolean = false

    private val steps = mutableMapOf<Int, Step<T>?>()
    private var endStep: Step<T>? = null
    private var startStep: Step<T>? = null

    private val results = ArrayDeque<T>()

    val valueCount: Int
        get() = results.size

    val pollValue: T
        get() = results.removeFirst()

    val popValue: T
        get() = results.removeLast()

    var finishedWithTimeout = false
    var latestThrowable: Throwable? = null
    var numberOfConsumes = 0

    private var lastStepPosition = -1

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
                numberOfConsumes++

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


