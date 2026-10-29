package tests.adapter

import adapter.out.security.Sha256PasswordHasher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class PasswordHasherTest {
    private val hasher = Sha256PasswordHasher()

    @Test
    fun shouldHashEmptyPasswordAndSaltUsingKnownSha256Vector() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hasher.hash("", ""))
    }

    @Test
    fun shouldPutSaltBeforePasswordUsingKnownAbcVector() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hasher.hash("bc", "a"))
    }

    @Test
    fun shouldMatchExistingAliceHash() {
        assertEquals("e91c00f48dd574a479152d06a37b919eff4669324e080aa7fc90de3a94e722b6", hasher.hash("qwerty", "alice-salt-2026"))
    }

    @Test
    fun shouldHashNonAsciiPasswordAndSaltAsUtf8() {
        // Independent vector computed with .NET SHA256 over UTF-8 bytes of "сольпароль".
        assertEquals("9bf4d5b964c91231ad771f8f83241fc6e65cce0a85d69168e786fd64c635e304", hasher.hash("пароль", "соль"))
    }

    @Test
    fun shouldChangeHashWhenSaltOrPasswordChanges() {
        val original = hasher.hash("password", "salt")
        assertNotEquals(original, hasher.hash("other", "salt"))
        assertNotEquals(original, hasher.hash("password", "other"))
        assertEquals(original, hasher.hash("password", "salt"))
    }
}
