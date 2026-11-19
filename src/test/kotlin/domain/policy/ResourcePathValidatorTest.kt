package tests.domain.policy

import domain.policy.RegexResourcePathValidator
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EmptySource
import org.junit.jupiter.params.provider.ValueSource

class ResourcePathValidatorTest {
    private val validator = RegexResourcePathValidator()

    @ParameterizedTest
    @ValueSource(strings = ["A", "A.B.C", "A.A8B.C.f_d", "_", "123", "a_b.C9", "abcdefghijklmnopqrst"])
    fun shouldAcceptValidResourcePaths(path: String) {
        assertTrue(validator.isValid(path))
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = [".A", "A.", "A..B", "A/B", "A\\B", "A B", " A", "A ", "А.Б", "A-B", "A\n", "abcdefghijklmnopqrstu", "A.abcdefghijklmnopqrstu"])
    fun shouldRejectInvalidResourcePaths(path: String) {
        assertFalse(validator.isValid(path))
    }

    @Test
    fun shouldApplySegmentLengthLimitToEachLevel() {
        assertTrue(validator.isValid("a".repeat(20) + "." + "b".repeat(20)))
        assertFalse(validator.isValid("a".repeat(20) + "." + "b".repeat(21)))
    }
}
