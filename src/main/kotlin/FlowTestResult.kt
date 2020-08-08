package tice.helper

class FlowTestResult<T>(flowScenario: FlowScenario<T>) {

    val finishedWithTimeout = flowScenario.finishedWithTimeout
    val thrownException: Exception? = flowScenario.thrownException
    val unusedResultCount = flowScenario.results.size
    val unusedStepsCount = flowScenario.steps.size
    val collectedValues = flowScenario.currentValue

}