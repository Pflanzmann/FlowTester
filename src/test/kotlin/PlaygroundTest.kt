import flowTester.org.example.flowTest.FlowScenarioApi
import flowTester.org.example.flowTest.StepDoubleAssignmentException
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.flowTest.exceptions.AwaitableFlowCanceledException
import org.flowTest.flowScenario.testCollect
import org.flowTest.flowScenario.testScenario
import org.flowTest.resumableFlow.AwaitableFlow
import org.flowTest.resumableFlow.AwaitableFlowApi
import org.flowTest.resumableFlow.awaitValueOf
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class PlaygroundTest {

    class FlowEndException : Throwable()

    private fun getConstantTimeFlow(): Flow<Int> = flow {
        var value = 0
        emit(value++)
        while (value < 10) {
            delay(1000L)
            emit(value++)
        }
        throw FlowEndException()
    }

    private fun getListFlow(): Flow<Int> = flowOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)

    private val broadcaster = ConflatedBroadcastChannel<Int>()

    private fun getBroadcastFlow(): Flow<Int> = broadcaster.asFlow()

    @Test
    fun `testScenario, infix, assertDidThrow`() = runBlockingTest {
        val testFlow = getConstantTimeFlow()

        testFlow testScenario {
            take = 4

            before { assertRemainingValuesCount { 0 } }

            doAt(0) { assertNextElement { 0 } }
            doAt(1) { throw IllegalStateException("Some Exception") }

            after {
                assertDidThrow { IllegalStateException::class }
            }
        }
    }

    @Test
    fun `testScenario, not_Infix, assertDidThrow`() = runBlockingTest {
        val testFlow = getConstantTimeFlow()

        testFlow.testScenario(take = 4) {
            before { assertRemainingValuesCount { 0 } }

            doAt(0) { assertNextElement { 0 } }
            doAt(1) { throw IllegalStateException("Some Exception") }

            after {
                assertDidThrow { IllegalStateException::class }
            }
        }
    }

    @Test
    fun `testScenario, then with prestep`() = runBlockingTest {
        val testFlow = getConstantTimeFlow()

        testFlow.testScenario(take = 5) {
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
        val testFlow = getConstantTimeFlow()

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
        val testFlow = getConstantTimeFlow()

        testFlow.testScenario {
            take = 7
            timeOut = FlowScenarioApi.MAX_TIMEOUT

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
        val testFlow = getConstantTimeFlow()

        testFlow.testScenario {
            take = 5
            before { assertRemainingValuesCount { 0 } }

            Assertions.assertThrows(StepDoubleAssignmentException::class.java) {
                doAt(0, 0, 0) { dismissNextValue() }
            }

            after {
                assertRemainingValuesCount { 4 }
            }
        }
    }

    @Test
    fun `testCollect`() = runBlockingTest {
        val testFlow = getConstantTimeFlow()

        testFlow.testCollect(timeOut = 5000) {
            assertFinishWithTimeout()
            assertRemainingValuesCount { 5 }
        }
    }

    @Test
    fun something3() = runBlockingTest {
        val testFlow = getConstantTimeFlow()

        testFlow.testCollect(take = 5) {
            assertNextElement(0)
            assertNextElement(1)
            dismissNextValue()
            assertNextElement(3)
            assertNextElement(4)
            assertAllValuesConsumed()
        }
    }

    @Test
    fun something4() = runBlockingTest {
        val testFlow = getConstantTimeFlow()

        testFlow.testCollect(take = 5) {
            assertNextElement(0)
            assertNextElement { 1 }
            assertNextElement { 2 }
            assertNextElement { 3 }
            assertRemainingValuesCount { 1 }
        }
    }

    @Test
    fun awaitableFlow1() = runBlocking {
        val testFlow = getConstantTimeFlow()

        val awaitableFlow = AwaitableFlow(testFlow)

        awaitValueOf { awaitableFlow } resultEquals { 0 }

        awaitValueOf { awaitableFlow } resultEquals { 1 }

        awaitValueOf { awaitableFlow } resultEquals { 2 }

    }

    @Test
    fun awaitableFlow2() = runBlocking {
        val testFlow = getConstantTimeFlow()

        val awaitableFlow = AwaitableFlow(testFlow)

        awaitValueOf { awaitableFlow } resultEquals 0

        awaitValueOf { awaitableFlow } resultEquals 1

        awaitValueOf { awaitableFlow } resultEquals 2
    }

    @Test
    fun awaitableFlow3() = runBlocking {
        val testFlow = getConstantTimeFlow()

        val awaitableFlow = AwaitableFlow(testFlow)

        awaitValueOf { awaitableFlow } then { println(it) }

        awaitValueOf { awaitableFlow } resultEquals 1

        awaitValueOf { awaitableFlow } resultEquals 2
    }

    @Test
    fun awaitableFlow4() = runBlocking {
        val awaitableFlow = AwaitableFlow(getConstantTimeFlow())

        var expectedValue = 0
        while (expectedValue < 10) {
            awaitValueOf { awaitableFlow } resultEquals expectedValue++
        }

        awaitValueOf { awaitableFlow } didThrow AwaitableFlowCanceledException::class
    }

    @Test
    fun awaitableFlow5() = runBlocking {
        val awaitableFlow = AwaitableFlow(getBroadcastFlow())

        broadcaster.send(0)
        awaitValueOf { awaitableFlow } resultEquals 0

        broadcaster.send(1)
        awaitValueOf { awaitableFlow } resultEquals 1

        broadcaster.send(2)
        awaitValueOf { awaitableFlow } resultEquals 2
    }

    @Test
    fun awaitableFlow6() = runBlocking {
        val awaitableFlow = AwaitableFlow(getBroadcastFlow()).apply { timeout = 1000L }

        broadcaster.send(0)
        awaitValueOf { awaitableFlow } resultEquals 0

        broadcaster.send(1)
        awaitValueOf { awaitableFlow } resultEquals 1

    }

    @Test
    fun awaitableFlow7() = runBlocking {
        val awaitableFlow = AwaitableFlow(getBroadcastFlow()).apply { timeout = 1000L }

        broadcaster.send(0)
        awaitValueOf { awaitableFlow } resultEquals 0

        broadcaster.send(1)
        awaitValueOf { awaitableFlow } resultEquals 1

        awaitableFlow.cancelFlow()

        broadcaster.send(2)
        awaitValueOf { awaitableFlow } didThrow AwaitableFlowCanceledException::class

        Assertions.assertEquals(AwaitableFlowApi.State.CANCELED, awaitableFlow.state)
    }
}