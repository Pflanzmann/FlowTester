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
import org.flowTest.flowScenario.testCollect
import org.flowTest.flowScenario.testScenario
import org.flowTest.resumableFlow.ResumableFlow
import org.flowTest.resumableFlow.nextValueOf
import org.flowTest.resumableFlow.resumableFlowOf
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class PlaygroundTest {

    class FlowEndException : Throwable()

    private fun getConstantTimeFlow(): Flow<Int> = flow {
        var value = 0
        emit(value++)
        while (value < 10) {
            delay(1000)
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
    fun resumableFlow1() = runBlocking {
        val testFlow = getConstantTimeFlow()

        val resumableFlow = ResumableFlow(testFlow)

        println("before 0")
        nextValueOf { resumableFlow } shouldBe { 0 }

        println("before 1")
        nextValueOf { resumableFlow } shouldBe { 1 }

        println("before 2")
        nextValueOf { resumableFlow } shouldBe { 2 }

    }

    @Test
    fun resumableFlow2() = runBlocking {
        val testFlow = getConstantTimeFlow()

        val resumableFlow = resumableFlowOf { testFlow }

        nextValueOf { resumableFlow } shouldBe 0

        nextValueOf { resumableFlow } shouldBe 1

        nextValueOf { resumableFlow } shouldBe 2
    }

    @Test
    fun resumableFlow3() = runBlocking {
        val testFlow = getConstantTimeFlow()

        val resumableFlow = resumableFlowOf { testFlow }

        nextValueOf { resumableFlow } then { println(it) }

        nextValueOf { resumableFlow } shouldBe 1

        nextValueOf { resumableFlow } shouldBe 2
    }

    @Test
    fun resumableFlow4() = runBlocking {
        val resumableFlow = resumableFlowOf { getConstantTimeFlow() }

        var expectedValue = 0
        while (expectedValue < 10) {
            println(expectedValue)
            nextValueOf { resumableFlow } shouldBe expectedValue++
        }

        nextValueOf { resumableFlow } didThrow FlowEndException::class

    }

    @Test
    fun resumableFlow5() = runBlocking {
        val resumableFlow = resumableFlowOf { getBroadcastFlow() }

        broadcaster.send(0)
        nextValueOf { resumableFlow } shouldBe 0

        broadcaster.send(1)
        nextValueOf { resumableFlow } shouldBe 1

        broadcaster.send(2)
        nextValueOf { resumableFlow } shouldBe 2
    }
}