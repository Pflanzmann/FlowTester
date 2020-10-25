package flowtester.scenario

import flowtester.scenario.FlowScenarioApi.Companion.DEFAULT_TIMEOUT
import flowtester.scenario.FlowScenarioApi.Companion.TAKE_WITHOUT_LIMIT
import kotlinx.coroutines.flow.Flow

suspend fun <T> Flow<T>.testScenario(
    timeOut: Long = DEFAULT_TIMEOUT,
    confirmSteps: Boolean = true,
    allowThrowable: Boolean = false,
    take: Int = TAKE_WITHOUT_LIMIT,
    setupScenario: suspend FlowScenarioApi<T>.() -> Unit
) {
    val flowTest = FlowScenario<T>(this).apply {
        this.timeOut = timeOut
        this.confirmUnconsumedSteps = confirmSteps
        this.allowUncaughtThrowable = allowThrowable
        this.take = take
    }

    flowTest.setupScenario()
    flowTest.startScenario()
}

suspend infix fun <T> Flow<T>.testScenario(block: suspend FlowScenarioApi<T>.() -> Unit) {
    testScenario(setupScenario = block)
}
