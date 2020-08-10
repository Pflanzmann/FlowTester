import flowTester.org.example.flowTest.FlowScenarioApi
import flowTester.org.example.flowTest.StepDoubleAssignmentException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.flowTest.testCollect
import org.flowTest.testScenario
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class PlaygroundTest {

    class FlowEndException() : Throwable()

    private fun getConstantFlow(): Flow<Int> = flow {
        var value = 0
        while (value < 10) {
            emit(value++)
            delay(1000)
        }
        throw FlowEndException()
    }

    @Test
    fun `testScenario, infix, assertDidThrow`() = runBlockingTest {
        val testFlow = getConstantFlow()

        testFlow testScenario {
            timeOut = 4500L

            before { assertRemainingValuesCount { 0 } }

            doAt(0) { assertNextElement { 0 } }
            doAt(1) { throw IllegalStateException("Some Exception") }

            after {
                assertDidThrow { IllegalStateException::class.java }
            }
        }
    }

    @Test
    fun `testScenario, infix, `() = runBlockingTest {
        val testFlow = getConstantFlow()

        testFlow testScenario {
            take = 12
            confirmSteps = false
            timeOut = FlowScenarioApi.MAX_TIMEOUT
            allowThrowable = false

            doAt(0) { assertNextElement { 0 } }
            doAt(1) { assertNextElement { 1 } }
            then { assertNextElement { 2 } }
            then { dismissNextValue() }
            then { assertNextElement { 4 } }
        }


    }

    @Test
    fun `testScenario, not_Infix, assertDidThrow`() = runBlockingTest {
        val testFlow = getConstantFlow()

        testFlow.testScenario(4500L) {
            before { assertRemainingValuesCount { 0 } }

            doAt(0) { assertNextElement { 0 } }
            doAt(1) { throw IllegalStateException("Some Exception") }

            after {
                assertDidThrow { IllegalStateException::class.java }
            }
        }
    }

    @Test
    fun `testScenario, then with prestep`() = runBlockingTest {
        val testFlow = getConstantFlow()

        testFlow.testScenario(4500L) {
            before { assertRemainingValuesCount { 0 } }

            doAt(0) { assertNextElement { 0 } }
            then { assertNextElement { 1 } }
            then { assertNextElement { 2 } }

            after {
                assertRemainingValuesCount { 2 }
            }
        }
    }

    @Test
    fun `testScenario, then without prestep`() = runBlockingTest {
        val testFlow = getConstantFlow()

        testFlow.testScenario(take = 3, confirmSteps = false) {

            before { assertRemainingValuesCount { 0 } }

            doAt(0) { assertNextElement { 0 } }
            doAt(1) { assertNextElement { 1 } }
            doAt(2) { assertNextElement { 2 } }
            then { assertNextElement { -1 } }
            then { assertNextElement { -1 } }

            after {
                assertRemainingValuesCount { 0 }
            }
        }
    }

    @Test
    fun `testScenario, vararg, then`() = runBlockingTest {
        val testFlow = getConstantFlow()

        testFlow.testScenario {
            timeOut = 6500L

            before { assertRemainingValuesCount { 0 } }

            doAt(0, 1, 2, 3) { dismissNextValue() }
            then { assertNextElement { 4 } }
            then { assertNextElement { 5 } }

            after {
                assertRemainingValuesCount { 1 }
            }
        }
    }

    @Test
    fun `testScenario, vararg, doubleAssignment`() = runBlockingTest {
        val testFlow = getConstantFlow()

        testFlow.testScenario {
            timeOut = 6500L

            before { assertRemainingValuesCount { 0 } }

            Assertions.assertThrows(StepDoubleAssignmentException::class.java) {
                doAt(0, 0) { dismissNextValue() }
            }

            after {
                assertRemainingValuesCount { 6 }
            }
        }
    }

    @Test
    fun `testCollect`() = runBlockingTest {
        val testFlow = getConstantFlow()

        testFlow.testCollect(4500L) {
            assertFinishWithTimeout()
            assertRemainingValuesCount { 5 }
        }
    }

    @Test
    fun something3() = runBlockingTest {
        val testFlow = getConstantFlow()

        testFlow.testCollect(4500L) {
            assertNextElement { 0 }
            assertNextElement { 1 }
            dismissNextValue()
            assertNextElement { 3 }
            assertNextElement { 4 }
            assertAllValuesConsumed()
        }
    }

    @Test
    fun something4() = runBlockingTest {
        val testFlow = getConstantFlow()

        testFlow.testCollect(take = 5) {
            assertNextElement { 0 }
            assertNextElement { 1 }
            assertNextElement { 2 }
            assertNextElement { 3 }
            assertRemainingValuesCount { 1 }
        }
    }
}