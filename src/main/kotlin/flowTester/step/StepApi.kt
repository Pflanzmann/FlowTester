package flowTester.step

import kotlin.reflect.KClass

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
     * Polls a number of values and dismisses them
     * @param number: The number of values that should get dismissed
     */
    fun dismissValue(number: Int = 1)

    /**
     * Returns whether all values got consumed or not
     */
    fun allValuesConsumed(): Boolean

    /**
     * Returns the number of unconsumed values
     */
    fun numberOfUnconsumedValues(): Int

    /**
     * Returns whether the flow ended with a timeout or not
     */
    fun finishWithTimeout(): Boolean

    /**
     * Returns whether the all steps got used or not
     */
    fun usedAllSteps(): Boolean

    /**
     * Verify if the flow did throw an exception of the given type
     */
    fun <E : Throwable> didThrow(type: KClass<E>): Boolean

    /**
     * Verify if the flow did throw an exception of the given type
     */
    fun <E : Throwable> didThrow(type: () -> KClass<E>): Boolean

    /**
     * Verify if the flow did throw an exception that equals the given exception
     */
    fun didThrow(expectedThrowable: Throwable): Boolean
}
