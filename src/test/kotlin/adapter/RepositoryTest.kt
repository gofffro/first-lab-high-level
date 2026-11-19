package tests.adapter

import adapter.out.memory.InMemoryResourceRepository
import adapter.out.memory.InMemoryUserRepository
import domain.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RepositoryTest {
    @Test
    fun shouldReturnUserByExactLogin() {
        val user = User("salt", "hash", emptyMap())
        val repository = InMemoryUserRepository(mapOf("alice" to user))
        assertEquals(user, repository.findByLogin("alice"))
        assertNull(repository.findByLogin("Alice"))
    }

    @Test
    fun shouldReturnNullForUnknownUser() {
        assertNull(InMemoryUserRepository(emptyMap()).findByLogin("missing"))
    }

    @Test
    fun shouldReturnZeroVolumeWithoutTreatingItAsMissing() {
        assertEquals(0, InMemoryResourceRepository(mapOf("A" to 0)).findMaximumVolume("A"))
    }

    @Test
    fun shouldReturnExactResourceLimitWithoutInheritingParentLimit() {
        val repository = InMemoryResourceRepository(mapOf("A" to 100, "A.B" to 20))
        assertEquals(20, repository.findMaximumVolume("A.B"))
        assertNull(repository.findMaximumVolume("A.B.C"))
        assertNull(repository.findMaximumVolume("a"))
    }

    @Test
    fun shouldReturnNullForUnknownResource() {
        assertNull(InMemoryResourceRepository(emptyMap()).findMaximumVolume("missing"))
    }
}
