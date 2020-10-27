package flowTester.scenario

import flowTester.step.StepApi

interface FlowScenarioApi<T> {
    companion object {
        const val MAX_TIMEOUT = Long.MAX_VALUE
        const val TAKE_WITHOUT_LIMIT = Int.MAX_VALUE
    }

    /**
     * The timeout for the collect-process of the flow
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
     * Make sure wheather a [Throwable] of the flow should need get verified or not
     */
    var allowUncaughtThrowable: Boolean

    /**
     * The maximal number of taken emitted values by the Scenario
     */
    var take: Int

    /**
     * Inserts a [StepApi] object with [step] as parameter at the given position
     *
     * @throws [StepDoubleAssignmentException] Throws if a position gets double assigned
     */
    fun doAt(position: Int, step: suspend StepApi<T>.() -> Unit)

    /**
     * Inserts a [StepApi] object at every given position with [step] as parameter for all
     *
     * @throws [StepDoubleAssignmentException] Throws if a position gets double assigned
     */
    fun doAt(vararg positions: Int, step: suspend StepApi<T>.() -> Unit)

    /**
     * Inserts a [StepApi] at the position +1 of the last inserted position
     *
     * When [then] gets called as the first inserted [StepApi], then it gets inserted as position 0
     */
    fun then(step: suspend StepApi<T>.() -> Unit)

    /**
     * Inserts a [StepApi] that gets executed before the collect on the flow starts
     */
    fun beforeAll(step: suspend StepApi<T>.() -> Unit)

    /**
     * Inserts a [StepApi] that gets executed after the collect on the flow ended
     */
    fun afterAll(step: suspend StepApi<T>.() -> Unit)
}