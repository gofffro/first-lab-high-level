package config

import adapter.out.memory.*
import adapter.out.security.Sha256PasswordHasher
import application.port.`in`.CheckAccessUseCase
import application.usecase.DefaultCheckAccessUseCase
import domain.policy.*

object CompositionRoot {
    fun createUseCase(): CheckAccessUseCase = DefaultCheckAccessUseCase(
        users = InMemoryUserRepository(DefaultData.users),
        resources = InMemoryResourceRepository(DefaultData.resources),
        passwordHasher = Sha256PasswordHasher(),
        actionParser = DefaultActionParser(),
        resourcePathValidator = RegexResourcePathValidator(),
        permissionPolicy = PrefixPermissionPolicy(),
        volumeValidator = NonNegativeVolumeValidator()
    )
}
