import java.security.MessageDigest

private val resourcePathPattern = Regex("^[A-Za-z0-9_]{1,20}(\\.[A-Za-z0-9_]{1,20})*$")

fun checkAccess(
    request: AccessRequest,
    users: Map<String, User>,
    resources: Map<String, Int>,
    passwordHasher: (password: String, salt: String) -> String = ::hashPassword,
    resourcePattern: Regex = resourcePathPattern
): AccessCheckResult {
    val user = users[request.login] ?: return AccessCheckResult.INVALID_LOGIN
    if (passwordHasher(request.password, user.salt) != user.passwordHash) {
        return AccessCheckResult.INVALID_PASSWORD
    }

    val action = parseAction(request.action) ?: return AccessCheckResult.UNKNOWN_ACTION
    if (!resourcePattern.matches(request.resource)) {
        return AccessCheckResult.INVALID_RESOURCE_OR_VOLUME
    }

    val volume = request.volume.toIntOrNull() ?: return AccessCheckResult.INVALID_RESOURCE_OR_VOLUME
    if (volume < 0) {
        return AccessCheckResult.INVALID_RESOURCE_OR_VOLUME
    }

    val maximumVolume = resources[request.resource] ?: return AccessCheckResult.RESOURCE_NOT_FOUND
    if (!hasPermission(user, request.resource, action)) {
        return AccessCheckResult.ACCESS_DENIED
    }

    return if (volume > maximumVolume) {
        AccessCheckResult.VOLUME_LIMIT_EXCEEDED
    } else {
        AccessCheckResult.SUCCESS
    }
}

fun hashPassword(password: String, salt: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest((salt + password).toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}

fun hasPermission(user: User, resource: String, action: Action): Boolean {
    val parts = resource.split('.')
    return parts.indices
        .map { parts.take(it + 1).joinToString(".") }
        .any { action in user.permissions[it].orEmpty() }
}

private fun parseAction(action: String): Action? = when (action.lowercase()) {
    "read" -> Action.READ
    "write" -> Action.WRITE
    "execute" -> Action.EXECUTE
    else -> null
}
