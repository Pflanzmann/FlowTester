import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import tice.helper.testScenarioWithTimeout

internal class PlaygroundTest {

    fun getConstantFlow(): Flow<Int> = flow {
        var value = 0
        while (value < 10) {
            emit(value++)
            delay(1000)
        }
    }

    @Test
    fun something() = runBlockingTest {
        val testFlow = getConstantFlow()

        testFlow testScenarioWithTimeout {

            stepAt(0) { assertNextElement { 0 } }
            stepAt(1) { assertNextElement { 1 } }
            stepAt(2) { assertNextElement { 2 } }
            stepAt(3) { assertNextElement { 3 } }

        }
    }
}