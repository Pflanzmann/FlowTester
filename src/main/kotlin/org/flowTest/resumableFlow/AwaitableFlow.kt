package org.flowTest.resumableFlow

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

interface AwaitableFlowApi<T> {
    enum class State { RUNNING, DONE, COMPLETED, CANCELED }

    val state: State
    var timeout: Long
    var dispatcher: CoroutineDispatcher

    fun cancelFlow()
}

interface AwaitableFlowAccess<T> : AwaitableFlowApi<T> {
    var valueJob: CompletableJob
    var waitingJob: CompletableJob

    var nextValue: T?
    var thrownException: Throwable?
}

internal class AwaitableFlow<T>(
    private val flow: Flow<T>
) : AwaitableFlowApi<T>, AwaitableFlowAccess<T> {

    override var valueJob = Job()
    override var waitingJob = Job()

    override var nextValue: T? = null
    override var thrownException: Throwable? = null

    override var state = AwaitableFlowApi.State.RUNNING
    override var timeout: Long = 5000L
    override var dispatcher: CoroutineDispatcher = Dispatchers.Default

    private val collectJob = Job()

    init {
        startCollecting()
    }

    private fun startCollecting() {
        CoroutineScope(dispatcher + collectJob).launch {
            try {
                flow.collect {
                    nextValue = it

                    waitingJob = Job()

                    valueJob.complete()
                    waitingJob.join()
                }
            } catch (e: Throwable) {
                thrownException = e
                state = AwaitableFlowApi.State.CANCELED
                return@launch
            }

            if (state != AwaitableFlowApi.State.CANCELED)
                state = AwaitableFlowApi.State.DONE
        }
    }

    override fun cancelFlow() {
        if (state == AwaitableFlowApi.State.RUNNING) {
            collectJob.cancel()
            state = AwaitableFlowApi.State.CANCELED
        }
    }
}

fun <T> awaitableFlow(flow: Flow<T>): AwaitableFlowApi<T> {
    return AwaitableFlow(flow)
}

fun <T> awaitableFlow(block: () -> Flow<T>): AwaitableFlowApi<T> {
    return awaitableFlow(block())
}
