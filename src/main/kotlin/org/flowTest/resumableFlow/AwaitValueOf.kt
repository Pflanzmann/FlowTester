package org.flowTest.resumableFlow

import kotlinx.coroutines.Job
import kotlinx.coroutines.withTimeout
import org.flowTest.exceptions.AwaitableFlowCanceledException
import org.junit.jupiter.api.fail


suspend fun <T> awaitValueOf(flow: AwaitableFlow<T>): AwaitedResultApi<T> {
    if (flow.state == AwaitableFlowApi.State.CANCELED)
        return AwaitedResult(value = null, throwable = AwaitableFlowCanceledException())

    withTimeout(flow.timeout) {
        flow.valueJob.join()
    }

    flow.valueJob = Job()

    flow.thrownException?.let {
        return AwaitedResult(value = null, throwable = it)
    }

    val value = flow.nextValue ?: fail("No next value available.")
    flow.nextValue = null
    flow.waitingJob.complete()

    return AwaitedResult(value)
}

suspend fun <T> awaitValueOf(block: () -> AwaitableFlow<T>): AwaitedResultApi<T> {
    return awaitValueOf(block())
}
