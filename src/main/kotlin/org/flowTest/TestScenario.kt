package org.flowTest

import flowTester.org.example.flowTest.FlowScenario
import flowTester.org.example.flowTest.FlowScenarioApi
import flowTester.org.example.flowTest.FlowScenarioApi.Companion.DEFAULT_TIMEOUT
import kotlinx.coroutines.flow.Flow
import org.junit.jupiter.api.fail

suspend infix fun <T> Flow<T>.testScenario(block: suspend FlowScenarioApi<T>.() -> Unit) {
    testScenario(setupScenario = block)
}

suspend fun <T> Flow<T>.testScenario(
        timeOut: Long = DEFAULT_TIMEOUT,
        confirmSteps: Boolean = true,
        allowThrowable: Boolean = false,
        setupScenario: suspend FlowScenarioApi<T>.() -> Unit
) {
    val flowTest = FlowScenario<T>(this).apply {
        this.timeOut = timeOut
        this.confirmSteps = confirmSteps
        this.allowThrowable = allowThrowable
    }

    flowTest.setupScenario()

    flowTest.startStep?.invoke()
    flowTest.startScenario()
    flowTest.endStep?.invoke()

    flowTest.thrownException?.let { fail("Uncaught Throwable: ${it.javaClass}", it) }
}