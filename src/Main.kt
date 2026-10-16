import adapter.`in`.cli.*
import application.port.`in`.CheckAccessUseCase
import config.CompositionRoot
import kotlin.system.exitProcess

internal fun run(args: Array<String>, useCase: CheckAccessUseCase = CompositionRoot.createUseCase()): Int {
    return when (val parsed = CliParser().parse(args)) {
        CliParseResult.Help -> { HelpPrinter.print(); CliResultPresenter.HELP_REQUESTED }
        CliParseResult.Invalid -> { HelpPrinter.print(); CliResultPresenter.INVALID_RESOURCE_OR_VOLUME }
        is CliParseResult.Success -> CliResultPresenter.exitCode(CliController(useCase).execute(parsed.command))
    }
}

fun main(args: Array<String>) {
    exitProcess(run(args))
}
