package flowTester.starter

import flowTester.scenario.FlowScenario
import flowTester.step.StepApi
import kotlinx.coroutines.flow.Flow

/**
 * Collects on a flow and invokes a method to test the results after the collection is done.
 * To not having an endless collection process you should set a timeout or a limit of taken objects.
 *
 * Starts a [FlowScenario] with one Step that gets invoked after the collection.
 *
 * @param timeOut The timeout for the collect-process of the flow
 * @param take The maximal number of taken emitted values by the Scenario
 * @param verifyAllSteps Make sure whether all [Steps] should get invoked or not
 * @param afterAll The method that gets executed after the collect
 */
suspend fun <T> Flow<T>.testCollect(
    timeOut: Long? = null,
    verifyAllSteps: Boolean? = null,
    take: Int? = null,
    afterAll: suspend StepApi<T>.() -> Unit
) {
    val flowTest = FlowScenario<T>(this).apply {
        timeOut?.let { this.timeOut = timeOut }
        verifyAllSteps?.let { this.verifyAllSteps = verifyAllSteps }
        take?.let { this.take = take }
    }

    flowTest.startScenario()
    flowTest.afterAll(step = afterAll)
}

/**
 * Helper to start the [testCollect] method as infix fun and default params defined in the [FlowScenario]
 */
suspend infix fun <T> Flow<T>.testCollect(block: suspend StepApi<T>.() -> Unit) {
    testCollect(afterAll = block)
}
