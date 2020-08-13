package org.flowTest.resumableFlow

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.jupiter.api.fail

interface ResumableFlowApi<T> {
    enum class State { RUNNING, DONE, COMPLETED, CANCELED }

    var state: State
}

internal interface ResumableFlowAccess<T> {
    var valueJob: CompletableJob
    var waitingJob: CompletableJob

    var nextValue: T?
    var thrownException: Throwable?
}

internal class ResumableFlow<T>(
    private val flow: Flow<T>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ResumableFlowApi<T>, ResumableFlowAccess<T> {

    override var valueJob = Job()
    override var waitingJob = Job()

    override var nextValue: T? = null
    override var thrownException: Throwable? = null

    override var state = ResumableFlowApi.State.RUNNING

    init {
        startCollecting()
    }

    private fun startCollecting() {
        CoroutineScope(dispatcher).launch {

            try {
                flow.collect {
                    nextValue = it

                    waitingJob = Job()

                    valueJob.complete()
                    waitingJob.join()
                }
            } catch (e: Throwable) {
                thrownException = e
                state = ResumableFlowApi.State.CANCELED
                return@launch
            }

            state = ResumableFlowApi.State.DONE
        }
    }

}

fun <T> resumableFlowOf(block: () -> Flow<T>): ResumableFlowApi<T> {
    return ResumableFlow(block())
}

suspend fun <T> nextValueOf(block: () -> ResumableFlowApi<T>): ResumableResultApi<T> {
    val resumableFlow = (block() as? ResumableFlowAccess<T>) ?: fail("intern error")

    withTimeout(5000L) {
        resumableFlow.valueJob.join()
    }
    resumableFlow.valueJob = Job()

    val value = resumableFlow.nextValue ?: fail("No next value available.")

    resumableFlow.waitingJob.complete()

    return ResumableResult(value, resumableFlow.thrownException)
}

