package tests.adapter

import adapter.`in`.cli.CheckAccessCommand
import adapter.`in`.cli.CliController
import application.port.`in`.CheckAccessUseCase
import domain.model.AccessCheckResult
import domain.model.AccessRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class CliControllerTest {
    @ParameterizedTest
    @EnumSource(AccessCheckResult::class)
    fun shouldForwardCommandExactlyOnceAndPreserveUseCaseResult(result: AccessCheckResult) {
        val calls = mutableListOf<AccessRequest>()
        val controller = CliController(CheckAccessUseCase { calls += it; result })
        val command = CheckAccessCommand("tester", "secret", "WrItE", "X.Y", "0010")
        assertEquals(result, controller.execute(command))
        assertEquals(listOf(AccessRequest("tester", "secret", "WrItE", "X.Y", "0010")), calls)
    }
}
