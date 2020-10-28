package flowTester.scenario

import flowTester.step.StepApi
import kotlin.reflect.KClass

interface FlowScenarioApi<T> {
    /**
     * The maximal number of taken emitted values by the Scenario.
     *
     * If [take] is not set, then it will be set to the position of the highest inserted step.
     * If the [timeOut] got set while take was not, take will be [Int.MAX_VALUE].
     */
    var take: Int

    /**
     * The timeout for the collect-process of the flow
     *
     * If [timeOut] is not set, then it will be set to [Long.MAX_VALUE]
     */
    var timeOut: Long

    /**
     * Make sure whether all step should get invoked or not
     */
    var verifyAllSteps: Boolean

    /**
     * Make sure whether all values should get consumed or not
     */
    var verifyAllValues: Boolean

    /**
     * Passes a function block for a [StepApi] at the given [position][step].
     * This function block will get a value as parameter which is associated with the step.
     *
     * @throws [StepDoubleAssignmentException] Throws if a position gets double assigned.
     */
    fun doAt(position: Int, step: suspend StepApi<T>.(value: T) -> Unit)

    /**
     * Inserts a [StepApi] object at every given position with [step] as parameter for all.
     *
     * @throws [StepDoubleAssignmentException] Throws if a position gets double assigned.
     */
    fun doAt(vararg positions: Int, step: suspend StepApi<T>.(value: T) -> Unit)

    /**
     * Inserts a function block for a [StepApi] at the last inserted position +1.
     * This function block will get a value as parameter.
     *
     * When [then] gets called as the first inserted [StepApi], then it gets inserted as position 0.
     */
    fun then(step: suspend StepApi<T>.(value: T) -> Unit)

    /**
     * Passes a function block for a [StepApi] that gets executed before the collect on the flow starts.
     * This function block will not get a value as parameter.
     */
    fun beforeAll(step: suspend StepApi<T>.() -> Unit)

    /**
     * Passes a suspend function block for a [StepApi] that gets executed after the collect on the flow ends.
     * This function block will not get a value as parameter.
     */
    fun afterAll(step: suspend StepApi<T>.() -> Unit)

    /**
     * Passes a suspend function block for a [StepApi] that gets executed when the flow catches and has the throwable as parameter.
     * @param catchBlock is virtually a synonym for the [afterAll] step. while it gets executed before [afterAll], it will still
     * get executed after the flow collected
     *
     * @param type Is the KClass type of the expected [Throwable]
     */
    fun <E : Throwable> catch(type: KClass<E>, catchBlock: (suspend StepApi<T>.(throwable: Throwable) -> Unit)? = null)
}
