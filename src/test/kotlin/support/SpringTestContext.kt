package tests.support

import adapter.out.jdbc.ConnectionProvider
import application.port.`in`.CheckAccessUseCase
import config.AccessConfiguration
import org.springframework.beans.factory.config.BeanDefinitionCustomizer
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import java.util.function.Supplier

fun <T> withAccessUseCase(connections: ConnectionProvider, test: (CheckAccessUseCase) -> T): T =
    AnnotationConfigApplicationContext().use { context ->
        context.registerBean(
            "testConnections", ConnectionProvider::class.java, Supplier { connections },
            BeanDefinitionCustomizer { it.isPrimary = true }
        )
        context.register(AccessConfiguration::class.java)
        context.scan("adapter", "application")
        context.refresh()
        test(context.getBean(CheckAccessUseCase::class.java))
    }
