package flowTester.step

internal interface StepApiInternal<T>: StepApi<T> {

    /**
     * A function to invoke the method block
     */
    suspend operator fun invoke()
}