import java.security.MessageDigest
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ParsingException
import kotlin.system.exitProcess

private const val SUCCESS = 0
private const val HELP_REQUESTED = 1
private const val INVALID_PASSWORD = 2
private const val INVALID_LOGIN = 3
private const val UNKNOWN_ACTION = 4
private const val ACCESS_DENIED = 5
private const val RESOURCE_NOT_FOUND = 6
private const val INVALID_RESOURCE_OR_VOLUME = 7
private const val VOLUME_LIMIT_EXCEEDED = 8

private data class User(
    val salt: String,
    val passwordHash: String,
    val permissions: Map<String, Set<Action>>
)

private enum class Action {
    READ,
    WRITE,
    EXECUTE
}

private val users = mapOf(
    "alice" to User(
        salt = "alice-salt-2026",
        passwordHash = "e91c00f48dd574a479152d06a37b919eff4669324e080aa7fc90de3a94e722b6",
        permissions = mapOf(
            "A" to setOf(Action.READ),
            "A.B.C" to setOf(Action.WRITE, Action.EXECUTE)
        )
    ),
    "bob" to User(
        salt = "bob-salt-2026",
        passwordHash = "4316aa71d2a86d3572a169b043e7a1a18d2909b73f0d92756722f82d9e408a93",
        permissions = mapOf(
            "A.A8B" to setOf(Action.READ, Action.WRITE)
        )
    )
)

private val resources = mapOf(
    "A" to 100,
    "A.B" to 50,
    "A.B.C" to 20,
    "A.A8B" to 40,
    "A.A8B.C" to 30,
    "A.A8B.C.f_d" to 10
)

private val resourcePathPattern = Regex("^[A-Za-z0-9_]{1,20}(\\.[A-Za-z0-9_]{1,20})*$")

private fun hashPassword(password: String, salt: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest((salt + password).toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}

private fun hasPermission(user: User, resource: String, action: Action): Boolean {
    val parts = resource.split('.')
    return parts.indices
        .map { parts.take(it + 1).joinToString(".") }
        .any { action in user.permissions[it].orEmpty() }
}

private fun parseAction(action: String): Action? = when (action.lowercase()) {
    "read" -> Action.READ
    "write" -> Action.WRITE
    "execute" -> Action.EXECUTE
    else -> null
}

private fun printHelp() {
    println(
        """
        Usage: java -jar app.jar --login LOGIN --password PASSWORD --action ACTION --resource PATH --volume VOLUME

        Options:
          --login LOGIN        User login
          --password PASSWORD  User password
          --action ACTION      read, write or execute
          --resource PATH      Resource path, for example A.A8B.C.f_d
          --volume VOLUME      Requested non-negative integer volume
          -h, --help           Show this help
        """.trimIndent()
    )
}

private fun run(args: Array<String>): Int {
    if (args.any { it == "-h" || it == "--help" }) {
        printHelp()
        return HELP_REQUESTED
    }

    val parser = ArgParser("app")
    val login by parser.option(ArgType.String, fullName = "login", description = "User login")
    val password by parser.option(ArgType.String, fullName = "password", description = "User password")
    val actionValue by parser.option(ArgType.String, fullName = "action", description = "Requested action")
    val resource by parser.option(ArgType.String, fullName = "resource", description = "Resource path")
    val volumeValue by parser.option(ArgType.String, fullName = "volume", description = "Requested volume")

    try {
        parser.parse(args)
    } catch (_: ParsingException) {
        printHelp()
        return INVALID_RESOURCE_OR_VOLUME
    }

    val requestLogin = login ?: run {
        printHelp()
        return INVALID_RESOURCE_OR_VOLUME
    }
    val requestPassword = password ?: run {
        printHelp()
        return INVALID_RESOURCE_OR_VOLUME
    }
    val requestAction = actionValue ?: run {
        printHelp()
        return INVALID_RESOURCE_OR_VOLUME
    }
    val requestResource = resource ?: run {
        printHelp()
        return INVALID_RESOURCE_OR_VOLUME
    }
    val requestVolume = volumeValue ?: run {
        printHelp()
        return INVALID_RESOURCE_OR_VOLUME
    }

    val user = users[requestLogin] ?: return INVALID_LOGIN
    if (hashPassword(requestPassword, user.salt) != user.passwordHash) {
        return INVALID_PASSWORD
    }

    val action = parseAction(requestAction) ?: return UNKNOWN_ACTION
    if (!resourcePathPattern.matches(requestResource)) {
        return INVALID_RESOURCE_OR_VOLUME
    }

    val volume = requestVolume.toIntOrNull() ?: return INVALID_RESOURCE_OR_VOLUME
    if (volume < 0) {
        return INVALID_RESOURCE_OR_VOLUME
    }

    val maximumVolume = resources[requestResource] ?: return RESOURCE_NOT_FOUND
    if (!hasPermission(user, requestResource, action)) {
        return ACCESS_DENIED
    }

    return if (volume > maximumVolume) VOLUME_LIMIT_EXCEEDED else SUCCESS
}

fun main(args: Array<String>) {
    exitProcess(run(args))
}
