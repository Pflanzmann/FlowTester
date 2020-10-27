package flowTester.scenario

import kotlinx.coroutines.flow.Flow

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

suspend infix fun <T> Flow<T>.testScenario(block: suspend FlowScenarioApi<T>.() -> Unit) {
    testScenario(setupScenario = block)
}
