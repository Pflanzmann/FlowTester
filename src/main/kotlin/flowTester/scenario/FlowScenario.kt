package flowTester.scenario

import flowTester.exception.DoubleAssignmentException
import flowTester.exception.ScenarioCheckException
import flowTester.step.Step
import flowTester.step.StepApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.reflect.KClass

internal class FlowScenario<T>(private val flow: Flow<T>) : FlowScenarioApiInternal<T> {

    private companion object {
        const val MAX_TIMEOUT = Long.MAX_VALUE
        const val TAKE_WITHOUT_LIMIT = Int.MAX_VALUE
        const val TAKE_FOR_EACH_STEP = -1
    }

    private val steps = mutableMapOf<Int, Step.ValueStep<T>?>()
    private var endStep: Step.NoArgStep<T>? = null
    private var startStep: Step.NoArgStep<T>? = null
    private var catch: Step.CatchStep<T>? = null

    private val results = ArrayDeque<T>()
    private var lastStepPosition = -1

    override var take: Int = TAKE_FOR_EACH_STEP
    override var timeOut: Long = MAX_TIMEOUT
    override var verifyAllSteps: Boolean = true
    override var verifyAllValues: Boolean = false

    override val valueCount: Int
        get() = results.size

    override val pollValue: T
        get() = results.removeFirst()

    override val popValue: T
        get() = results.removeLast()

    override var finishedWithTimeout = false
    override var indexCurrentStep = 0

    override fun beforeAll(step: suspend StepApi<T>.() -> Unit) {
        this.startStep = Step.NoArgStep(this, step)
    }

    override fun afterAll(step: suspend StepApi<T>.() -> Unit) {
        this.endStep = Step.NoArgStep(this, step)
    }

    override fun doAt(position: Int, step: suspend StepApi<T>.(value: T) -> Unit) {
        steps[position]?.let { throw DoubleAssignmentException() }
        steps[position] = Step.ValueStep(this, step)
        lastStepPosition = position
    }

    override fun doAt(vararg positions: Int, step: suspend StepApi<T>.(value: T) -> Unit) {
        positions.forEach { position ->
            doAt(position, step)
        }
        positions.maxOrNull()?.let { lastStepPosition = it }
    }

    override fun then(step: suspend StepApi<T>.(value: T) -> Unit) {
        ++lastStepPosition
        doAt(lastStepPosition, step)
    }

    override fun <E : Throwable> catch(type: KClass<E>, catchBlock: (suspend StepApi<T>.(throwable: Throwable) -> Unit)?) {
        catch?.let { throw DoubleAssignmentException() }

        val newCatch: suspend StepApi<T>.(throwable: Throwable) -> Unit = { throwable: Throwable ->
            if (!type.isInstance(throwable))
                throw throw ScenarioCheckException("Not the expected Exception", throwable)

            catchBlock?.invoke(this, throwable)
        }

        catch = Step.CatchStep(this, newCatch)
    }

    suspend fun startScenario() {
        startStep?.invoke()

        take = setupTakeNumber()

        withTimeoutOrNull(timeOut) {
            flow.catch {
                catch?.invoke(it) ?: throw ScenarioCheckException("unexpected Throwable")
            }.take(take).collectIndexed { index, value ->
                indexCurrentStep++

                results.add(value)
                steps[index]?.let {
                    steps.remove(index)
                    it.invoke(value)

                }
            }
        } ?: run { finishedWithTimeout = true }

        endStep?.invoke()

        standardChecks()
    }

    private fun setupTakeNumber(): Int {
        return when {
            timeOut != MAX_TIMEOUT && take == TAKE_FOR_EACH_STEP -> TAKE_WITHOUT_LIMIT
            take == TAKE_FOR_EACH_STEP -> (steps.keys.maxOrNull() ?: 0) + 1
            else -> take
        }
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

        catch?.takeIf { !it.invoked }?.run {
            throw ScenarioCheckException("Did not throw")
        }

        if (timeOut == MAX_TIMEOUT && finishedWithTimeout)
            throw ScenarioCheckException("Finished with timout but shouldn't")
    }
}


