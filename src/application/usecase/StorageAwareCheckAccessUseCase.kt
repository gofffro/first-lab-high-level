package application.usecase

import application.port.`in`.CheckAccessUseCase
import application.port.out.StorageConnectionException
import application.port.out.StorageQueryException
import domain.model.AccessCheckResult
import domain.model.AccessRequest

class StorageAwareCheckAccessUseCase(private val delegate: CheckAccessUseCase) : CheckAccessUseCase {
    override fun execute(request: AccessRequest): AccessCheckResult = try {
        delegate.execute(request)
    } catch (_: StorageConnectionException) {
        AccessCheckResult.DATABASE_CONNECTION_ERROR
    } catch (_: StorageQueryException) {
        AccessCheckResult.DATABASE_QUERY_ERROR
    }
}
