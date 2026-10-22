package domain.model

data class User(val salt: String, val passwordHash: String, val permissions: Map<String, Set<Action>>)

enum class Action { READ, WRITE, EXECUTE }

data class AccessRequest(
    val login: String,
    val password: String,
    val action: String,
    val resource: String,
    val volume: String
)

enum class AccessCheckResult {
    SUCCESS, INVALID_PASSWORD, INVALID_LOGIN, UNKNOWN_ACTION, ACCESS_DENIED,
    RESOURCE_NOT_FOUND, INVALID_RESOURCE_OR_VOLUME, VOLUME_LIMIT_EXCEEDED
}
