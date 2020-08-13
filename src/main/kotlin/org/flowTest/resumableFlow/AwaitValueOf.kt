package org.flowTest.resumableFlow

import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.fail


@Throws(TimeoutCancellationException::class)
suspend fun <T> awaitValueOf(block: () -> AwaitableFlowApi<T>): AwaitedResultApi<T> {
    val resumableFlow = (block() as? AwaitableFlowAccess<T>) ?: fail("internal error")

    withTimeout(resumableFlow.timeout) {
        resumableFlow.valueJob.join()
    }

    resumableFlow.valueJob = Job()

    val value = resumableFlow.nextValue ?: fail("No next value available.")

    resumableFlow.waitingJob.complete()

    return AwaitedResult(value)
}

