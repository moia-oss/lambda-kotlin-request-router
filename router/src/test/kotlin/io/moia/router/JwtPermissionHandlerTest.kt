package io.moia.router

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test

class JwtPermissionHandlerTest {

    /*
    {
        "sub": "1234567890",
        "name": "John Doe",
        "iat": 1516239022,
        "scope": "one two"
    }
    */
    val jwtWithScopeClaimSpace = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJzY29wZSI6Im9uZSB0d28ifQ.2tPrDymXDejHfVjNlVh4XUj22ZuDrKHP6dvWN7JNAWY"
    /*
    {
      "sub": "1234567890",
      "name": "John Doe",
      "iat": 1516239022,
      "userRights": "one, two"
    }
     */
    val jwtWithCustomClaimAndSeparator = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJ1c2VyUmlnaHRzIjoib25lLCB0d28ifQ.49yk0fq39zMF77ZLJsXH_6D6I3iSDpy-Qk3vZ_PssIY"

    @Test
    fun `should extract permissions from standard JWT contained in bearer auth header`() {
        val handler = permissionHandler("Bearer $jwtWithScopeClaimSpace")

        thenRecognizesRequiredPermissions(handler)
    }

    @Test
    fun `should extract permissions from standard JWT contained in auth header`() {
        val handler = permissionHandler(jwtWithScopeClaimSpace)

        thenRecognizesRequiredPermissions(handler)
    }

    @Test
    fun `should extract permissions from custom permissions claim`() {
        val handler = JwtPermissionHandler(
            accessor = JwtAccessor(APIGatewayProxyRequestEvent()
                .withHeader("Authorization", jwtWithCustomClaimAndSeparator)),
            permissionsClaim = "userRights",
            permissionSeparator = ","
        )

        thenRecognizesRequiredPermissions(handler)
    }

    @Test
    fun `should return true when no permissions are required`() {
        val handler = permissionHandler(jwtWithScopeClaimSpace)

        val result = handler.hasAnyRequiredPermission(emptySet())

        then(result).isTrue()
    }

    @Test
    fun `should work for missing header`() {
        val handler = JwtPermissionHandler(JwtAccessor(APIGatewayProxyRequestEvent()))

        then(handler.extractPermissions()).isEmpty()
    }

    @Test
    fun `should work for not jwt auth header`() {
        val handler = permissionHandler("a.b.c")

        then(handler.extractPermissions()).isEmpty()
    }

    private fun thenRecognizesRequiredPermissions(handler: JwtPermissionHandler) {
        then(handler.hasAnyRequiredPermission(setOf("one"))).isTrue()
        then(handler.hasAnyRequiredPermission(setOf("two"))).isTrue()
        then(handler.hasAnyRequiredPermission(setOf("nope"))).isFalse()
    }

    private fun permissionHandler(authHeader: String) =
        JwtPermissionHandler(JwtAccessor(APIGatewayProxyRequestEvent()
            .withHeader("Authorization", authHeader)))
}