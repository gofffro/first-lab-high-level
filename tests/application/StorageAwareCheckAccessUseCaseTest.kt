package tests.application

import application.port.`in`.CheckAccessUseCase
import application.port.out.StorageConnectionException
import application.port.out.StorageQueryException
import application.usecase.StorageAwareCheckAccessUseCase
import domain.model.AccessCheckResult
import domain.model.AccessRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class StorageAwareCheckAccessUseCaseTest {
    private val request = AccessRequest("alice", "qwerty", "read", "A", "1")

    @Test
    fun shouldTranslateConnectionFailureToDedicatedResult() {
        val useCase = StorageAwareCheckAccessUseCase(CheckAccessUseCase { throw StorageConnectionException(Exception("test")) })
        assertEquals(AccessCheckResult.DATABASE_CONNECTION_ERROR, useCase.execute(request))
    }

    @Test
    fun shouldTranslateQueryFailureToDedicatedResult() {
        val useCase = StorageAwareCheckAccessUseCase(CheckAccessUseCase { throw StorageQueryException(Exception("test")) })
        assertEquals(AccessCheckResult.DATABASE_QUERY_ERROR, useCase.execute(request))
    }

    @ParameterizedTest
    @EnumSource(AccessCheckResult::class)
    fun shouldPreserveDelegatedResultAndRequest(result: AccessCheckResult) {
        val calls = mutableListOf<AccessRequest>()
        val useCase = StorageAwareCheckAccessUseCase(CheckAccessUseCase { calls += it; result })
        assertEquals(result, useCase.execute(request))
        assertEquals(listOf(request), calls)
    }

    @Test
    fun shouldNotHideProgrammingErrorsAsDatabaseFailures() {
        val useCase = StorageAwareCheckAccessUseCase(CheckAccessUseCase { error("bug") })
        assertThrows(IllegalStateException::class.java) { useCase.execute(request) }
    }
}
