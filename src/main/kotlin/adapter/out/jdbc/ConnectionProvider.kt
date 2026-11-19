package adapter.out.jdbc

import java.nio.file.InvalidPathException
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

fun interface ConnectionProvider {
    fun open(): Connection
}

class H2ConnectionProvider(private val databasePath: String) : ConnectionProvider {
    override fun open(): Connection {
        try {
            // Explicit loading also works when the driver is bundled into app.jar.
            Class.forName("org.h2.Driver")
            val path = Paths.get(databasePath).toAbsolutePath().normalize().toString().replace('\\', '/')
            return DriverManager.getConnection("jdbc:h2:file:$path;IFEXISTS=TRUE;TRACE_LEVEL_FILE=0", "sa", "")
        } catch (e: ClassNotFoundException) {
            throw SQLException("H2 driver is unavailable", e)
        } catch (e: InvalidPathException) {
            throw SQLException("Invalid database path", e)
        }
    }
}
