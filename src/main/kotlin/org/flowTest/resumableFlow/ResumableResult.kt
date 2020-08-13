package org.flowTest.resumableFlow

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.fail
import kotlin.reflect.KClass

interface ResumableResultApi<T> {
    infix fun shouldBe(value: T)
    infix fun shouldBe(block: () -> T)
    infix fun then(block: (T?) -> Unit)
    infix fun <E : Throwable> didThrow(type: KClass<E>)
    infix fun <E : Throwable> didThrow(type: () -> KClass<E>)
}

internal class ResumableResult<T>(private val value: T?, private val exception: Throwable?) : ResumableResultApi<T> {

    init {
        if (value != null && exception != null)
            fail("Unexpected exception: ${exception}")
    }

    override infix fun shouldBe(value: T) {
        Assertions.assertEquals(value, this.value)
    }

    override infix fun shouldBe(block: () -> T) {
        Assertions.assertEquals(block(), this.value)
    }

    override fun then(block: (T?) -> Unit) {
        block(value)
    }

    override infix fun <E : Throwable> didThrow(type: KClass<E>) {
        exception ?: fail("Nothing was thrown but got ${value}\nExpected: ${type.simpleName}\n")

        if (!type.isInstance(exception))
            fail("Wrong Throwable type: ${type.simpleName}\nExpect: $type\n")
    }

    override infix fun <E : Throwable> didThrow(type: () -> KClass<E>) {
        didThrow(type)
    }
}