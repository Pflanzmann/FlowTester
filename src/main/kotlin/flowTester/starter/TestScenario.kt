package flowTester.starter

import flowTester.scenario.FlowScenario
import flowTester.scenario.FlowScenarioApi
import kotlinx.coroutines.flow.Flow

/**
 * Collects on a flow and invokes a method after every collected value to test the results.
 * To not having an endless collection process you should set a timeout or a limit of taken objects.
 *
 * Starts a [FlowScenario] with multiple steps that gets invoked after each collected value.
 *
 * @param timeOut The timeout for the collect-process of the flow
 * @param take The maximal number of taken emitted values by the Scenario
 * @param verifyAllSteps Make sure whether all step should get invoked or not
 * @param setupScenario The method to build the [FlowScenario] with multiple steps
 */
suspend fun <T> Flow<T>.testScenario(
    timeOut: Long? = null,
    verifyAllSteps: Boolean? = null,
    take: Int? = null,
    setupScenario: suspend FlowScenarioApi<T>.() -> Unit
) {
    val flowTest = FlowScenario<T>(this).apply {
        timeOut?.let { this.timeOut = timeOut }
        verifyAllSteps?.let { this.verifyAllSteps = verifyAllSteps }
        take?.let { this.take = take }
    }

    flowTest.setupScenario()
    flowTest.startScenario()
}

/**
 * Helper to start the [testScenario] method as infix fun and default params defined in the [FlowScenario]
 */
suspend infix fun <T> Flow<T>.testScenario(block: suspend FlowScenarioApi<T>.() -> Unit) {
    testScenario(setupScenario = block)
}
