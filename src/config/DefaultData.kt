package config

import domain.model.Action
import domain.model.User

object DefaultData {
    val users = mapOf(
        "alice" to User(
            salt = "alice-salt-2026",
            passwordHash = "e91c00f48dd574a479152d06a37b919eff4669324e080aa7fc90de3a94e722b6",
            permissions = mapOf("A" to setOf(Action.READ), "A.B.C" to setOf(Action.WRITE, Action.EXECUTE))
        ),
        "bob" to User(
            salt = "bob-salt-2026",
            passwordHash = "4316aa71d2a86d3572a169b043e7a1a18d2909b73f0d92756722f82d9e408a93",
            permissions = mapOf("A.A8B" to setOf(Action.READ, Action.WRITE))
        )
    )

    val resources = mapOf(
        "A" to 100,
        "A.B" to 50,
        "A.B.C" to 20,
        "A.A8B" to 40,
        "A.A8B.C" to 30,
        "A.A8B.C.f_d" to 10
    )
}
