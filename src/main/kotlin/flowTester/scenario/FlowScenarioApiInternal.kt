package flowTester.scenario

internal interface FlowScenarioApiInternal<T> : FlowScenarioApi<T> {

    /**
     * Returns the number of collected values
     */
    val valueCount: Int

    /**
     * Polls the value that was emitted first and removes it from the list
     */
    val pollValue: T

    /**
     * Pops the value that was emitted last and removes it from the list
     */
    val popValue: T

    /**
     * Check Did the flow finish with a timeout
     */
    var finishedWithTimeout: Boolean

    /**
     * Returns the number of consumed Steps
     */
    var numberOfConsumedSteps: Int

    /**
     * Consumes t
     */
    fun consumeThrowable(): Throwable?
}