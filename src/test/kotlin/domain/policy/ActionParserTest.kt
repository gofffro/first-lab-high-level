package tests.domain.policy

import domain.model.Action
import domain.policy.DefaultActionParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EmptySource
import org.junit.jupiter.params.provider.ValueSource

class ActionParserTest {
    private val parser = DefaultActionParser()

    @ParameterizedTest
    @CsvSource("read,READ", "write,WRITE", "execute,EXECUTE", "READ,READ", "WrItE,WRITE", "eXeCuTe,EXECUTE")
    fun shouldParseSupportedActionsIgnoringCase(raw: String, expected: Action) {
        assertEquals(expected, parser.parse(raw))
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = ["remove", "delete", "read ", " read", "read\n", "чтение"])
    fun shouldRejectUnknownActionsWithoutTrimmingInput(raw: String) {
        assertNull(parser.parse(raw))
    }
}
