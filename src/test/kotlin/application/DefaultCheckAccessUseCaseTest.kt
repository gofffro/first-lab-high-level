package tests.application

import application.port.out.PasswordHasher
import application.port.out.ResourceRepository
import application.port.out.UserRepository
import application.usecase.DefaultCheckAccessUseCase
import domain.model.AccessCheckResult
import domain.model.AccessRequest
import domain.model.Action
import domain.model.User
import domain.policy.ActionParser
import domain.policy.PermissionPolicy
import domain.policy.ResourcePathValidator
import domain.policy.VolumeValidator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class DefaultCheckAccessUseCaseTest {
    private val request = AccessRequest("tester", "password", "read", "A.B", "10")

    // Fresh test doubles per test; these tests do not depend on default users or SHA-256.
    private class Fixture {
        val calls = mutableListOf<String>()
        var user: User? = User("test-salt", "expected-hash", emptyMap())
        var hash = "expected-hash"
        var action: Action? = Action.READ
        var validPath = true
        var volume: Int? = 10
        var maximumVolume: Int? = 20
        var allowed = true

        fun useCase() = DefaultCheckAccessUseCase(
            users = UserRepository { login -> calls += "user:$login"; user },
            resources = ResourceRepository { path -> calls += "resource:$path"; maximumVolume },
            passwordHasher = PasswordHasher { password, salt -> calls += "hash:$password:$salt"; hash },
            actionParser = ActionParser { raw -> calls += "action:$raw"; action },
            resourcePathValidator = ResourcePathValidator { path -> calls += "path:$path"; validPath },
            permissionPolicy = PermissionPolicy { checkedUser, path, requestedAction ->
                assertEquals(user, checkedUser)
                calls += "permission:$path:$requestedAction"
                allowed
            },
            volumeValidator = VolumeValidator { raw -> calls += "volume:$raw"; volume }
        )
    }

    @Test
    fun shouldReturnInvalidLoginBeforeAllOtherChecks() {
        val f = Fixture().apply { user = null; hash = "wrong"; action = null; validPath = false; volume = null }
        assertEquals(AccessCheckResult.INVALID_LOGIN, f.useCase().execute(request))
        assertEquals(listOf("user:tester"), f.calls)
    }

    @Test
    fun shouldReturnInvalidPasswordBeforeActionAndValidation() {
        val f = Fixture().apply { hash = "wrong"; action = null; validPath = false }
        assertEquals(AccessCheckResult.INVALID_PASSWORD, f.useCase().execute(request))
        assertEquals(listOf("user:tester", "hash:password:test-salt"), f.calls)
    }

    @Test
    fun shouldReturnUnknownActionBeforePathValidation() {
        val f = Fixture().apply { action = null; validPath = false }
        assertEquals(AccessCheckResult.UNKNOWN_ACTION, f.useCase().execute(request))
        assertEquals("action:read", f.calls.last())
    }

    @Test
    fun shouldRejectInvalidPathBeforeParsingVolumeOrLookingUpResource() {
        val f = Fixture().apply { validPath = false; maximumVolume = null }
        assertEquals(AccessCheckResult.INVALID_RESOURCE_OR_VOLUME, f.useCase().execute(request))
        assertEquals("path:A.B", f.calls.last())
    }

    @Test
    fun shouldRejectInvalidVolumeBeforeLookingUpResource() {
        val f = Fixture().apply { volume = null; maximumVolume = null }
        assertEquals(AccessCheckResult.INVALID_RESOURCE_OR_VOLUME, f.useCase().execute(request))
        assertEquals("volume:10", f.calls.last())
    }

    @Test
    fun shouldReturnMissingResourceBeforePermissionCheck() {
        val f = Fixture().apply { maximumVolume = null; allowed = false }
        assertEquals(AccessCheckResult.RESOURCE_NOT_FOUND, f.useCase().execute(request))
        assertEquals("resource:A.B", f.calls.last())
    }

    @Test
    fun shouldReturnAccessDeniedBeforeCheckingVolumeLimit() {
        val f = Fixture().apply { allowed = false; volume = 100 }
        assertEquals(AccessCheckResult.ACCESS_DENIED, f.useCase().execute(request))
    }

    @ParameterizedTest
    @CsvSource("0,20,SUCCESS", "19,20,SUCCESS", "20,20,SUCCESS", "21,20,VOLUME_LIMIT_EXCEEDED", "0,0,SUCCESS", "1,0,VOLUME_LIMIT_EXCEEDED", "2147483647,2147483647,SUCCESS")
    fun shouldApplyInclusiveResourceVolumeLimit(volume: Int, maximum: Int, expected: AccessCheckResult) {
        val f = Fixture().apply { this.volume = volume; maximumVolume = maximum }
        assertEquals(expected, f.useCase().execute(request))
    }

    @Test
    fun shouldPassOriginalRequestAndParsedActionToDependencies() {
        val f = Fixture().apply { action = Action.WRITE }
        val command = request.copy(login = "another", password = "other-password", action = "WRITE", resource = "X.Y", volume = "0010")
        assertEquals(AccessCheckResult.SUCCESS, f.useCase().execute(command))
        assertEquals(listOf("user:another", "hash:other-password:test-salt", "action:WRITE", "path:X.Y", "volume:0010", "resource:X.Y", "permission:X.Y:WRITE"), f.calls)
    }
}
