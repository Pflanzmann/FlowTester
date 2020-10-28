package flowTester.step

import flowTester.scenario.FlowScenarioApiInternal

internal interface StepApiInternal<T> : StepApi<T> {

    val invoked: Boolean

//    /**
//     * A function to invoke the step with the latest emitted value step
//     */
//    suspend operator fun invoke(value: T)
//
//    suspend operator fun invoke()
//
//    suspend operator fun invoke(throwable: Throwable)
}