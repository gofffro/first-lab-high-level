package tests.integration

import adapter.out.jdbc.ConnectionProvider
import adapter.out.jdbc.H2ConnectionProvider
import adapter.out.jdbc.JdbcResourceRepository
import adapter.out.jdbc.JdbcUserRepository
import application.port.out.StorageConnectionException
import application.port.out.StorageQueryException
import config.CompositionRoot
import domain.model.AccessCheckResult
import domain.model.AccessRequest
import domain.model.Action
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tests.support.TestDatabase
import java.nio.file.Files
import java.nio.file.Path
import java.sql.SQLException

class JdbcRepositoryTest {
    @TempDir
    lateinit var directory: Path
    private lateinit var connections: ConnectionProvider

    @BeforeEach
    fun initializeFileDatabase() {
        connections = TestDatabase.create(directory.resolve("access"))
    }

    @Test
    fun shouldLoadUserHashSaltAndAllPermissionsFromSql() {
        val user = requireNotNull(JdbcUserRepository(connections).findByLogin("alice"))
        assertEquals("alice-salt-2026", user.salt)
        assertEquals("e91c00f48dd574a479152d06a37b919eff4669324e080aa7fc90de3a94e722b6", user.passwordHash)
        assertEquals(mapOf("A" to setOf(Action.READ), "A.B.C" to setOf(Action.WRITE, Action.EXECUTE)), user.permissions)
    }

    @Test
    fun shouldLoadBobPermissionsWithoutMixingOtherUsers() {
        val user = requireNotNull(JdbcUserRepository(connections).findByLogin("bob"))
        assertEquals(mapOf("A.A8B" to setOf(Action.READ, Action.WRITE)), user.permissions)
    }

    @Test
    fun shouldNotMatchUnknownOrSqlInjectedLogin() {
        val repository = JdbcUserRepository(connections)
        assertNull(repository.findByLogin("missing"))
        assertNull(repository.findByLogin("Alice"))
        assertNull(repository.findByLogin("' OR 1=1 --"))
    }

    @Test
    fun shouldReturnUserWithoutPermissions() {
        connections.open().use { connection ->
            connection.createStatement().use {
                it.executeUpdate("INSERT INTO app_users VALUES ('guest', 'salt', '${"0".repeat(64)}')")
            }
        }
        assertEquals(emptyMap<String, Set<Action>>(), JdbcUserRepository(connections).findByLogin("guest")?.permissions)
    }

    @Test
    fun shouldReadExactLimitAndRejectMissingOrInjectedPath() {
        val repository = JdbcResourceRepository(connections)
        assertEquals(20, repository.findMaximumVolume("A.B.C"))
        assertNull(repository.findMaximumVolume("A.B.C.D"))
        assertNull(repository.findMaximumVolume("a.b.c"))
        assertNull(repository.findMaximumVolume("' OR 1=1 --"))
    }

    @Test
    fun shouldReturnZeroLimitAsPresentResource() {
        execute("INSERT INTO resources VALUES ('Z', 0)")
        assertEquals(0, JdbcResourceRepository(connections).findMaximumVolume("Z"))
    }

    @Test
    fun shouldPersistChangesAfterConnectionClosesAndUseDatabaseInsteadOfDefaults() {
        execute("UPDATE resources SET maximum_volume = 5 WHERE path = 'A.B.C'")
        val reopened = H2ConnectionProvider(directory.resolve("access").toString())
        assertEquals(5, JdbcResourceRepository(reopened).findMaximumVolume("A.B.C"))
        val result = CompositionRoot.createUseCase(reopened).execute(AccessRequest("alice", "qwerty", "read", "A.B.C", "6"))
        assertEquals(AccessCheckResult.VOLUME_LIMIT_EXCEEDED, result)
        assertTrue(Files.isRegularFile(directory.resolve("access.mv.db")))
    }

    @Test
    fun shouldInitializeTwiceWithoutDuplicatesOrOverwritingExistingLimit() {
        execute("UPDATE resources SET maximum_volume = 5 WHERE path = 'A.B.C'")
        connections.open().use { connection ->
            TestDatabase.initialize(connection)
            connection.createStatement().use { statement ->
                for ((table, count) in mapOf("app_users" to 2, "resources" to 6, "permissions" to 5)) {
                    statement.executeQuery("SELECT COUNT(*) FROM $table").use { rows ->
                        assertTrue(rows.next()); assertEquals(count, rows.getInt(1), table)
                    }
                }
            }
        }
        assertEquals(5, JdbcResourceRepository(connections).findMaximumVolume("A.B.C"))
    }

    @Test
    fun shouldRejectOrphanPermissionsAndNegativeLimits() {
        assertThrows(SQLException::class.java) { execute("INSERT INTO permissions VALUES ('missing', 'A', 'READ')") }
        assertThrows(SQLException::class.java) { execute("INSERT INTO resources VALUES ('Z', -1)") }
        assertThrows(SQLException::class.java) { execute("INSERT INTO permissions VALUES ('alice', 'A', 'DELETE')") }
    }

    @Test
    fun shouldReturnConnectionFailureWithoutCreatingMissingDatabase() {
        val path = directory.resolve("missing")
        val provider = H2ConnectionProvider(path.toString())
        assertThrows(StorageConnectionException::class.java) { JdbcUserRepository(provider).findByLogin("alice") }
        assertEquals(AccessCheckResult.DATABASE_CONNECTION_ERROR, CompositionRoot.createUseCase(provider).execute(AccessRequest("alice", "qwerty", "read", "A", "1")))
        assertFalse(Files.exists(directory.resolve("missing.mv.db")))
    }

    @Test
    fun shouldReturnQueryFailureWhenSchemaIsBroken() {
        execute("ALTER TABLE resources RENAME COLUMN maximum_volume TO broken_volume")
        assertThrows(StorageQueryException::class.java) { JdbcResourceRepository(connections).findMaximumVolume("A") }
        assertEquals(AccessCheckResult.DATABASE_QUERY_ERROR, CompositionRoot.createUseCase(connections).execute(AccessRequest("alice", "qwerty", "read", "A", "1")))
    }

    @Test
    fun shouldCloseConnectionAfterSuccessfulAndFailedQueries() {
        val opened = mutableListOf<java.sql.Connection>()
        val observed = ConnectionProvider { connections.open().also { opened += it } }
        JdbcUserRepository(observed).findByLogin("alice")
        JdbcResourceRepository(observed).findMaximumVolume("A")
        execute("ALTER TABLE resources RENAME COLUMN maximum_volume TO broken_volume")
        assertThrows(StorageQueryException::class.java) { JdbcResourceRepository(observed).findMaximumVolume("A") }
        assertEquals(3, opened.size)
        assertTrue(opened.all { it.isClosed })
    }

    private fun execute(sql: String) {
        connections.open().use { connection -> connection.createStatement().use { it.executeUpdate(sql) } }
    }
}
