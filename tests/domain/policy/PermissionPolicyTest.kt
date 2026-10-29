package tests.domain.policy

import domain.model.Action
import domain.model.User
import domain.policy.PrefixPermissionPolicy
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PermissionPolicyTest {
    private val policy = PrefixPermissionPolicy()
    private fun user(vararg permissions: Pair<String, Set<Action>>) = User("salt", "hash", mapOf(*permissions))

    @ParameterizedTest
    @ValueSource(strings = ["A", "A.B", "A.B.C", "A.B.C.D"])
    fun shouldInheritPermissionFromEachAncestor(resource: String) {
        assertTrue(policy.isAllowed(user("A" to setOf(Action.READ)), resource, Action.READ))
    }

    @Test
    fun shouldAllowPermissionGrantedAtIntermediateLevel() {
        assertTrue(policy.isAllowed(user("A.B" to setOf(Action.WRITE)), "A.B.C", Action.WRITE))
    }

    @ParameterizedTest
    @ValueSource(strings = ["AB", "A1", "B.A", "a", "A_B"])
    fun shouldNotTreatTextPrefixAsPathAncestor(resource: String) {
        assertFalse(policy.isAllowed(user("A" to setOf(Action.READ)), resource, Action.READ))
    }

    @Test
    fun shouldNotGrantChildPermissionToParentOrSibling() {
        val user = user("A.B.C" to setOf(Action.READ))
        assertFalse(policy.isAllowed(user, "A.B", Action.READ))
        assertFalse(policy.isAllowed(user, "A.B.D", Action.READ))
    }

    @Test
    fun shouldCheckRequestedActionIndependently() {
        assertFalse(policy.isAllowed(user("A" to setOf(Action.READ)), "A.B", Action.WRITE))
    }

    @Test
    fun shouldDenyAccessWhenNoPermissionsExist() {
        assertFalse(policy.isAllowed(user(), "A", Action.READ))
    }

    @Test
    fun shouldKeepInheritedGrantWhenChildHasEmptyPermissionSet() {
        val user = user("A" to setOf(Action.READ), "A.B" to emptySet())
        assertTrue(policy.isAllowed(user, "A.B.C", Action.READ))
    }
}
