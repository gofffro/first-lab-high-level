package domain.policy

import domain.model.Action
import domain.model.User

fun interface ActionParser { fun parse(rawAction: String): Action? }

class DefaultActionParser : ActionParser {
    private val actions = mapOf("read" to Action.READ, "write" to Action.WRITE, "execute" to Action.EXECUTE)
    override fun parse(rawAction: String): Action? = actions[rawAction.lowercase()]
}

fun interface ResourcePathValidator { fun isValid(path: String): Boolean }

class RegexResourcePathValidator(
    private val pattern: Regex = Regex("^[A-Za-z0-9_]{1,20}(\\.[A-Za-z0-9_]{1,20})*$")
) : ResourcePathValidator {
    override fun isValid(path: String): Boolean = pattern.matches(path)
}

fun interface VolumeValidator { fun parse(rawVolume: String): Int? }

class NonNegativeVolumeValidator : VolumeValidator {
    override fun parse(rawVolume: String): Int? = rawVolume.toIntOrNull()?.takeIf { it >= 0 }
}

fun interface PermissionPolicy { fun isAllowed(user: User, resource: String, action: Action): Boolean }

class PrefixPermissionPolicy : PermissionPolicy {
    override fun isAllowed(user: User, resource: String, action: Action): Boolean {
        val parts = resource.split('.')
        return parts.indices.any { action in user.permissions[parts.take(it + 1).joinToString(".")].orEmpty() }
    }
}
