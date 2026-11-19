package application.port.`in`

import domain.model.AccessCheckResult
import domain.model.AccessRequest

fun interface CheckAccessUseCase { fun execute(request: AccessRequest): AccessCheckResult }
