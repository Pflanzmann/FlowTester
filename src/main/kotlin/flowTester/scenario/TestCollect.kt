package flowTester.scenario

import flowTester.scenario.FlowScenarioApi.Companion.DEFAULT_TIMEOUT
import flowTester.step.StepApi
import kotlinx.coroutines.flow.Flow


suspend fun <T> Flow<T>.testCollect(
    timeOut: Long = DEFAULT_TIMEOUT,
    confirmSteps: Boolean = true,
    allowThrowable: Boolean = false,
    take: Int = FlowScenarioApi.TAKE_WITHOUT_LIMIT,
    setupScenario: suspend StepApi<T>.() -> Unit
) {
    val flowTest = FlowScenario<T>(this).apply {
        this.timeOut = timeOut
        this.forceConsumeAllSteps = confirmSteps
        this.allowUncaughtThrowable = allowThrowable
        this.take = take
    }

    flowTest.startScenario()
    flowTest.afterAll(step = setupScenario)
}

suspend infix fun <T> Flow<T>.testCollect(block: suspend StepApi<T>.() -> Unit) {
    testCollect(setupScenario = block)
}
