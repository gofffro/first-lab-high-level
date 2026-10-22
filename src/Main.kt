import adapter.`in`.cli.*
import application.port.`in`.CheckAccessUseCase
import config.CompositionRoot
import kotlin.system.exitProcess

internal fun run(
    args: Array<String>,
    useCase: CheckAccessUseCase = CompositionRoot.createUseCase(),
    output: (String) -> Unit = ::println
): Int {
    return when (val parsed = CliParser().parse(args)) {
        CliParseResult.Help -> { HelpPrinter.print(output); CliResultPresenter.HELP_REQUESTED }
        CliParseResult.Invalid -> { HelpPrinter.print(output); CliResultPresenter.INVALID_RESOURCE_OR_VOLUME }
        is CliParseResult.Success -> CliResultPresenter.exitCode(CliController(useCase).execute(parsed.command))
    }
}

fun main(args: Array<String>) {
    exitProcess(run(args))
}
