package adapter.`in`.cli

import application.port.`in`.CheckAccessUseCase
import domain.model.AccessCheckResult
import domain.model.AccessRequest
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ParsingException

sealed interface CliParseResult {
    object Help : CliParseResult
    data class Success(val command: CheckAccessCommand) : CliParseResult
    object Invalid : CliParseResult
}

data class CheckAccessCommand(
    val login: String,
    val password: String,
    val action: String,
    val resource: String,
    val volume: String
)

class CliParser {
    fun parse(args: Array<String>): CliParseResult {
        if (args.any { it == "-h" || it == "--help" }) return CliParseResult.Help
        val parser = ArgParser("app")
        val login by parser.option(ArgType.String, fullName = "login")
        val password by parser.option(ArgType.String, fullName = "password")
        val action by parser.option(ArgType.String, fullName = "action")
        val resource by parser.option(ArgType.String, fullName = "resource")
        val volume by parser.option(ArgType.String, fullName = "volume")
        try { parser.parse(args) } catch (_: ParsingException) { return CliParseResult.Invalid }
        if (login == null || password == null || action == null || resource == null || volume == null) return CliParseResult.Invalid
        return CliParseResult.Success(CheckAccessCommand(login!!, password!!, action!!, resource!!, volume!!))
    }
}

object HelpPrinter {
    fun print(output: (String) -> Unit = ::println) = output("""
Usage: java -jar app.jar --login LOGIN --password PASSWORD --action ACTION --resource PATH --volume VOLUME

Options:
  --login LOGIN        User login
  --password PASSWORD  User password
  --action ACTION      read, write or execute
  --resource PATH      Resource path, for example A.A8B.C.f_d
  --volume VOLUME      Requested non-negative integer volume
  -h, --help           Show this help
""".trimIndent())
}

class CliController(private val useCase: CheckAccessUseCase) {
    fun execute(command: CheckAccessCommand): AccessCheckResult = useCase.execute(
        AccessRequest(command.login, command.password, command.action, command.resource, command.volume)
    )
}

object CliResultPresenter {
    const val SUCCESS = 0
    const val HELP_REQUESTED = 1
    const val INVALID_PASSWORD = 2
    const val INVALID_LOGIN = 3
    const val UNKNOWN_ACTION = 4
    const val ACCESS_DENIED = 5
    const val RESOURCE_NOT_FOUND = 6
    const val INVALID_RESOURCE_OR_VOLUME = 7
    const val VOLUME_LIMIT_EXCEEDED = 8

    fun exitCode(result: AccessCheckResult): Int = when (result) {
        AccessCheckResult.SUCCESS -> SUCCESS
        AccessCheckResult.INVALID_PASSWORD -> INVALID_PASSWORD
        AccessCheckResult.INVALID_LOGIN -> INVALID_LOGIN
        AccessCheckResult.UNKNOWN_ACTION -> UNKNOWN_ACTION
        AccessCheckResult.ACCESS_DENIED -> ACCESS_DENIED
        AccessCheckResult.RESOURCE_NOT_FOUND -> RESOURCE_NOT_FOUND
        AccessCheckResult.INVALID_RESOURCE_OR_VOLUME -> INVALID_RESOURCE_OR_VOLUME
        AccessCheckResult.VOLUME_LIMIT_EXCEEDED -> VOLUME_LIMIT_EXCEEDED
    }
}
