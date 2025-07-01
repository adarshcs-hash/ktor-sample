import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.authenticatedSupabaseApi
import io.github.jan.supabase.auth.providers.Google
import io.ktor.client.request.request
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable



fun Application.configureAuthRouting() {
    routing {
        get("/login-google") {
            try {

                val session = SupabaseClient.instance.auth.signInWith(
                    provider = io.github.jan.supabase.auth.providers.Google,

                )
                call.respond(HttpStatusCode.OK, "Redirecting to Google login...")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to initiate login: ${e.message}")
            }
        }

        // Callback endpoint to handle OAuth redirect
        get("/callback") {
            try {
                val code = call.request.queryParameters["code"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing authorization code")

                // Exchange code for session
                val session =  SupabaseClient.instance.auth.exchangeCodeForSession(code)
                call.respond(
                    HttpStatusCode.OK,
                    mapOf("message" to "Login successful", "access_token" to session.accessToken)
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Callback error: ${e.message}")
            }
        }
    }
}