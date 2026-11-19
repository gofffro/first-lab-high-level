package application.port.out

// Storage-neutral failures: the application does not import java.sql or H2.
sealed class StorageException(cause: Throwable) : RuntimeException(cause)
class StorageConnectionException(cause: Throwable) : StorageException(cause)
class StorageQueryException(cause: Throwable) : StorageException(cause)
