package flowtester.scenario

import flowtester.scenario.FlowScenarioApi.Companion.DEFAULT_TIMEOUT
import flowtester.scenario.FlowScenarioApi.Companion.MAX_TIMEOUT
import flowtester.scenario.FlowScenarioApi.Companion.TAKE_WITHOUT_LIMIT
import flowtester.step.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.fail
import org.opentest4j.AssertionFailedError

interface FlowScenarioApi<T> {
    companion object {
        const val DEFAULT_TIMEOUT = 5000L
        const val MAX_TIMEOUT = Long.MAX_VALUE
        const val TAKE_WITHOUT_LIMIT = Int.MAX_VALUE
    }

    var timeOut: Long
    var confirmUnconsumedSteps: Boolean
    var allowUncaughtThrowable: Boolean
    var take: Int
    var canThrowDefault: Boolean

    fun doAt(position: Int, canThrow: Boolean = canThrowDefault, step: suspend StepApi<T>.() -> Unit)
    fun doAt(vararg positions: Int, canThrow: Boolean = canThrowDefault, step: suspend StepApi<T>.() -> Unit)
    fun beforeAll(canThrow: Boolean = canThrowDefault, step: suspend StartStepApi<T>.() -> Unit)
    fun afterAll(canThrow: Boolean = canThrowDefault, step: suspend EndStepApi<T>.() -> Unit)
    fun then(canThrow: Boolean = canThrowDefault, step: suspend StepApi<T>.() -> Unit)
}

internal class FlowScenario<T>(private val flow: Flow<T>) : FlowScenarioApi<T> {
    class StepInvocationException(override val cause: Throwable? = null) : Throwable(cause)
    object FlowCancellationException : Exception()
    class StepDoubleAssignmentException : Throwable()

    override var timeOut: Long = DEFAULT_TIMEOUT
    override var take: Int = TAKE_WITHOUT_LIMIT
    override var confirmUnconsumedSteps: Boolean = true
    override var allowUncaughtThrowable: Boolean = false
    override var canThrowDefault: Boolean = false

    private val steps = mutableMapOf<Int, Step<T>?>()
    var endStep: EndStep<T>? = null
    var startStep: StartStep<T>? = null

    val results = ArrayDeque<T>()
    val nextValue: T?
        get() = results.removeFirst()

    var finishedWithTimeout = false
    var latestThrowable: Throwable? = null
    var numberOfConsumes = 0

    private var lastStepPosition = -1

    override fun beforeAll(canThrow: Boolean, step: suspend StartStepApi<T>.() -> Unit) {
        this.startStep = StartStep(this, step)
    }

    override fun afterAll(canThrow: Boolean, step: suspend EndStepApi<T>.() -> Unit) {
        this.endStep = EndStep(this, step)
    }

    override fun doAt(position: Int, canThrow: Boolean, step: suspend StepApi<T>.() -> Unit) {
        steps[position]?.let { throw StepDoubleAssignmentException() }
        steps[position] = Step(this, step, canThrow)
        lastStepPosition = position
    }

    override fun doAt(vararg positions: Int, canThrow: Boolean, step: suspend StepApi<T>.() -> Unit) {
        positions.forEach {
            doAt(it, canThrow, step)
        }
        positions.maxOrNull()?.let { lastStepPosition = it }
    }

    override fun then(canThrow: Boolean, step: suspend StepApi<T>.() -> Unit) {
        lastStepPosition++
        doAt(lastStepPosition, canThrow, step)
    }

    suspend fun startScenario() {
        startStep?.invoke()

        try {
            withTimeoutOrNull(timeOut) {
                flow.take(take).collectIndexed { index, value ->
                    numberOfConsumes++

                    results.add(value)
                    steps[index]?.let {
                        steps.remove(index)
                        try {
                            it.invoke()
                        } catch (e: AssertionError) {
                            throw e
                        } catch (e: Throwable) {
                            if (!it.canThrow)
                                throw StepInvocationException(e)
                        }
                    }
                }
            } ?: run { finishedWithTimeout = true }
        } catch (e: StepInvocationException) {
            throw e
        } catch (e: AssertionFailedError) {
            fail(e)
        } catch (e: Throwable) {
            latestThrowable = e
        }

        endStep?.invoke()

        standardChecks()
    }

    private fun standardChecks() {
        if (confirmUnconsumedSteps && steps.isNotEmpty())
            fail("Not all Steps got invoked \nRemaining positions: [${steps.map { it.key.toString() }.joinToString(separator = ",")}]\n")

        if (!allowUncaughtThrowable)
            latestThrowable?.let { fail("Uncaught Throwable: ", latestThrowable) }

        if (timeOut == MAX_TIMEOUT && finishedWithTimeout)
            fail("Finished with timout but shouldn't")
    }
}


