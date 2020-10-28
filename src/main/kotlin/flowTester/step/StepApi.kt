package flowTester.step

/**
 * A Step defines a method and the scope that is invoked when the flow emits a new value
 */
interface StepApi<T> {

    /**
     * Polls the value that was emitted first and removes it from the list
     */
    val pollValue: T

    /**
     * Pops the value that was emitted last and removes it from the list
     */
    val popValue: T

    /**
     * Returns index of the current step
     */
    val currentPosition: Int

    /**
     * Polls a number of values and dismisses them
     * @param number: The number of values that should get dismissed
     */
    fun dismissValue(number: Int = 1)

    /**
     * Returns whether all values got consumed or not
     */
    fun consumedAllValues(): Boolean

    /**
     * Returns the number of unconsumed values
     */
    fun numberOfUnconsumedValues(): Int

    /**
     * Returns whether the flow ended with a timeout or not
     */
    fun finishWithTimeout(): Boolean
}
