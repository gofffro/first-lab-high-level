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

private fun AccessCheckResult.exitCode(): Int = when (this) {
    AccessCheckResult.SUCCESS -> SUCCESS
    AccessCheckResult.INVALID_PASSWORD -> INVALID_PASSWORD
    AccessCheckResult.INVALID_LOGIN -> INVALID_LOGIN
    AccessCheckResult.UNKNOWN_ACTION -> UNKNOWN_ACTION
    AccessCheckResult.ACCESS_DENIED -> ACCESS_DENIED
    AccessCheckResult.RESOURCE_NOT_FOUND -> RESOURCE_NOT_FOUND
    AccessCheckResult.INVALID_RESOURCE_OR_VOLUME -> INVALID_RESOURCE_OR_VOLUME
    AccessCheckResult.VOLUME_LIMIT_EXCEEDED -> VOLUME_LIMIT_EXCEEDED
}

internal fun run(
    args: Array<String>,
    users: Map<String, User> = defaultUsers,
    resources: Map<String, Int> = defaultResources
): Int {
    if (args.any { it == "-h" || it == "--help" }) {
        printHelp()
        return HELP_REQUESTED
    }

    val parser = ArgParser("app")
    val login by parser.option(ArgType.String, fullName = "login", description = "User login")
    val password by parser.option(ArgType.String, fullName = "password", description = "User password")
    val action by parser.option(ArgType.String, fullName = "action", description = "Requested action")
    val resource by parser.option(ArgType.String, fullName = "resource", description = "Resource path")
    val volume by parser.option(ArgType.String, fullName = "volume", description = "Requested volume")

    try {
        parser.parse(args)
    } catch (_: ParsingException) {
        printHelp()
        return INVALID_RESOURCE_OR_VOLUME
    }

    val request = AccessRequest(
        login = login ?: return missingArgument(),
        password = password ?: return missingArgument(),
        action = action ?: return missingArgument(),
        resource = resource ?: return missingArgument(),
        volume = volume ?: return missingArgument()
    )

    return checkAccess(request, users, resources).exitCode()
}

private fun missingArgument(): Int {
    printHelp()
    return INVALID_RESOURCE_OR_VOLUME
}

fun main(args: Array<String>) {
    exitProcess(run(args))
}
