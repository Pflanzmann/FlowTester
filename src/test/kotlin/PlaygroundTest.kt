import flowtester.scenario.FlowScenario
import flowtester.scenario.FlowScenarioApi
import flowtester.scenario.testCollect
import flowtester.scenario.testScenario
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError

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

                doAt(0) { nextValueEquals { 0 } }
                doAt(1, canThrow = true) { throw IllegalStateException("Some Exception") }

                afterAll { remainingValuesCount(3) }
            }
        }

        @Test
        fun `testScenario 2`() = runBlockingTest {
            val testFlow = getConstantTimeFlow()

            testFlow.testScenario(take = 4) {

                doAt(0) { nextValueEquals { 0 } }
                doAt(1, canThrow = true) { throw IllegalStateException("Some Exception") }

                afterAll { remainingValuesCount(3) }
            }
        }

        @Test
        fun `testScenario 3`() = runBlockingTest {
            val testFlow = getConstantTimeFlow()

            testFlow.testScenario(take = 5) {

                doAt(0) { nextValueEquals { 0 } }
                then { nextValueEquals { 1 } }
                then { nextValueEquals { 2 } }

                afterAll { remainingValuesCount { 2 } }
            }
        }

        @Test
        fun `testScenario 4`() = runBlockingTest {
            val testFlow = getConstantTimeFlow()

            testFlow.testScenario(take = 3, confirmSteps = false) {

                doAt(0) { nextValueEquals { 0 } }
                doAt(1) { nextValueEquals { 1 } }
                doAt(2) { nextValueEquals { 2 } }
                then { nextValueEquals { -1 } }
                then { nextValueEquals { -1 } }

                afterAll { remainingValuesCount { 0 } }
            }
        }

        @Test
        fun `testScenario 5`() = runBlockingTest {
            val testFlow = getConstantTimeFlow()

            testFlow.testScenario {
                take = 7
                timeOut = FlowScenarioApi.MAX_TIMEOUT


                doAt(0, 1, 2, 3) { dismissValue() }
                then { nextValueEquals { 4 } }
                then { nextValueEquals { 5 } }

                afterAll { remainingValuesCount { 1 } }
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

                afterAll { remainingValuesCount { 4 } }
            }
        }

        @Test
        fun `testScenario 7`() = runBlockingTest {
            val testFlow = getConstantTimeFlow()

            Assertions.assertThrows(AssertionFailedError::class.java) {
                runBlockingTest {
                    testFlow.testScenario {

                        afterAll { consumedEnough() }
                    }
                }
            }
        }

        @Test
        fun `testScenario 8`() = runBlockingTest {
            val testFlow = stateFlow

            var currentPostion = 0

            testFlow.testScenario {
                take = 7

                beforeAll { }

                doAt(0, 1, 2, 3) {
                    dismissValue()
                    stateFlow.emit(++currentPostion)
                }
                then {
                    nextValueEquals { 4 }
                    stateFlow.emit(++currentPostion)
                }
                then {
                    nextValueEquals { 5 }
                    stateFlow.emit(++currentPostion)
                    stateFlow.emit(++currentPostion)
                }

                afterAll {
                    remainingValuesCount { 1 }
                    consumedEnough()
                }
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
                nextValueEquals(0)
                nextValueEquals(1)
                dismissValue()
                nextValueEquals(3)
                nextValueEquals(4)
            }
        }

        @Test
        fun `testCollect 3`() = runBlockingTest {
            val testFlow = getConstantTimeFlow()

            testFlow.testCollect(take = 5) {
                nextValueEquals(0)
                nextValueEquals { 1 }
                nextValueEquals { 2 }
                nextValueEquals { 3 }
            }
        }
    }
}