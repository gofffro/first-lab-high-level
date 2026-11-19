package adapter.out.jdbc

import application.port.out.ResourceRepository
import application.port.out.StorageConnectionException
import application.port.out.StorageQueryException
import application.port.out.UserRepository
import domain.model.Action
import domain.model.User
import java.sql.Connection
import java.sql.SQLDataException
import java.sql.SQLException

abstract class JdbcRepository(private val connections: ConnectionProvider) {
    protected fun <T> query(block: (Connection) -> T): T {
        val connection = try {
            connections.open()
        } catch (e: SQLException) {
            throw StorageConnectionException(e)
        }
        return try {
            connection.use(block)
        } catch (e: SQLException) {
            throw StorageQueryException(e)
        }
    }
}

class JdbcUserRepository(connections: ConnectionProvider) : JdbcRepository(connections), UserRepository {
    override fun findByLogin(login: String): User? = query { connection ->
        connection.prepareStatement("""
            SELECT u.password_salt, u.password_hash, p.resource_path, p.action
            FROM app_users u
            LEFT JOIN permissions p ON p.login = u.login
            WHERE u.login = ?
        """.trimIndent()).use { statement ->
            statement.setString(1, login)
            statement.executeQuery().use { rows ->
                if (!rows.next()) return@query null
                val salt = rows.getString("password_salt")
                val hash = rows.getString("password_hash")
                val permissions = mutableMapOf<String, MutableSet<Action>>()
                do {
                    val path = rows.getString("resource_path") ?: continue
                    val action = try {
                        Action.valueOf(rows.getString("action"))
                    } catch (e: IllegalArgumentException) {
                        throw SQLDataException("Invalid action in database", e)
                    }
                    permissions.getOrPut(path) { mutableSetOf() }.add(action)
                } while (rows.next())
                User(salt, hash, permissions.mapValues { it.value.toSet() })
            }
        }
    }
}

class JdbcResourceRepository(connections: ConnectionProvider) : JdbcRepository(connections), ResourceRepository {
    override fun findMaximumVolume(path: String): Int? = query { connection ->
        connection.prepareStatement("SELECT maximum_volume FROM resources WHERE path = ?").use { statement ->
            statement.setString(1, path)
            statement.executeQuery().use { rows ->
                if (rows.next()) rows.getInt("maximum_volume") else null
            }
        }
    }
}
