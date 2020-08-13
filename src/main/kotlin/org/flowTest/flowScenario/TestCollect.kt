package org.flowTest.flowScenario

import flowTester.org.example.flowTest.FlowScenario
import flowTester.org.example.flowTest.FlowScenarioApi
import flowTester.org.example.flowTest.FlowScenarioApi.Companion.DEFAULT_TIMEOUT
import kotlinx.coroutines.flow.Flow


suspend infix fun <T> Flow<T>.testCollect(block: suspend StepApi<T>.() -> Unit) {
    testCollect(setupScenario = block)
}

suspend fun <T> Flow<T>.testCollect(
        timeOut: Long = DEFAULT_TIMEOUT,
        confirmSteps: Boolean = true,
        allowThrowable: Boolean = false,
        take: Int = FlowScenarioApi.NO_LIMIT,
        setupScenario: suspend StepApi<T>.() -> Unit
) {
    val flowTest = FlowScenario<T>(this).apply {
        this.timeOut = timeOut
        this.confirmSteps = confirmSteps
        this.allowThrowable = allowThrowable
        this.take = take
    }

    flowTest.startScenario()
    Step(flowTest, setupScenario).invoke()
}