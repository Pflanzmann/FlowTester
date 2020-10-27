import flowTester.exception.StepDoubleAssignmentException
import flowTester.scenario.FlowScenario
import flowTester.starter.testCollect
import flowTester.starter.testScenario
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class PlaygroundTest {

    class SomeRandomException : Throwable()

    private fun emitTo9AndThrowThenWithDelay(): Flow<Int> = flow {
        var value = 0
        while (value < 10) {
            emit(value++)
            delay(1000L)
        }
        throw SomeRandomException()
    }

    private val stateFlow = MutableStateFlow<Int>(0)

    @Nested
    inner class TestScenario_Working {

        @Test
        fun `testScenario 01`() = runBlockingTest {
            val testFlow = emitTo9AndThrowThenWithDelay()

            testFlow testScenario {
                allowUncaughtThrowable = true
                take = 15

                doAt(0) { Assertions.assertEquals(0, pollValue) }

                afterAll {
                    Assertions.assertEquals(9, numberOfUnconsumedValues())
                }
            }
        }

        @Test
        fun `testScenario 02`() = runBlockingTest {
            val testFlow = emitTo9AndThrowThenWithDelay()

            testFlow.testScenario(take = 15) {
                doAt(0) { Assertions.assertEquals(0, pollValue) }

                afterAll {
                    didThrow(SomeRandomException())
                    Assertions.assertEquals(9, numberOfUnconsumedValues())
                }
            }
        }

        @Test
        fun `testScenario 03`() = runBlockingTest {
            val testFlow = emitTo9AndThrowThenWithDelay()

            testFlow.testScenario(take = 5) {

                doAt(0) { Assertions.assertEquals(0, pollValue) }
                then { Assertions.assertEquals(1, pollValue) }
                then { Assertions.assertEquals(2, pollValue) }

                afterAll { Assertions.assertEquals(2, numberOfUnconsumedValues()) }
            }
        }

        @Test
        fun `testScenario 04`() = runBlockingTest {
            val testFlow = emitTo9AndThrowThenWithDelay()

            testFlow.testScenario(take = 3, verifyAllSteps = false) {

                doAt(0) { Assertions.assertEquals(0, pollValue) }
                doAt(1) { Assertions.assertEquals(1, pollValue) }
                doAt(2) { Assertions.assertEquals(2, pollValue) }

                then { Assertions.fail() }
                then { Assertions.fail() }

                afterAll { Assertions.assertEquals(0, numberOfUnconsumedValues()) }
            }
        }

        @Test
        fun `testScenario 05`() = runBlockingTest {
            val testFlow = emitTo9AndThrowThenWithDelay()

            testFlow.testScenario {
                take = 7

                doAt(0, 1, 2, 3) { dismissValue() }
                doAt(4) { Assertions.assertEquals(4, pollValue) }
                doAt(5) { Assertions.assertEquals(5, pollValue) }


                afterAll { Assertions.assertEquals(1, numberOfUnconsumedValues()) }
            }
        }

        @Test
        fun `testScenario 06`() = runBlockingTest {
            val testFlow = emitTo9AndThrowThenWithDelay()

            testFlow.testScenario {
                take = 5

                Assertions.assertThrows(StepDoubleAssignmentException::class.java) {
                    doAt(0, 0, 0) { dismissValue() }
                }

                afterAll { Assertions.assertEquals(4, numberOfUnconsumedValues()) }
            }
        }

        @Test
        fun `testScenario 07`() = runBlockingTest {
            val testFlow = emitTo9AndThrowThenWithDelay()

            testFlow.testScenario {
                allowUncaughtThrowable = true

                afterAll { Assertions.assertFalse(usedAllSteps()) }
            }
        }

        @Test
        fun `testScenario 08`() = runBlockingTest {
            val testFlow = stateFlow

            var currentPostion = 0

            testFlow.testScenario {
                take = 7
                verifyAllSteps = false

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
        fun `testScenario 09`() = runBlockingTest {
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
            val testFlow = emitTo9AndThrowThenWithDelay()

            testFlow.testScenario {
                timeOut = 500L

                doAt(0, 1, 2, 3) { dismissValue() }

                afterAll { Assertions.assertTrue(finishWithTimeout()) }
            }
        }

        @Test
        fun `testScenario 11`() = runBlockingTest {
            val testFlow = emitTo9AndThrowThenWithDelay()

            var incrementalExpectedInt = 0

            testFlow.testScenario {
                timeOut = 5000L

                doAt(0, 1, 2, 3) { Assertions.assertEquals(incrementalExpectedInt++, popValue) }

                afterAll {
                    Assertions.assertTrue(finishWithTimeout())
                    Assertions.assertEquals(1, numberOfUnconsumedValues())
                }
            }
        }

        @Test
        fun `testScenario 12`() = runBlockingTest {
            val testFlow = emitTo9AndThrowThenWithDelay()

            var incrementalExpectedInt = 0

            testFlow.testScenario {
                timeOut = 5000L

                doAt(0, 1, 2, 3) { Assertions.assertEquals(incrementalExpectedInt++, pollValue) }

                afterAll { Assertions.assertTrue(finishWithTimeout()) }
            }
        }
    }

    @Nested
    inner class TestCollect {

        @Test
        fun `testCollect 1`() = runBlockingTest {
            val testFlow = emitTo9AndThrowThenWithDelay()

            testFlow.testCollect(timeOut = 5000) {

            }
        }

        @Test
        fun `testCollect 2`() = runBlockingTest {
            val testFlow = emitTo9AndThrowThenWithDelay()

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
            val testFlow = emitTo9AndThrowThenWithDelay()

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