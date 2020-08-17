package org.flowTest.resumableFlow

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

interface AwaitableFlowApi<T> {
    enum class State { RUNNING, COMPLETED, CANCELED }

    val state: State
    var timeout: Long
    var dispatcher: CoroutineDispatcher

    fun cancelFlow()
}

class AwaitableFlow<T>(
    private val flow: Flow<T>
) : AwaitableFlowApi<T> {
    override var state = AwaitableFlowApi.State.RUNNING
    override var timeout: Long = 5000L
    override var dispatcher: CoroutineDispatcher = Dispatchers.Default

    internal var valueJob = Job()
    internal var waitingJob = Job()

    internal  var nextValue: T? = null
    internal  var thrownException: Throwable? = null

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
                valueJob.complete()
                return@launch
            }

            if (state != AwaitableFlowApi.State.CANCELED)
                state = AwaitableFlowApi.State.COMPLETED
        }
    }

    override fun cancelFlow() {
        if (state == AwaitableFlowApi.State.RUNNING) {
            collectJob.cancel()
            state = AwaitableFlowApi.State.CANCELED
        }
    }
}
