package tests.support

import adapter.out.jdbc.ConnectionProvider
import adapter.out.jdbc.H2ConnectionProvider
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager

object TestDatabase {
    fun create(path: Path): ConnectionProvider {
        Class.forName("org.h2.Driver")
        val url = "jdbc:h2:file:${path.toAbsolutePath().toString().replace('\\', '/')};TRACE_LEVEL_FILE=0"
        DriverManager.getConnection(url, "sa", "").use { initialize(it) }
        return H2ConnectionProvider(path.toString())
    }

    fun initialize(connection: Connection) {
        val root = Paths.get(System.getProperty("project.root", ".")).toAbsolutePath()
        for (name in listOf("init.sql", "fill.sql")) {
            val script = root.resolve("scripts").resolve(name).toString().replace('\\', '/').replace("'", "''")
            connection.createStatement().use { it.execute("RUNSCRIPT FROM '$script' CHARSET 'UTF-8'") }
        }
    }
}
