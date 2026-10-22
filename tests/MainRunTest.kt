import application.port.`in`.CheckAccessUseCase
import domain.model.AccessCheckResult
import domain.model.AccessRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class MainRunTest {
    private val validArguments = arrayOf("--login", "tester", "--password", "secret", "--action", "read", "--resource", "A.B", "--volume", "10")

    @ParameterizedTest
    @CsvSource("SUCCESS,0", "INVALID_PASSWORD,2", "INVALID_LOGIN,3", "UNKNOWN_ACTION,4", "ACCESS_DENIED,5", "RESOURCE_NOT_FOUND,6", "INVALID_RESOURCE_OR_VOLUME,7", "VOLUME_LIMIT_EXCEEDED,8")
    fun shouldReturnUseCaseExitCodeWithoutPrintingBusinessResult(result: AccessCheckResult, expectedCode: Int) {
        val calls = mutableListOf<AccessRequest>()
        val output = mutableListOf<String>()
        val useCase = CheckAccessUseCase { calls += it; result }
        assertEquals(expectedCode, run(validArguments, useCase) { output += it })
        assertEquals(listOf(AccessRequest("tester", "secret", "read", "A.B", "10")), calls)
        assertTrue(output.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["-h", "--help"])
    fun shouldPrintHelpWithoutCallingUseCase(option: String) {
        val output = mutableListOf<String>()
        val useCase = CheckAccessUseCase { error("Help must not call the use case") }
        assertEquals(1, run(arrayOf(option), useCase) { output += it })
        assertEquals(1, output.size)
        assertTrue(output.single().startsWith("Usage: java -jar app.jar"))
    }

    @Test
    fun shouldPrintHelpForMissingArgumentsWithoutCallingUseCase() {
        val output = mutableListOf<String>()
        val useCase = CheckAccessUseCase { error("Invalid CLI input must not call the use case") }
        assertEquals(7, run(emptyArray(), useCase) { output += it })
        assertEquals(1, output.size)
    }

}
