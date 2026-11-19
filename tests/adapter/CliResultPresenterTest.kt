package tests.adapter

import adapter.`in`.cli.CliResultPresenter
import domain.model.AccessCheckResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class CliResultPresenterTest {
    @ParameterizedTest
    @CsvSource("SUCCESS,0", "INVALID_PASSWORD,2", "INVALID_LOGIN,3", "UNKNOWN_ACTION,4", "ACCESS_DENIED,5", "RESOURCE_NOT_FOUND,6", "INVALID_RESOURCE_OR_VOLUME,7", "VOLUME_LIMIT_EXCEEDED,8", "DATABASE_CONNECTION_ERROR,9", "DATABASE_QUERY_ERROR,10")
    fun shouldPreserveExitCodeContract(result: AccessCheckResult, expectedCode: Int) {
        assertEquals(expectedCode, CliResultPresenter.exitCode(result))
    }
}
