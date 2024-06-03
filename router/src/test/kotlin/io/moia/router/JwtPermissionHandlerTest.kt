/*
 * Copyright 2019 MOIA GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package io.moia.router

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test

@Suppress("ktlint:standard:max-line-length")
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
        val handler =
            JwtPermissionHandler(
                accessor =
                    JwtAccessor(
                        APIGatewayProxyRequestEvent()
                            .withHeader("Authorization", jwtWithCustomClaimAndSeparator),
                    ),
                permissionsClaim = "userRights",
                permissionSeparator = ",",
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
        JwtPermissionHandler(
            JwtAccessor(
                APIGatewayProxyRequestEvent()
                    .withHeader("Authorization", authHeader),
            ),
        )
}
