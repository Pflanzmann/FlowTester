package org.flowTest.resumableFlow

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.fail
import kotlin.reflect.KClass

interface AwaitedResultApi<T> {

    infix fun resultEquals(value: T)
    infix fun resultEquals(block: () -> T)
    infix fun then(block: (T?) -> Unit)
    infix fun <I : Throwable> didThrow(throwable: KClass<I>)
}

internal class AwaitedResult<T>(private val value: T?, private val throwable: Throwable? = null) : AwaitedResultApi<T> {

    override infix fun resultEquals(value: T) {
        Assertions.assertEquals(value, this.value)
    }

    override infix fun resultEquals(block: () -> T) {
        resultEquals(block())
    }

    override fun then(block: (T?) -> Unit) {
        block(value)
    }

    override fun <I : Throwable> didThrow(type: KClass<I>) {
        if (!type.isInstance(throwable))
            fail("Did not throw ${type.simpleName}\nThrew instead: $throwable\n")
    }
}