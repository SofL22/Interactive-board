package una.ac.cr.lab2

import android.text.Html
import org.json.JSONArray
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
            BoardTheme.PASTEL -> obtenerIdeaPastel()
            BoardTheme.DARK -> obtenerIdeaDark()
            BoardTheme.NATURE -> obtenerIdeaNature()
            BoardTheme.DUCKS -> obtenerIdeaDucks()
            BoardTheme.FRUTIGER -> obtenerIdeaFrutiger()
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

    private fun obtenerIdeaPastel(): String {
        return try {
            val response = ejecutarGet("https://zenquotes.io/api/random")
            val array = JSONArray(response)
            val quote = array.optJSONObject(0)?.optString("q").orEmpty().trim()
            val author = array.optJSONObject(0)?.optString("a").orEmpty().trim()

            if (quote.isBlank()) {
                FALLBACK_PASTEL
            } else {
                "🎨 Pastel: \"$quote\"${if (author.isNotBlank()) " — $author" else ""}"
            }
        } catch (_: HttpStatusException) {
            FALLBACK_PASTEL
        } catch (_: IOException) {
            "Sin conexión para idea pastel 🎨"
        } catch (_: JSONException) {
            FALLBACK_PASTEL
        }
    }

    private fun obtenerIdeaDark(): String {
        return try {
            val response = ejecutarGet("https://uselessfacts.jsph.pl/api/v2/facts/random?language=en")
            val json = JSONObject(response)
            val fact = json.optString("text").trim()

            if (fact.isBlank()) {
                FALLBACK_DARK
            } else {
                "🌑 Dark: Convierte este dato en microcuento de terror: $fact"
            }
        } catch (_: HttpStatusException) {
            FALLBACK_DARK
        } catch (_: IOException) {
            "Sin conexión para idea dark 🌑"
        } catch (_: JSONException) {
            FALLBACK_DARK
        }
    }

    private fun obtenerIdeaNature(): String {
        return try {
            val response = ejecutarGet("https://api.adviceslip.com/advice")
            val json = JSONObject(response)
            val slip = json.optJSONObject("slip")
            val advice = slip?.optString("advice").orEmpty().trim()

            if (advice.isBlank()) {
                FALLBACK_NATURE
            } else {
                "🌿 Nature: Aplica este consejo en contacto con la naturaleza: $advice"
            }
        } catch (_: HttpStatusException) {
            FALLBACK_NATURE
        } catch (_: IOException) {
            "Sin conexión para idea nature 🌿"
        } catch (_: JSONException) {
            FALLBACK_NATURE
        }
    }

    private fun obtenerIdeaDucks(): String {
        return try {
            val response = ejecutarGet("https://random-d.uk/api/v2/random")
            val json = JSONObject(response)
            val imageUrl = json.optString("url").trim()

            if (imageUrl.isBlank()) {
                FALLBACK_DUCKS
            } else {
                "🦆 Ducks: Mira este pato y crea su historia en 2 líneas: $imageUrl"
            }
        } catch (_: HttpStatusException) {
            FALLBACK_DUCKS
        } catch (_: IOException) {
            "Sin conexión para idea ducks 🦆"
        } catch (_: JSONException) {
            FALLBACK_DUCKS
        }
    }

    private fun obtenerIdeaFrutiger(): String {
        return try {
            val response = ejecutarGet("https://api.quotable.io/random?tags=technology,famous-quotes")
            val json = JSONObject(response)
            val quote = json.optString("content").trim()
            val author = json.optString("author").trim()

            if (quote.isBlank()) {
                FALLBACK_FRUTIGER
            } else {
                "💿 Frutiger: Diseña una pantalla con esta inspiración: \"$quote\"${if (author.isNotBlank()) " — $author" else ""}"
            }
        } catch (_: HttpStatusException) {
            FALLBACK_FRUTIGER
        } catch (_: IOException) {
            "Sin conexión para idea frutiger 💿"
        } catch (_: JSONException) {
            FALLBACK_FRUTIGER
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
        const val FALLBACK_PASTEL = "No hay inspiración pastel disponible 🎨"
        const val FALLBACK_DARK = "No hay contenido dark disponible 🌑"
        const val FALLBACK_NATURE = "No hay ideas nature disponibles 🌿"
        const val FALLBACK_DUCKS = "No hay patos disponibles 🦆"
        const val FALLBACK_FRUTIGER = "No hay inspiración frutiger disponible 💿"
    }
}