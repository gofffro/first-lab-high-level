package adapter.out.memory

import application.port.out.ResourceRepository
import application.port.out.UserRepository
import domain.model.User

class InMemoryUserRepository(private val users: Map<String, User>) : UserRepository {
    override fun findByLogin(login: String): User? = users[login]
}

class InMemoryResourceRepository(private val resources: Map<String, Int>) : ResourceRepository {
    override fun findMaximumVolume(path: String): Int? = resources[path]
}
