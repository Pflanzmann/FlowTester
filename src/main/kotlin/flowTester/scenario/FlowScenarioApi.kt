package flowTester.scenario

import flowTester.step.StepApi

interface FlowScenarioApi<T> {
    companion object {
        const val MAX_TIMEOUT = Long.MAX_VALUE
        const val TAKE_WITHOUT_LIMIT = Int.MAX_VALUE
    }

    var timeOut: Long
    var verifyAllSteps: Boolean
    var verifyAllValues: Boolean
    var allowUncaughtThrowable: Boolean
    var take: Int

    fun doAt(position: Int, step: suspend StepApi<T>.() -> Unit)
    fun doAt(vararg positions: Int, step: suspend StepApi<T>.() -> Unit)
    fun beforeAll(step: suspend StepApi<T>.() -> Unit)
    fun afterAll(step: suspend StepApi<T>.() -> Unit)
    fun then(step: suspend StepApi<T>.() -> Unit)
}