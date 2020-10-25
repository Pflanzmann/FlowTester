package flowtester.step

import flowtester.scenario.FlowScenario

interface StartStepApi<T> {
    suspend operator fun invoke()
}

internal class StartStep<T>(
    private val flowScenario: FlowScenario<T>,
    private val block: suspend StartStepApi<T>.() -> Unit
) : StartStepApi<T> {

    override suspend operator fun invoke() = block()
}

