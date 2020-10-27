import flowTester.scenario.FlowScenario
import flowTester.scenario.FlowScenarioApi
import flowTester.scenario.testCollect
import flowTester.scenario.testScenario
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class PlaygroundTest {

    class SomeRandomException : Throwable()

    private fun getConstantTimeFlow(): Flow<Int> = flow {
        var value = 0
        emit(value++)
        while (value < 10) {
            delay(1000L)
            emit(value++)
        }
        throw SomeRandomException()
    }

    private fun getListFlow(): Flow<Int> = flowOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)

    private val broadcaster = ConflatedBroadcastChannel<Int>()

    private fun getBroadcastFlow(): Flow<Int> = broadcaster.asFlow()

    private val stateFlow = MutableStateFlow<Int>(0)

    @Nested
    inner class TestScenario_Working {

        @Test
        fun `testScenario 1`() = runBlockingTest {
            val testFlow = getConstantTimeFlow()

            testFlow testScenario {
                take = 4

                doAt(0) { Assertions.assertEquals(0, pollValue) }
                doAt(1, canThrow = true) { throw IllegalStateException("Some Exception") }

                afterAll { Assertions.assertEquals(3, numberOfUnconsumedValues()) }
            }
        }

        @Test
        fun `testScenario 2`() = runBlockingTest {
            val testFlow = getConstantTimeFlow()

            testFlow.testScenario(take = 4) {

                doAt(0) { Assertions.assertEquals(0, pollValue) }
                doAt(1, canThrow = true) { throw IllegalStateException("Some Exception") }

                afterAll { Assertions.assertEquals(3, numberOfUnconsumedValues()) }
            }
        }

        @Test
        fun `testScenario 3`() = runBlockingTest {
            val testFlow = getConstantTimeFlow()

            testFlow.testScenario(take = 5) {

                doAt(0) { Assertions.assertEquals(0, pollValue) }
                then { Assertions.assertEquals(1, pollValue) }
                then { Assertions.assertEquals(2, pollValue) }

                afterAll { Assertions.assertEquals(2, numberOfUnconsumedValues()) }
            }
        }

        @Test
        fun `testScenario 4`() = runBlockingTest {
            val testFlow = getConstantTimeFlow()

            testFlow.testScenario(take = 3, confirmSteps = false) {

                doAt(0) { Assertions.assertEquals(0, pollValue) }
                doAt(1) { Assertions.assertEquals(1, pollValue) }
                doAt(2) { Assertions.assertEquals(2, pollValue) }

                then { Assertions.fail() }
                then { Assertions.fail() }

                afterAll { Assertions.assertEquals(0, numberOfUnconsumedValues()) }
            }
        }

        @Test
        fun `testScenario 5`() = runBlockingTest {
            val testFlow = getConstantTimeFlow()

            testFlow.testScenario {
                take = 7
                timeOut = FlowScenarioApi.MAX_TIMEOUT

                doAt(0, 1, 2, 3) { dismissValue() }
                doAt(4) { Assertions.assertEquals(4, pollValue) }
                doAt(5) { Assertions.assertEquals(5, pollValue) }


                afterAll { Assertions.assertEquals(1, numberOfUnconsumedValues()) }
            }
        }

        @Test
        fun `testScenario 6`() = runBlockingTest {
            val testFlow = getConstantTimeFlow()

            testFlow.testScenario {
                take = 5

                Assertions.assertThrows(FlowScenario.StepDoubleAssignmentException::class.java) {
                    doAt(0, 0, 0) { dismissValue() }
                }

                afterAll { Assertions.assertEquals(4, numberOfUnconsumedValues()) }
            }
        }

        @Test
        fun `testScenario 7`() = runBlockingTest {
            val testFlow = getConstantTimeFlow()

            testFlow.testScenario {

                afterAll { Assertions.assertFalse(consumedAllSteps()) }
            }
        }

        @Test
        fun `testScenario 8`() = runBlockingTest {
            val testFlow = stateFlow

            var currentPostion = 0

            testFlow.testScenario {
                take = 7
                forceConsumeAllSteps = false

                beforeAll { }

                doAt(0, 1, 2, 3) {
                    dismissValue()
                    stateFlow.emit(++currentPostion)
                }
                then {
                    doAt(4) { Assertions.assertEquals(4, pollValue) }
                    stateFlow.emit(++currentPostion)
                }
                then {
                    doAt(5) { Assertions.assertEquals(5, pollValue) }
                    stateFlow.emit(++currentPostion)
                    stateFlow.emit(++currentPostion)
                }

                afterAll {
                    afterAll { Assertions.assertEquals(1, numberOfUnconsumedValues()) }
                }
            }
        }

        @Test
        fun `testScenario 9`() = runBlockingTest {
            val testFlow = flowOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)

            testFlow testScenario {
                take = 4

                doAt(0) { Assertions.assertEquals(0, pollValue) }
                doAt(1) { Assertions.assertEquals(1, pollValue) }
                then { Assertions.assertEquals(2, pollValue) }
                then { Assertions.assertEquals(3, pollValue) }

                afterAll { Assertions.assertEquals(0, numberOfUnconsumedValues()) }
            }
        }

        @Test
        fun `testScenario 10`() = runBlockingTest {
            val testFlow = getConstantTimeFlow()

            testFlow.testScenario {
                forceConsumeAllSteps = false
                timeOut = 500L

                doAt(0, 1, 2, 3) { dismissValue() }

                afterAll { Assertions.assertTrue(finishWithTimeout()) }
            }
        }
    }

    @Nested
    inner class TestCollect {

        @Test
        fun `testCollect 1`() = runBlockingTest {
            val testFlow = getConstantTimeFlow()

            testFlow.testCollect(timeOut = 5000) {

            }
        }

        @Test
        fun `testCollect 2`() = runBlockingTest {
            val testFlow = getConstantTimeFlow()

            testFlow.testCollect(take = 5) {
                Assertions.assertEquals(0, pollValue)
                Assertions.assertEquals(1, pollValue)
                dismissValue()
                Assertions.assertEquals(3, pollValue)
                Assertions.assertEquals(4, pollValue)
            }
        }

        @Test
        fun `testCollect 3`() = runBlockingTest {
            val testFlow = getConstantTimeFlow()

            testFlow.testCollect(take = 5) {
                Assertions.assertEquals(0, pollValue)
                Assertions.assertEquals(1, pollValue)
                Assertions.assertEquals(2, pollValue)
                Assertions.assertEquals(3, pollValue)
                Assertions.assertEquals(4, pollValue)
            }
        }
    }
}