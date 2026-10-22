package tests.domain.policy

import domain.policy.NonNegativeVolumeValidator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EmptySource
import org.junit.jupiter.params.provider.ValueSource

class VolumeValidatorTest {
    private val validator = NonNegativeVolumeValidator()

    @ParameterizedTest
    @CsvSource("0,0", "1,1", "20,20", "2147483647,2147483647", "+10,10", "0007,7", "-0,0")
    fun shouldParseNonNegativeIntVolumes(raw: String, expected: Int) {
        assertEquals(expected, validator.parse(raw))
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = ["-1", "-2147483648", "2147483648", "999999999999999999999", "1.5", "1e2", "none", " 1", "1 ", "+", "1\n"])
    fun shouldRejectInvalidNegativeOrOverflowingVolumes(raw: String) {
        assertNull(validator.parse(raw))
    }
}
