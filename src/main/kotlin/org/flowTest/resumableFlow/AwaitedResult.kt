package org.flowTest.resumableFlow

import org.junit.jupiter.api.Assertions

interface AwaitedResultApi<T> {

    infix fun resultEquals(value: T)
    infix fun resultEquals(block: () -> T)
    infix fun then(block: (T?) -> Unit)
}

internal class AwaitedResult<T>(private val value: T?) : AwaitedResultApi<T> {

    override infix fun resultEquals(value: T) {
        Assertions.assertEquals(value, this.value)
    }

    override infix fun resultEquals(block: () -> T) {
        Assertions.assertEquals(block(), this.value)
    }

    override fun then(block: (T?) -> Unit) {
        block(value)
    }
}