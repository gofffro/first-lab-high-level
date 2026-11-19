package config

import adapter.out.jdbc.ConnectionProvider
import adapter.out.jdbc.H2ConnectionProvider
import domain.policy.DefaultActionParser
import domain.policy.NonNegativeVolumeValidator
import domain.policy.PrefixPermissionPolicy
import domain.policy.RegexResourcePathValidator
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class AccessConfiguration {
    @Bean
    fun connectionProvider(@Value("\${app.database.path}") path: String): ConnectionProvider = H2ConnectionProvider(path)

    @Bean
    fun actionParser() = DefaultActionParser()

    @Bean
    fun resourcePathValidator() = RegexResourcePathValidator()

    @Bean
    fun volumeValidator() = NonNegativeVolumeValidator()

    @Bean
    fun permissionPolicy() = PrefixPermissionPolicy()
}
