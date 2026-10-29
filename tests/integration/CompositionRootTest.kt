package tests.integration

import config.CompositionRoot
import adapter.out.jdbc.ConnectionProvider
import domain.model.AccessCheckResult
import domain.model.AccessRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tests.support.TestDatabase
import java.nio.file.Path

// Small integration contract: verifies wiring and actual default data separately from unit tests.
class CompositionRootTest {
    @TempDir
    lateinit var directory: Path
    private lateinit var connections: ConnectionProvider

    @BeforeEach
    fun initializeFileDatabaseFromSql() {
        connections = TestDatabase.create(directory.resolve("access"))
    }

    @ParameterizedTest
    @CsvSource(
        "alice,qwerty,read,A.B.C,10,SUCCESS",
        "alice,wrong,read,A.B.C,10,INVALID_PASSWORD",
        "charlie,qwerty,read,A.B.C,10,INVALID_LOGIN",
        "alice,qwerty,remove,A.B.C,10,UNKNOWN_ACTION",
        "bob,secret,execute,A.A8B.C,10,ACCESS_DENIED",
        "alice,qwerty,read,A.B.D,10,RESOURCE_NOT_FOUND",
        "alice,qwerty,read,A..B,10,INVALID_RESOURCE_OR_VOLUME",
        "alice,qwerty,read,A.B.C,none,INVALID_RESOURCE_OR_VOLUME",
        "alice,qwerty,read,A.B.C,21,VOLUME_LIMIT_EXCEEDED",
        "bob,secret,read,A.A8B.C.f_d,10,SUCCESS",
        "alice,qwerty,WRITE,A.B.C,20,SUCCESS",
        "alice,qwerty,execute,A.B.C,20,SUCCESS",
        "alice,qwerty,read,A,0,SUCCESS"
    )
    fun shouldWireRealAdaptersAndPreserveDefaultDataContract(
        login: String, password: String, action: String, resource: String, volume: String, expected: AccessCheckResult
    ) {
        assertEquals(expected, CompositionRoot.createUseCase(connections).execute(AccessRequest(login, password, action, resource, volume)))
    }
}
