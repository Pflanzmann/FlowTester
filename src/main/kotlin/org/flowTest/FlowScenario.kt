package flowTester.org.example.flowTest

import flowTester.org.example.flowTest.FlowScenarioApi.Companion.DEFAULT_TIMEOUT
import flowTester.org.example.flowTest.FlowScenarioApi.Companion.MAX_TIMEOUT
import flowTester.org.example.flowTest.FlowScenarioApi.Companion.NO_LIMIT
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.withTimeoutOrNull
import org.flowTest.Step
import org.flowTest.StepApi
import org.junit.jupiter.api.fail
import org.opentest4j.AssertionFailedError
import java.util.*

class StepDoubleAssignmentException : Throwable()

interface FlowScenarioApi<T> {
    companion object {
        const val DEFAULT_TIMEOUT = 5000L
        const val MAX_TIMEOUT = Long.MAX_VALUE
        const val NO_LIMIT = -1
    }

    var timeOut: Long
    var confirmSteps: Boolean
    var allowThrowable: Boolean

    fun doAt(position: Int, step: suspend StepApi<T>.() -> Unit)
    fun doAt(vararg positions: Int, step: suspend StepApi<T>.() -> Unit)
    fun after(step: suspend StepApi<T>.() -> Unit)
    fun before(step: suspend StepApi<T>.() -> Unit)
    fun then(step: suspend StepApi<T>.() -> Unit)
    var take: Int
}

internal class FlowScenario<T>(private val flow: Flow<T>) : FlowScenarioApi<T> {
    private class FlowCancellationException : Throwable()

    override var timeOut: Long = DEFAULT_TIMEOUT
    override var confirmSteps: Boolean = true
    override var allowThrowable: Boolean = true
    override var take: Int = NO_LIMIT
        set(value) {
            field = value - 1
        }

    val steps = mutableMapOf<Int, StepApi<T>?>()
    var endStep: Step<T>? = null
    var startStep: Step<T>? = null

    val results = LinkedList<T>()

    var currentValue = 0

    val nextValue: T?
        get() = results.poll()

    var finishedWithTimeout = false
    var thrownException: Throwable? = null

    var lastStepPosition = 0

    override fun before(step: suspend StepApi<T>.() -> Unit) {
        this.startStep = Step(this, step)
    }

    override fun after(step: suspend StepApi<T>.() -> Unit) {
        this.endStep = Step(this, step)
    }

    override fun doAt(position: Int, step: suspend StepApi<T>.() -> Unit) {
        steps[position]?.let { throw StepDoubleAssignmentException() }
        steps[position] = Step(this, step)
        lastStepPosition = position
    }

    override fun doAt(vararg positions: Int, step: suspend StepApi<T>.() -> Unit) {
        positions.forEach {
            doAt(it, step)
        }
        positions.max()?.let { lastStepPosition = it }
    }

    override fun then(step: suspend StepApi<T>.() -> Unit) {
        lastStepPosition++
        doAt(lastStepPosition, step)
    }

    suspend fun startScenario() {
        try {
            withTimeoutOrNull(timeOut) {
                flow.collectIndexed { index, value ->

                    results.add(value)
                    steps[index]?.let {
                        steps.remove(index)
                        it.invoke()
                    }

                    if (index >= take)
                        throw FlowCancellationException()
                }
            } ?: run { finishedWithTimeout = true }
        } catch (e: TimeoutCancellationException) {
            steps[currentValue]?.invoke()
        } catch (e: AssertionFailedError) {
            throw e
        } catch (e: FlowCancellationException) {

        } catch (e: Throwable) {
            thrownException?.let { throw e }
            thrownException = e
        }

        standardChecks()
    }

    private fun standardChecks() {
        if (steps.isNotEmpty() && confirmSteps)
            fail("Not all Steps got invoked \nRemaining positions: [${steps.map { it.key.toString() }.joinToString(separator = ",")}]\n")

        if (allowThrowable)
            thrownException?.let { fail("Uncaught Throwable: ${it.javaClass}", it) }

        if (timeOut == MAX_TIMEOUT && finishedWithTimeout)
            fail("Finished with timout but shouldn't")
    }
}


