package flowTester.step

import flowTester.scenario.FlowScenarioApiInternal

internal sealed class Step<T>(private val flowScenario: FlowScenarioApiInternal<T>) : StepApiInternal<T> {
//    abstract val flowScenario: FlowScenarioApiInternal<T>

    override var invoked: Boolean = false

    override val pollValue: T
        get() = this.flowScenario.pollValue

    override val popValue: T
        get() = this.flowScenario.popValue

    override val currentPosition: Int
        get() = this.flowScenario.indexCurrentStep

    override fun consumedAllValues(): Boolean = this.flowScenario.valueCount == 0

    override fun numberOfUnconsumedValues(): Int = this.flowScenario.valueCount

    override fun finishWithTimeout(): Boolean = this.flowScenario.finishedWithTimeout

    override fun dismissValue(number: Int) = repeat(number) {
        this.flowScenario.pollValue
    }

    internal class NoArgStep<T>(fS: FlowScenarioApiInternal<T>, private val block: suspend StepApi<T>.() -> Unit) : Step<T>(fS) {

        suspend operator fun invoke() {
            block()
            invoked = true
        }
    }

    class ValueStep<T>(fS: FlowScenarioApiInternal<T>, private val block: suspend StepApi<T>.(T) -> Unit) : Step<T>(fS) {

        suspend operator fun invoke(value: T) {
            block(value)
            invoked = true
        }
    }

    class CatchStep<T>(fS: FlowScenarioApiInternal<T>, private val block: (suspend StepApi<T>.(Throwable) -> Unit)? = null) : Step<T>(fS) {

        suspend operator fun invoke(throwable: Throwable) {
            block?.invoke(this, throwable)
            invoked = true
        }
    }
}