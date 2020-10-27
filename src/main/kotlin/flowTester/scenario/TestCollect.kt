package flowTester.scenario

import flowTester.step.StepApi
import kotlinx.coroutines.flow.Flow


suspend fun <T> Flow<T>.testCollect(
    timeOut: Long? = null,
    verifyAllSteps: Boolean? = null,
    take: Int? = null,
    setupScenario: suspend StepApi<T>.() -> Unit
) {
    val flowTest = FlowScenario<T>(this).apply {
        timeOut?.let { this.timeOut = timeOut }
        verifyAllSteps?.let { this.verifyAllSteps = verifyAllSteps }
        take?.let { this.take = take }
    }

    flowTest.startScenario()
    flowTest.afterAll(step = setupScenario)
}

suspend infix fun <T> Flow<T>.testCollect(block: suspend StepApi<T>.() -> Unit) {
    testCollect(setupScenario = block)
}
