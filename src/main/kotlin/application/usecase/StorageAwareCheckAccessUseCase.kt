package application.usecase

import application.port.`in`.CheckAccessUseCase
import application.port.out.StorageConnectionException
import application.port.out.StorageQueryException
import domain.model.AccessCheckResult
import domain.model.AccessRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class StorageAwareCheckAccessUseCase(
    @Qualifier("defaultCheckAccessUseCase") private val delegate: CheckAccessUseCase
) : CheckAccessUseCase {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun execute(request: AccessRequest): AccessCheckResult = try {
        delegate.execute(request)
    } catch (_: StorageConnectionException) {
        logger.warn("Database connection failed")
        AccessCheckResult.DATABASE_CONNECTION_ERROR
    } catch (_: StorageQueryException) {
        logger.warn("Database query failed")
        AccessCheckResult.DATABASE_QUERY_ERROR
    }
}
