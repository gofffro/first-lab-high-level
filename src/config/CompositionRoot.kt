package config

import adapter.out.jdbc.ConnectionProvider
import adapter.out.jdbc.H2ConnectionProvider
import adapter.out.jdbc.JdbcResourceRepository
import adapter.out.jdbc.JdbcUserRepository
import adapter.out.security.Sha256PasswordHasher
import application.port.`in`.CheckAccessUseCase
import application.usecase.DefaultCheckAccessUseCase
import application.usecase.StorageAwareCheckAccessUseCase
import domain.policy.*

object CompositionRoot {
    fun createUseCase(
        connections: ConnectionProvider = H2ConnectionProvider(System.getenv("APP_DB_PATH") ?: "data/access")
    ): CheckAccessUseCase = StorageAwareCheckAccessUseCase(DefaultCheckAccessUseCase(
        users = JdbcUserRepository(connections),
        resources = JdbcResourceRepository(connections),
        passwordHasher = Sha256PasswordHasher(),
        actionParser = DefaultActionParser(),
        resourcePathValidator = RegexResourcePathValidator(),
        permissionPolicy = PrefixPermissionPolicy(),
        volumeValidator = NonNegativeVolumeValidator()
    ))
}
