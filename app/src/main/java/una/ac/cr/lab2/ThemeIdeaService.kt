package una.ac.cr.lab2

import android.text.Html
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONException
import org.json.JSONObject

class ThemeIdeaService {

    fun obtenerIdea(theme: BoardTheme): String {
        return when (theme) {
            BoardTheme.CRAZY -> obtenerIdeaCrazy()
            BoardTheme.RETRO_PIXEL -> obtenerIdeaRetro()
            else -> ""
        }
    }

    private fun obtenerIdeaCrazy(): String {
        return try {
            val response = ejecutarGet("https://v2.jokeapi.dev/joke/Any?safe-mode")
            val json = JSONObject(response)
            val type = json.optString("type")

            if (type == "single") {
                val joke = json.optString("joke")
                if (joke.isNotBlank()) "🤪 $joke" else FALLBACK_CRAZY
            } else {
                val setup = json.optString("setup")
                val delivery = json.optString("delivery")
                if (setup.isNotBlank() || delivery.isNotBlank()) {
                    "🤪 ${setup.trim()}\n${delivery.trim()}".trim()
                } else {
                    FALLBACK_CRAZY
                }
            }
        } catch (_: HttpStatusException) {
            FALLBACK_CRAZY
        } catch (_: IOException) {
            "Sin conexión para idea crazy 🤪"
        } catch (_: JSONException) {
            FALLBACK_CRAZY
        }
    }

    private fun obtenerIdeaRetro(): String {
        return try {
            val response = ejecutarGet("https://opentdb.com/api.php?amount=1&category=15&type=multiple")
            val json = JSONObject(response)
            val results = json.optJSONArray("results")
            if (results == null || results.length() == 0) {
                return FALLBACK_RETRO
            }

            val question = results.getJSONObject(0).optString("question")
            if (question.isBlank()) {
                FALLBACK_RETRO
            } else {
                val decodedQuestion = Html.fromHtml(question, Html.FROM_HTML_MODE_LEGACY).toString()
                "🕹️ Retro:\n$decodedQuestion"
            }
        } catch (_: HttpStatusException) {
            FALLBACK_RETRO
        } catch (_: IOException) {
            "Sin conexión para idea retro 🕹️"
        } catch (_: JSONException) {
            FALLBACK_RETRO
        }
    }

    @Throws(IOException::class, HttpStatusException::class)
    private fun ejecutarGet(urlStr: String): String {
        val connection = URL(urlStr).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 8000
        connection.readTimeout = 8000

        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw HttpStatusException(code)
            }

            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private class HttpStatusException(val code: Int) : IOException("HTTP error: $code")

    companion object {
        const val FALLBACK_CRAZY = "No hay locura disponible 🤪"
        const val FALLBACK_RETRO = "No hay contenido retro 🕹️"
    }
}