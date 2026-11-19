package adapter.`in`.cli

import application.port.`in`.CheckAccessUseCase
import domain.model.AccessCheckResult
import domain.model.AccessRequest
import org.springframework.stereotype.Component

sealed interface CliParseResult {
    object Help : CliParseResult
    data class Success(val command: CheckAccessCommand) : CliParseResult
    object Invalid : CliParseResult
    object SyntaxError : CliParseResult
}

data class CheckAccessCommand(
    val login: String,
    val password: String,
    val action: String,
    val resource: String,
    val volume: String
)

@Component
class CliParser {
    private val options = setOf("--login", "--password", "--action", "--resource", "--volume")

    fun parse(args: Array<out String>): CliParseResult {
        if (args.any { it == "-h" || it == "--help" }) return CliParseResult.Help
        val values = mutableMapOf<String, String>()
        for (index in args.indices step 2) {
            val option = args[index]
            val value = args.getOrNull(index + 1) ?: return CliParseResult.SyntaxError
            if (option !in options || values.putIfAbsent(option, value) != null) {
                return CliParseResult.SyntaxError
            }
        }
        if (values.size != options.size) return CliParseResult.Invalid
        return CliParseResult.Success(CheckAccessCommand(
            values.getValue("--login"), values.getValue("--password"), values.getValue("--action"),
            values.getValue("--resource"), values.getValue("--volume")
        ))
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

@Component
class CliController(private val useCase: CheckAccessUseCase) {
    fun execute(command: CheckAccessCommand): AccessCheckResult = useCase.execute(
        AccessRequest(command.login, command.password, command.action, command.resource, command.volume)
    )
}

@Component
class CliApplication(private val parser: CliParser, private val controller: CliController) {
    fun run(args: Array<out String>, output: (String) -> Unit = ::println): Int =
        when (val parsed = parser.parse(args)) {
            CliParseResult.Help -> {
                HelpPrinter.print(output)
                CliResultPresenter.HELP_REQUESTED
            }
            CliParseResult.Invalid -> {
                HelpPrinter.print(output)
                CliResultPresenter.INVALID_RESOURCE_OR_VOLUME
            }
            CliParseResult.SyntaxError -> CliResultPresenter.SYNTAX_ERROR
            is CliParseResult.Success -> CliResultPresenter.exitCode(controller.execute(parsed.command))
        }
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
    const val DATABASE_CONNECTION_ERROR = 9
    const val DATABASE_QUERY_ERROR = 10
    const val SYNTAX_ERROR = 127

    fun exitCode(result: AccessCheckResult): Int = when (result) {
        AccessCheckResult.SUCCESS -> SUCCESS
        AccessCheckResult.INVALID_PASSWORD -> INVALID_PASSWORD
        AccessCheckResult.INVALID_LOGIN -> INVALID_LOGIN
        AccessCheckResult.UNKNOWN_ACTION -> UNKNOWN_ACTION
        AccessCheckResult.ACCESS_DENIED -> ACCESS_DENIED
        AccessCheckResult.RESOURCE_NOT_FOUND -> RESOURCE_NOT_FOUND
        AccessCheckResult.INVALID_RESOURCE_OR_VOLUME -> INVALID_RESOURCE_OR_VOLUME
        AccessCheckResult.VOLUME_LIMIT_EXCEEDED -> VOLUME_LIMIT_EXCEEDED
        AccessCheckResult.DATABASE_CONNECTION_ERROR -> DATABASE_CONNECTION_ERROR
        AccessCheckResult.DATABASE_QUERY_ERROR -> DATABASE_QUERY_ERROR
    }
}
