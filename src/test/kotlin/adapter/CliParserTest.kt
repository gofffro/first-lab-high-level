package tests.adapter

import adapter.`in`.cli.CheckAccessCommand
import adapter.`in`.cli.CliParseResult
import adapter.`in`.cli.CliParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class CliParserTest {
    private val parser = CliParser()
    private fun options() = linkedMapOf("login" to "alice", "password" to "qwerty", "action" to "read", "resource" to "A.B.C", "volume" to "10")
    private fun arguments(options: Map<String, String>) = options.flatMap { (key, value) -> listOf("--$key", value) }.toTypedArray()

    @Test
    fun shouldMapAllOptionsToCommandRegardlessOfOrder() {
        val args = arguments(options().toList().reversed().toMap())
        assertEquals(CliParseResult.Success(CheckAccessCommand("alice", "qwerty", "read", "A.B.C", "10")), parser.parse(args))
    }

    @ParameterizedTest
    @ValueSource(strings = ["login", "password", "action", "resource", "volume"])
    fun shouldRejectEachMissingRequiredOption(option: String) {
        val options = options().apply { remove(option) }
        assertSame(CliParseResult.Invalid, parser.parse(arguments(options)))
    }

    @ParameterizedTest
    @ValueSource(strings = ["-h", "--help"])
    fun shouldGiveHelpPriorityOverOtherInvalidOptions(help: String) {
        assertSame(CliParseResult.Help, parser.parse(arrayOf("--unknown", help)))
    }

    @Test
    fun shouldRejectEmptyArgumentList() {
        assertSame(CliParseResult.Invalid, parser.parse(emptyArray()))
    }

    @Test
    fun shouldLeaveBusinessValidationToUseCase() {
        val command = CheckAccessCommand("alice", "qwerty", "remove", "A..B", "none")
        val args = arguments(options().apply { put("action", command.action); put("resource", command.resource); put("volume", command.volume) })
        assertEquals(CliParseResult.Success(command), parser.parse(args))
    }

    @Test
    fun shouldNotRetainOptionsBetweenParseCalls() {
        parser.parse(arguments(options()))
        assertSame(CliParseResult.Invalid, parser.parse(emptyArray()))
    }
}
