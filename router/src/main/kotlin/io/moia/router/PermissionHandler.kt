package io.moia.router

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.Base64

interface PermissionHandler {

    fun hasAnyRequiredPermission(requiredPermissions: Set<String>): Boolean
}

class NoOpPermissionHandler : PermissionHandler {
    override fun hasAnyRequiredPermission(requiredPermissions: Set<String>) = true
}

open class JwtAccessor(
    private val request: APIGatewayProxyRequestEvent,
    private val authorizationHeaderName: String = "authorization"
) {

    private val objectMapper = jacksonObjectMapper()

    fun extractJwtToken(): String? =
        // support "Bearer <token>" as well as "<token>"
        request.getHeaderCaseInsensitive(authorizationHeaderName)?.split(" ")?.toList()?.last()

    fun extractJwtClaims() =
        extractJwtToken()
            ?.let { token -> token.split("\\.".toRegex()).dropLastWhile { it.isEmpty() } }
            ?.takeIf { it.size == 3 }
            ?.let { it[1] }
            ?.let { jwtPayload ->
                try {
                    String(Base64.getDecoder().decode(jwtPayload))
                } catch (e: Exception) {
                    return null
                }
            }
            ?.let { objectMapper.readValue<Map<String, Any>>(it) }
}
open class JwtPermissionHandler(
    val accessor: JwtAccessor,
    val permissionsClaim: String = defaultPermissionsClaim,
    val permissionSeparator: String = defaultPermissionSeparator
) : PermissionHandler {

    constructor(
        request: APIGatewayProxyRequestEvent,
        permissionsClaim: String = defaultPermissionsClaim,
        permissionSeparator: String = defaultPermissionSeparator
    ) : this(JwtAccessor(request), permissionsClaim, permissionSeparator)

    override fun hasAnyRequiredPermission(requiredPermissions: Set<String>): Boolean =
        if (requiredPermissions.isEmpty()) true
        else extractPermissions().any { requiredPermissions.contains(it) }

    internal open fun extractPermissions(): Set<String> =
        accessor.extractJwtClaims()
            ?.let { it[permissionsClaim] }
            ?.let {
                when (it) {
                    is List<*> -> (it as List<String>).toSet()
                    is String -> it.split(permissionSeparator).map { s -> s.trim() }.toSet()
                    else -> null
                }
            }
            ?: emptySet()

    companion object {
        private const val defaultPermissionsClaim = "scope"
        private const val defaultPermissionSeparator: String = " "
    }
}
