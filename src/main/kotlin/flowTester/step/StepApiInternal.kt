package flowTester.step

internal interface StepApiInternal<T>: StepApi<T> {

    /**
     * A function to invoke the step
     */
    suspend operator fun invoke()
}