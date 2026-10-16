package adapter.out.security

import application.port.out.PasswordHasher
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class Sha256PasswordHasher : PasswordHasher {
    override fun hash(password: String, salt: String): String = MessageDigest.getInstance("SHA-256")
        .digest((salt + password).toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
