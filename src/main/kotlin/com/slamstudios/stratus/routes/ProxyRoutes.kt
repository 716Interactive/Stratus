package com.slamstudios.stratus.routes

import com.slamstudios.stratus.services.ProxyService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class CreateProxyRequest(
    val name: String,
    val host: String,
    val port: Int,
    val proxyGroupId: String? = null,
    val isMain: Boolean = false
)

fun Route.proxyRoutes() {
    route("/proxies") {
        get {
            call.respond(ProxyService.getAll())
        }
        
        get("/main") {
            val main = ProxyService.getMainProxy()
            if (main != null) call.respond(main) else call.respond(HttpStatusCode.NotFound)
        }

        post {
            val req = call.receive<CreateProxyRequest>()
            val proxy = ProxyService.registerProxy(
                name = req.name,
                host = req.host,
                port = req.port,
                groupId = req.proxyGroupId,
                isMain = req.isMain
            )
            call.respond(proxy)
        }
    }
}
