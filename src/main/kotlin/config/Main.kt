package config

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import kotlin.system.exitProcess

@SpringBootApplication(proxyBeanMethods = false, scanBasePackages = ["config", "adapter", "application"])
class AccessApplication

fun main(args: Array<String>) {
    val application = SpringApplication(AccessApplication::class.java).apply {
        setAddCommandLineProperties(false)
    }
    val code = try {
        val context = application.run(*args)
        SpringApplication.exit(context)
    } catch (_: Exception) {
        // Spring Boot reports startup failures through the file logger.
        11
    }
    exitProcess(code)
}
