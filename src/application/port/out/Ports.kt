package application.port.out

import domain.model.User

fun interface UserRepository { fun findByLogin(login: String): User? }
fun interface ResourceRepository { fun findMaximumVolume(path: String): Int? }
fun interface PasswordHasher { fun hash(password: String, salt: String): String }
