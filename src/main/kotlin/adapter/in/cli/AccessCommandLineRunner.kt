package adapter.`in`.cli

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.ExitCodeGenerator
import org.springframework.stereotype.Component

@Component
class AccessCommandLineRunner @Autowired constructor(private val cli: CliApplication) :
    CommandLineRunner, ExitCodeGenerator {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var code = 0

    override fun run(vararg args: String) {
        code = cli.run(args)
        logger.info("CLI completed with exit code {}", code)
    }

    override fun getExitCode() = code
}
