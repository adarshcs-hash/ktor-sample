import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlin.time.Duration


object SupabaseClient {
    val instance by lazy {
        createSupabaseClient(
            supabaseUrl = "https://jojvfmbhyhdekekwxdti.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImpvanZmbWJoeWhkZWtla3d4ZHRpIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0Nzk3NzM1NSwiZXhwIjoyMDYzNTUzMzU1fQ.jXJc763a-HWJjoKjJKIr2w6kUkaslGIvbSRppzvsBpo"
        ) {
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                        explicitNulls = false
                        coerceInputValues = true
                    })
                }
            }
            requestTimeout = Duration.parse("PT1S")

            install(Storage) {
            }

        }
    }
}

