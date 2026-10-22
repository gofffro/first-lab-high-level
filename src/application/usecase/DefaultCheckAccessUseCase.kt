package application.usecase

import application.port.`in`.CheckAccessUseCase
import application.port.out.PasswordHasher
import application.port.out.ResourceRepository
import application.port.out.UserRepository
import domain.model.AccessCheckResult
import domain.model.AccessRequest
import domain.policy.*

class DefaultCheckAccessUseCase(
    private val users: UserRepository,
    private val resources: ResourceRepository,
    private val passwordHasher: PasswordHasher,
    private val actionParser: ActionParser,
    private val resourcePathValidator: ResourcePathValidator,
    private val permissionPolicy: PermissionPolicy,
    private val volumeValidator: VolumeValidator = NonNegativeVolumeValidator()
) : CheckAccessUseCase {
    override fun execute(request: AccessRequest): AccessCheckResult {
        val user = users.findByLogin(request.login) ?: return AccessCheckResult.INVALID_LOGIN
        if (passwordHasher.hash(request.password, user.salt) != user.passwordHash) {
            return AccessCheckResult.INVALID_PASSWORD
        }
        val action = actionParser.parse(request.action) ?: return AccessCheckResult.UNKNOWN_ACTION
        if (!resourcePathValidator.isValid(request.resource)) {
            return AccessCheckResult.INVALID_RESOURCE_OR_VOLUME
        }
        val volume = volumeValidator.parse(request.volume) ?: return AccessCheckResult.INVALID_RESOURCE_OR_VOLUME
        val maximumVolume = resources.findMaximumVolume(request.resource)
            ?: return AccessCheckResult.RESOURCE_NOT_FOUND
        if (!permissionPolicy.isAllowed(user, request.resource, action)) {
            return AccessCheckResult.ACCESS_DENIED
        }
        return if (volume > maximumVolume) AccessCheckResult.VOLUME_LIMIT_EXCEEDED else AccessCheckResult.SUCCESS
    }
}
