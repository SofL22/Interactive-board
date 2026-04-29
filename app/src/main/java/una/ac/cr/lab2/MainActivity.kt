package una.ac.cr.lab2


import android.Manifest
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.GestureDetector
import androidx.appcompat.widget.AppCompatEditText
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.graphics.PorterDuff
import android.media.MediaPlayer
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import android.text.Editable
import android.text.TextWatcher
import android.widget.LinearLayout
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var boardContainer: FrameLayout
    private lateinit var headerContainer: LinearLayout
    private lateinit var btnAgregarPostIt: Button
    private lateinit var btnCambiarTema: Button
    private lateinit var tvTitulo: TextView
    private lateinit var tvNetworkStatus: TextView
    private var offset = 50
    private var temaActual = BoardTheme.DUCKS
    private val prefsName = "pizarra_prefs"
    private val postItsKey = "postits_guardados"
    private lateinit var rootLayout: FrameLayout
    private lateinit var ivDecorLeft: ImageView
    private lateinit var ivDecorRight: ImageView
    private lateinit var ivDecorTop: ImageView

    private lateinit var btnAgregarSticker: Button
    private lateinit var btnIdeaTema: Button
    private val themeIdeaService = ThemeIdeaService()

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        headerContainer = findViewById(R.id.headerContainer)
        rootLayout = findViewById(R.id.rootLayout)
        tvTitulo = findViewById(R.id.tvTitulo)
        tvNetworkStatus = findViewById(R.id.tvNetworkStatus)
        boardContainer = findViewById(R.id.boardContainer)
        btnAgregarPostIt = findViewById(R.id.btnAgregarPostIt)
        btnCambiarTema = findViewById(R.id.btnCambiarTema)
        ivDecorLeft = findViewById(R.id.ivDecorLeft)
        ivDecorRight = findViewById(R.id.ivDecorRight)
        ivDecorTop = findViewById(R.id.ivDecorTop)
        btnAgregarSticker = findViewById(R.id.btnAgregarSticker)
        btnIdeaTema = findViewById(R.id.btnIdeaTema)

        btnAgregarPostIt.setOnClickListener {
            agregarPostIt()
        }

        btnCambiarTema.setOnClickListener {
            mostrarSelectorTema()
        }
        temaActual = cargarTemaGuardado()
        aplicarTema(temaActual)
        cargarPostIts()
        actualizarEstadoRed()
        btnAgregarSticker.setOnClickListener {
            mostrarSelectorStickers()
        }

        btnIdeaTema.setOnClickListener {
            generarPostItDesdeTema()
        }
    }

    private fun aplicarEstiloBotones(background: Int, textColor: Int) {
        listOf(btnAgregarPostIt, btnCambiarTema, btnAgregarSticker, btnIdeaTema).forEach {
            it.elevation = 8f
            it.stateListAnimator = null
        }

        btnAgregarPostIt.setBackgroundResource(background)
        btnCambiarTema.setBackgroundResource(background)
        btnAgregarSticker.setBackgroundResource(background)
        btnIdeaTema.setBackgroundResource(background)


        btnAgregarPostIt.setTextColor(getColor(textColor))
        btnCambiarTema.setTextColor(getColor(textColor))
        btnAgregarSticker.setTextColor(getColor(textColor))
        btnIdeaTema.setTextColor(getColor(textColor))
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun actualizarEstadoRed() {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        val status = when {
            capabilities == null -> "Sin conexión"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Conectado por Wi‑Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Conectado por datos móviles"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Conectado por Ethernet"
            else -> "Conexión activa"
        }

        tvNetworkStatus.text = "Red: $status"
        tvNetworkStatus.setTextColor(
            getColor(
                if (capabilities == null) android.R.color.holo_red_light else android.R.color.holo_green_dark
            )
        )
    }
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun onResume() {
        super.onResume()
        actualizarEstadoRed()
    }

    private fun desactivarEdicionDeTodosLosPostIts() {
        for (i in 0 until boardContainer.childCount) {
            val child = boardContainer.getChildAt(i)
            if (child is androidx.appcompat.widget.AppCompatEditText) {
                desactivarEdicion(child)
            }
        }
    }

    private fun guardarTema(theme: BoardTheme) {
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        prefs.edit { putString("tema_actual", theme.name) }
    }

    private fun cargarTemaGuardado(): BoardTheme {
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        val themeName = prefs.getString("tema_actual", BoardTheme.PASTEL.name)

        return themeName
            ?.let { savedName -> BoardTheme.entries.firstOrNull { it.name == savedName } }
            ?: BoardTheme.PASTEL

    }

    private fun obtenerDrawablePostIt(): Int {
        return when (temaActual) {
            BoardTheme.DUCKS -> R.drawable.postit_duck
            BoardTheme.PASTEL -> R.drawable.postit_yellow
            BoardTheme.DARK -> R.drawable.postit_dark_scary
            BoardTheme.NATURE -> R.drawable.postit_nature
            BoardTheme.FRUTIGER -> R.drawable.postit_frutiger
            BoardTheme.RETRO_PIXEL -> R.drawable.postit_retro
            BoardTheme.CRAZY -> R.drawable.postit_crazy
            else -> R.drawable.postit_green


        }
    }

    private fun limpiarDecoraciones() {
        ivDecorLeft.clearAnimation()
        ivDecorRight.clearAnimation()
        ivDecorTop.clearAnimation()

        ivDecorLeft.setImageDrawable(null)
        ivDecorRight.setImageDrawable(null)
        ivDecorTop.setImageDrawable(null)

        ivDecorLeft.alpha = 1f
        ivDecorRight.alpha = 1f
        ivDecorTop.alpha = 1f

        ivDecorLeft.translationX = 0f
        ivDecorLeft.translationY = 0f
        ivDecorRight.translationX = 0f
        ivDecorRight.translationY = 0f
        ivDecorTop.translationX = 0f
        ivDecorTop.translationY = 0f
    }

    private fun aplicarTemaDucks() {
        rootLayout.setBackgroundColor(getColor(R.color.ducks_root))
        boardContainer.setBackgroundResource(R.drawable.board_ducks)
        headerContainer.setBackgroundResource(R.drawable.button_ducks)
        tvTitulo.text = "🦆 MagiClass 🦆"
        tvTitulo.setTextColor(getColor(R.color.ducks_title))

        btnAgregarPostIt.text = "🦆 Post-it"
        btnCambiarTema.text = "✨ Temas 🦆"
        btnIdeaTema.text = "💡 Idea"

        aplicarEstiloBotones(
            R.drawable.button_ducks,
            android.R.color.black
        )
        ivDecorLeft.setImageResource(R.drawable.duck)
        ivDecorRight.setImageResource(R.drawable.duck)
        ivDecorTop.setImageResource(R.drawable.duck)

        animarFlotacion(ivDecorLeft)
        animarFlotacion(ivDecorRight)
    }

    private fun reproducirSonidoPato() {
        val mediaPlayer = MediaPlayer.create(this, R.raw.quack)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            it.release()
        }
    }

    private fun guardarPostIts() {
        val jsonArray = JSONArray()

        for (i in 0 until boardContainer.childCount) {
            val child = boardContainer.getChildAt(i)
            if (child is AppCompatEditText) {
                val obj = JSONObject()
                obj.put("text", child.text?.toString() ?: "")
                obj.put("x", child.x)
                obj.put("y", child.y)
                jsonArray.put(obj)
            }
        }

        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        prefs.edit { putString(postItsKey, jsonArray.toString()) }
    }

    private fun cargarPostIts() {
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        val jsonString = prefs.getString(postItsKey, null) ?: return

        val jsonArray = JSONArray(jsonString)

        boardContainer.removeAllViews()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)

            val texto = obj.getString("text")
            val x = obj.getDouble("x").toFloat()
            val y = obj.getDouble("y").toFloat()

            crearPostItRestaurado(texto, x, y)
        }
    }

    private fun crearPostItRestaurado(texto: String, posX: Float, posY: Float) {
        val postIt = AppCompatEditText(this)

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        postIt.layoutParams = params

        postIt.hint = "Doble toque para editar"
        postIt.setText(texto)
        postIt.setPadding(24, 24, 24, 24)
        postIt.setBackgroundResource(obtenerDrawablePostIt())
        postIt.elevation = 8f
        postIt.minWidth = 300
        postIt.minHeight = 260
        postIt.maxWidth = 700
        postIt.isSingleLine = false
        postIt.setHorizontallyScrolling(false)

        desactivarEdicion(postIt)
        configurarGestosPostIt(postIt)
        agregarAutoGuardado(postIt)

        boardContainer.addView(postIt)

        postIt.post {
            ajustarTamanoPostIt(postIt)
            postIt.x = posX
            postIt.y = posY
        }
    }

    private fun agregarAutoGuardado(postIt: AppCompatEditText) {
        postIt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                ajustarTamanoPostIt(postIt)
                guardarPostIts()
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun habilitarDrag(view: View) {
        var dX = 0f
        var dY = 0f
        var inicialX = 0f
        var inicialY = 0f
        var dragging = false

        view.setOnTouchListener { v, event ->
            when (event.action) {


                MotionEvent.ACTION_MOVE -> {
                    val deltaX = kotlin.math.abs(event.rawX - inicialX)
                    val deltaY = kotlin.math.abs(event.rawY - inicialY)

                    if (deltaX > 20 || deltaY > 20) {
                        dragging = true
                    }

                    if (dragging) {
                        var nuevaX = event.rawX + dX
                        var nuevaY = event.rawY + dY

                        val maxX = boardContainer.width - v.width
                        val maxY = boardContainer.height - v.height

                        if (nuevaX < 0) nuevaX = 0f
                        if (nuevaY < 0) nuevaY = 0f
                        if (nuevaX > maxX) nuevaX = maxX.toFloat()
                        if (nuevaY > maxY) nuevaY = maxY.toFloat()

                        v.x = nuevaX
                        v.y = nuevaY
                        return@setOnTouchListener true
                    }

                    false
                }

                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - event.rawX
                    dY = v.y - event.rawY
                    v.scaleX = 1.05f
                    v.scaleY = 1.05f
                    v.elevation = 16f
                    true
                }

                MotionEvent.ACTION_UP -> {
                    v.scaleX = 1f
                    v.scaleY = 1f
                    v.elevation = 8f
                    true
                }

                else -> false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun configurarGestosPostIt(postIt: AppCompatEditText) {

        val gestureDetector = GestureDetector(
            this,
            object : GestureDetector.SimpleOnGestureListener() {

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    desactivarEdicionDeTodosLosPostIts()
                    activarEdicion(postIt)
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    mostrarDialogoEliminar(postIt, "post-it")
                }
            }
        )

        var dX = 0f
        var dY = 0f

        postIt.setOnTouchListener { v, event ->

            gestureDetector.onTouchEvent(event)

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {
                    if (!postIt.isFocusableInTouchMode) {
                        dX = v.x - event.rawX
                        dY = v.y - event.rawY
                    }
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (!postIt.isFocusableInTouchMode) {

                        val newX = event.rawX + dX
                        val newY = event.rawY + dY

                        val maxX = boardContainer.width - v.width
                        val maxY = boardContainer.height - v.height

                        v.x = newX.coerceIn(0f, maxX.toFloat())
                        v.y = newY.coerceIn(0f, maxY.toFloat())

                        return@setOnTouchListener true
                    }
                    false
                }

                MotionEvent.ACTION_UP -> {
                    guardarPostIts()
                    true
                }

                else -> false
            }
        }
    }

    private fun agregarPostIt() {
        crearPostItConTexto("")
    }

    private fun crearPostItConTexto(texto: String) {
        val postIt = AppCompatEditText(this)

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = offset
        params.topMargin = offset

        postIt.layoutParams = params
        postIt.hint = "Doble toque para editar"
        if (texto.isNotBlank()) {
            postIt.setText(texto)
        }
        postIt.setPadding(24, 24, 24, 24)
        postIt.setBackgroundResource(obtenerDrawablePostIt())
        when (temaActual) {
            BoardTheme.DARK -> {
                postIt.setTextColor(getColor(R.color.scary_text))
                postIt.setHintTextColor(getColor(R.color.scary_title))
            }
            else -> {
                postIt.setTextColor(getColor(android.R.color.black))
                postIt.setHintTextColor(getColor(android.R.color.darker_gray))
            }
        }
        postIt.elevation = 8f
        postIt.minWidth = 300
        postIt.minHeight = 260
        postIt.maxWidth = 700
        postIt.isSingleLine = false
        postIt.setHorizontallyScrolling(false)

        desactivarEdicion(postIt)
        configurarGestosPostIt(postIt)
        agregarAutoGuardado(postIt)

        boardContainer.addView(postIt)

        postIt.post {
            ajustarTamanoPostIt(postIt)
            postIt.x = offset.toFloat()
            postIt.y = offset.toFloat()
            guardarPostIts()
        }

        offset += 40
        if (offset > 300) {
            offset = 50
        }

        if (temaActual == BoardTheme.DUCKS) {
            reproducirSonidoPato()
        }
        actualizarPostItsSegunTema()
    }

    private fun generarPostItDesdeTema() {

        lifecycleScope.launch {
            val contenido = withContext(Dispatchers.IO) {
                themeIdeaService.obtenerIdea(temaActual)
            }
            crearPostItConTexto(contenido)
        }
    }

    private fun ajustarTamanoPostIt(postIt: AppCompatEditText) {
        postIt.post {
            val layout = postIt.layout
            val anchoTexto = if (layout != null && layout.lineCount > 0) {
                (0 until layout.lineCount).maxOf { layout.getLineWidth(it).toInt() }
            } else {
                postIt.width
            }
            val anchoDeseado = anchoTexto + postIt.paddingLeft + postIt.paddingRight
            postIt.measure(
                View.MeasureSpec.makeMeasureSpec(postIt.maxWidth, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val params = postIt.layoutParams
            params.width = anchoDeseado.coerceIn(postIt.minWidth, postIt.maxWidth)
            params.height = postIt.measuredHeight.coerceAtLeast(postIt.minHeight)
            postIt.layoutParams = params
        }
    }
    private fun animarFlotacion(view: ImageView) {
        view.animate()
            .translationYBy(20f)
            .setDuration(1500)
            .withEndAction {
                view.animate()
                    .translationYBy(-20f)
                    .setDuration(1500)
                    .withEndAction {
                        animarFlotacion(view)
                    }
                    .start()
            }
            .start()
    }

    private fun activarEdicion(postIt: androidx.appcompat.widget.AppCompatEditText) {
        postIt.isFocusable = true
        postIt.isFocusableInTouchMode = true
        postIt.isCursorVisible = true
        postIt.isLongClickable = true
        postIt.requestFocus()
        postIt.setSelection(postIt.text?.length ?: 0)

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(postIt, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun actualizarPostItsSegunTema() {
        for (i in 0 until boardContainer.childCount) {
            val child = boardContainer.getChildAt(i)
            if (child is androidx.appcompat.widget.AppCompatEditText) {
                child.setBackgroundResource(obtenerDrawablePostIt())

                when (temaActual) {

                    BoardTheme.DUCKS -> {
                        child.setTextColor(getColor(android.R.color.black))
                        child.setHintTextColor(getColor(android.R.color.darker_gray))
                    }

                    BoardTheme.DARK -> {
                        child.setTextColor(getColor(R.color.scary_text))
                        child.setHintTextColor(getColor(R.color.scary_title))
                    }

                    BoardTheme.NATURE -> {
                        child.setTextColor(getColor(android.R.color.black))
                        child.setHintTextColor(getColor(android.R.color.darker_gray))
                    }

                    BoardTheme.PASTEL -> {
                        child.setTextColor(getColor(R.color.pastel_text))
                        child.setHintTextColor(getColor(R.color.pastel_title))
                    }

                    else -> {}
                }
            }
        }
    }

    private fun desactivarEdicion(postIt: androidx.appcompat.widget.AppCompatEditText) {
        postIt.isFocusable = false
        postIt.isFocusableInTouchMode = false
        postIt.isCursorVisible = false
        postIt.isLongClickable = false
        postIt.clearFocus()

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(postIt.windowToken, 0)
    }

    private fun aplicarTemaDarkScary() {
        rootLayout.setBackgroundColor(getColor(R.color.scary_root))
        boardContainer.setBackgroundResource(R.drawable.board_dark_scary)
        headerContainer.setBackgroundResource(R.drawable.button_scary)
        tvTitulo.text = "☠ MagiClass ☠"
        tvTitulo.setTextColor(getColor(R.color.scary_title))

        btnAgregarPostIt.text = "🔥 Agregar Post-it"
        btnCambiarTema.text = "Temas"
        btnAgregarSticker.text = "Agregar Sticker"
        btnIdeaTema.text = "💡 Idea"
        btnAgregarSticker.setTextColor(getColor(R.color.scary_text))

        btnAgregarPostIt.setTextColor(getColor(R.color.scary_text))
        btnCambiarTema.setTextColor(getColor(R.color.scary_text))

        aplicarEstiloBotones(
            R.drawable.button_scary,
            R.color.scary_text
        )

        ivDecorLeft.setImageResource(R.drawable.lava_left)
        ivDecorRight.setImageResource(R.drawable.lava_right)
        ivDecorTop.setImageResource(R.drawable.lava_up)

        animacionOscura(ivDecorLeft)
        animacionOscura(ivDecorRight)
        animacionHumo(ivDecorTop)
    }

    private fun aplicarTemaPastel() {
        rootLayout.setBackgroundColor(getColor(R.color.pastel_background))
        boardContainer.setBackgroundResource(R.drawable.board_pastel)
        headerContainer.setBackgroundResource(R.drawable.button_pastel)

        tvTitulo.setTextColor(getColor(R.color.pastel_title))
        tvTitulo.text =  "🎀 MagiClass 🎀"

        btnAgregarPostIt.setBackgroundResource(R.drawable.button_pastel)
        btnCambiarTema.setBackgroundResource(R.drawable.button_pastel)
        btnAgregarPostIt.text = "🩷 Agregar Post-it"
        btnCambiarTema.text = "✨ Temas"
        btnIdeaTema.text = "💡 Idea"

        aplicarEstiloBotones(
            R.drawable.button_pastel,
            android.R.color.holo_purple
        )

        btnAgregarPostIt.setTextColor(getColor(R.color.pastel_button_text))
        btnCambiarTema.setTextColor(getColor(R.color.pastel_button_text))
        btnAgregarSticker.setTextColor(getColor(R.color.pastel_button_text))

        ivDecorLeft.setImageResource(R.drawable.pastel_izquierda)
        ivDecorRight.setImageResource(R.drawable.heart_derecha)
        ivDecorTop.setImageDrawable(null)

        ivDecorLeft.visibility = View.VISIBLE
        ivDecorRight.visibility = View.VISIBLE
        ivDecorTop.visibility = View.VISIBLE

        animarFlotacion(ivDecorLeft)
        animarFlotacion(ivDecorRight)

    }

    private fun aplicarTemaNature() {
        rootLayout.setBackgroundColor(getColor(R.color.nature_root))
        boardContainer.setBackgroundResource(R.drawable.board_nature)
        headerContainer.setBackgroundResource(R.drawable.button_nature)
        tvTitulo.text = "🌿 MagiClass 🌿"
        tvTitulo.setTextColor(getColor(R.color.nature_title))

        btnAgregarPostIt.text = "🍃 Agregar Post-it"
        btnCambiarTema.text = "🌱 Temas"
        btnIdeaTema.text = "💡 Idea"

        aplicarEstiloBotones(
            R.drawable.button_nature,
            android.R.color.white
        )

        ivDecorLeft.setImageResource(R.drawable.leaf_left)
        ivDecorRight.setImageResource(R.drawable.leaf_right)


        animarEnredadera(ivDecorLeft)
        animarEnredadera(ivDecorRight)
        animarHojas(ivDecorTop)
    }

    private fun aplicarTemaFrutiger() {
        rootLayout.setBackgroundColor(getColor(R.color.frutiger_root))
        boardContainer.setBackgroundColor(getColor(R.color.frutiger_board))
        headerContainer.setBackgroundResource(R.drawable.button_frutiger)
        tvTitulo.text = " Frutiger Aero "
        tvTitulo.setTextColor(getColor(R.color.frutiger_title))

        btnAgregarPostIt.text = "💎 Post-it"
        btnCambiarTema.text = " Temas"
        btnAgregarSticker.text = "Agregar Sticker"
        btnIdeaTema.text = "💡 Idea"
        btnAgregarSticker.setTextColor(getColor(R.color.scary_text))
        btnAgregarPostIt.setTextColor(getColor(android.R.color.black))
        btnAgregarPostIt.setBackgroundColor(getColor(R.color.frutiger_button))
        btnCambiarTema.setBackgroundColor(getColor(R.color.frutiger_button))

        btnAgregarPostIt.setTextColor(getColor(R.color.frutiger_text))
        btnCambiarTema.setTextColor(getColor(R.color.frutiger_text))

        aplicarEstiloBotones(
            R.drawable.button_frutiger,
            android.R.color.white
        )

        ivDecorLeft.visibility = View.VISIBLE
        ivDecorRight.visibility = View.VISIBLE
        ivDecorTop.visibility = View.VISIBLE
    }

    private fun aplicarTemaRetroPixel() {
        rootLayout.setBackgroundColor(getColor(R.color.retro_root))
        boardContainer.setBackgroundColor(getColor(R.color.retro_board))
        headerContainer.setBackgroundResource(R.drawable.button_pixel)
        tvTitulo.text = "🕹 Retro Pixel 🕹"
        tvTitulo.setTextColor(getColor(R.color.retro_title))

        btnAgregarPostIt.text = "👾 Post-it"
        btnCambiarTema.text = "🕹 Temas"
        btnAgregarSticker.text = "Agregar Sticker"
        btnIdeaTema.text = "💡 Idea"
        btnAgregarPostIt.backgroundTintMode = PorterDuff.Mode.SRC_ATOP

        aplicarEstiloBotones(
            R.drawable.button_pixel,
            android.R.color.white
        )

        btnAgregarPostIt.setBackgroundColor(getColor(R.color.retro_button))
        btnCambiarTema.setBackgroundColor(getColor(R.color.retro_button))
        btnCambiarTema.setBackgroundColor(getColor(R.color.retro_button))

        btnAgregarPostIt.setTextColor(getColor(R.color.retro_text))
        btnCambiarTema.setTextColor(getColor(R.color.retro_text))
        btnAgregarSticker.setTextColor(getColor(R.color.pastel_button_text))

        ivDecorLeft.setImageResource(R.drawable.angry)
        ivDecorRight.setImageResource(R.drawable.pool)
        ivDecorTop.setImageResource(R.drawable.star)

        ivDecorLeft.visibility = View.VISIBLE
        ivDecorRight.visibility = View.VISIBLE
        ivDecorTop.visibility = View.VISIBLE

        animarFlotacion(ivDecorLeft)
        animarFlotacion(ivDecorRight)
        animarFlotacion(ivDecorTop)
    }

    private fun aplicarTemaCrazy() {
        rootLayout.setBackgroundColor(getColor(R.color.crazy_root))
        boardContainer.setBackgroundColor(getColor(R.color.crazy_board))
        headerContainer.setBackgroundResource(R.drawable.button_crazy)
        tvTitulo.text = "💥 CRAZY MODE 💥"
        tvTitulo.setTextColor(getColor(R.color.crazy_yellow))

        btnAgregarPostIt.text = "⚡ Post-it"
        btnCambiarTema.text = "🎲 Temas"
        btnIdeaTema.text = "💡 Idea"

        btnAgregarPostIt.setBackgroundColor(getColor(R.color.crazy_pink))
        btnCambiarTema.setBackgroundColor(getColor(R.color.crazy_blue))

        btnAgregarPostIt.setTextColor(getColor(R.color.crazy_text))
        btnCambiarTema.setTextColor(getColor(R.color.crazy_text))

        ivDecorLeft.setImageResource(R.drawable.lava_left)
        ivDecorRight.setImageResource(R.drawable.lava_right)

        ivDecorLeft.setImageResource(R.drawable.crazy_izquierda)
        ivDecorRight.setImageResource(R.drawable.crazy_derecha)

        ivDecorLeft.visibility = View.VISIBLE
        ivDecorRight.visibility = View.VISIBLE
        ivDecorTop.visibility = View.VISIBLE
        aplicarEstiloBotones(
            R.drawable.button_crazy,
            android.R.color.white
        )

        animacionOscura(ivDecorLeft)
        animacionOscura(ivDecorRight)
        animacionOscura(ivDecorTop)
    }

    private fun animarEnredadera(view: ImageView) {
        view.animate()
            .translationXBy(8f)
            .setDuration(2200)
            .withEndAction {
                view.animate()
                    .translationXBy(-8f)
                    .setDuration(2200)
                    .withEndAction {
                        animarEnredadera(view)
                    }
                    .start()
            }
            .start()
    }

    private fun animarHojas(view: ImageView) {
        view.animate()
            .translationYBy(6f)
            .rotationBy(2f)
            .setDuration(2000)
            .withEndAction {
                view.animate()
                    .translationYBy(-6f)
                    .rotationBy(-2f)
                    .setDuration(2000)
                    .withEndAction {
                        animarHojas(view)
                    }
                    .start()
            }
            .start()
    }

    private fun aplicarTema(theme: BoardTheme) {
        temaActual = theme
        limpiarDecoraciones()

        when (theme) {
            //BoardTheme.NORMAL -> aplicarTemaNormal()
            BoardTheme.DUCKS -> aplicarTemaDucks()
            BoardTheme.DARK -> aplicarTemaDarkScary()
            BoardTheme.NATURE -> aplicarTemaNature()
            BoardTheme.PASTEL -> aplicarTemaPastel()
            BoardTheme.CRAZY -> aplicarTemaCrazy()
            BoardTheme.FRUTIGER -> aplicarTemaFrutiger()
            BoardTheme.RETRO_PIXEL -> aplicarTemaRetroPixel()
            else -> {}
        }

        actualizarPostItsSegunTema()
        guardarTema(theme)
    }

    private fun animacionOscura(view: ImageView) {
        view.animate()
            .alpha(0.65f)
            .setDuration(1400)
            .withEndAction {
                view.animate()
                    .alpha(1f)
                    .setDuration(1400)
                    .withEndAction {
                        animacionOscura(view)
                    }
                    .start()
            }
            .start()
    }

    private fun animacionHumo(view: ImageView) {
        view.animate()
            .translationYBy(-10f)
            .alpha(0.8f)
            .setDuration(1800)
            .withEndAction {
                view.animate()
                    .translationYBy(10f)
                    .alpha(1f)
                    .setDuration(1800)
                    .withEndAction {
                        animacionHumo(view)
                    }
                    .start()
            }
            .start()
    }

    private fun mostrarSelectorStickers(){
        val nombres = arrayOf("Alpaca", "Globos", "Oso", "Vaca", "Flamenco", "Regalo")
        val recursos = arrayOf(
            R.drawable.alpaca,
            R.drawable.balloons,
            R.drawable.bear,
            R.drawable.cow,
            R.drawable.flamingo,
            R.drawable.gift
        )

        AlertDialog.Builder(this)
            .setTitle("Selecciona un sticker")
            .setItems(nombres) { _, which ->
                agregarSticker(recursos[which])
            }
            .show()
    }

    private fun configurarDragSticker(sticker: AppCompatImageView) {
        var dX = 0f
        var dY = 0f

        sticker.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val newX = event.rawX + dX
                    val newY = event.rawY + dY

                    val maxX = boardContainer.width - view.width.toFloat()
                    val maxY = boardContainer.height - view.height.toFloat()

                    view.x = newX.coerceIn(0f, maxX)
                    view.y = newY.coerceIn(0f, maxY)
                    true
                }

                else -> false
            }
        }
    }

    private fun agregarSticker(resId: Int){
        val sticker = AppCompatImageView(this)

        val size = 180
        val params = FrameLayout.LayoutParams(size, size)
        sticker.layoutParams = params

        sticker.setImageResource(resId)
        sticker.scaleType = ImageView.ScaleType.FIT_CENTER

        sticker.x = 100f
        sticker.y = 100f

        configurarDragSticker(sticker)

        boardContainer.addView(sticker)
    }

    private fun mostrarRandomTemas() {
        val view = layoutInflater.inflate(R.layout.dialog_random_cards, null)

        val card1 = view.findViewById<ImageView>(R.id.card1)
        val card2 = view.findViewById<ImageView>(R.id.card2)
        val card3 = view.findViewById<ImageView>(R.id.card3)

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        val clickCarta = View.OnClickListener {
            val temasSecretos = listOf(
                BoardTheme.FRUTIGER,
                BoardTheme.RETRO_PIXEL,
                BoardTheme.CRAZY
            )

            val temaElegido = temasSecretos.random()
            dialog.dismiss()
            mostrarResultadoRandom(temaElegido)
        }

        card1.setOnClickListener(clickCarta)
        card2.setOnClickListener(clickCarta)
        card3.setOnClickListener(clickCarta)

        dialog.show()
    }

    private fun mostrarResultadoRandom(theme: BoardTheme) {
        val nombre = when (theme) {
            BoardTheme.FRUTIGER -> " Frutiger Aero"
            BoardTheme.RETRO_PIXEL -> "🕹 Retro Pixel"
            BoardTheme.CRAZY -> " Crazy Mode"
            else -> ""
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("✨ Tema secreto desbloqueado ✨")
            .setMessage("Te salió: $nombre")
            .setPositiveButton("Aplicar") { _, _ ->
                aplicarTema(theme)
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun mostrarDialogoEliminar(view: View, tipo: String) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar $tipo")
            .setMessage("¿Deseas eliminar este $tipo?")
            .setPositiveButton("Eliminar") { _, _ ->
                boardContainer.removeView(view)
                guardarPostIts()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarSelectorTema() {
        val opciones = arrayOf("Pastel Study", "Dark Neon", "Nature Board", "Ducks", "Random")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Selecciona un tema")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> aplicarTema(BoardTheme.PASTEL)
                    1 -> aplicarTema(BoardTheme.DARK)
                    2 -> aplicarTema(BoardTheme.NATURE)
                    3 -> aplicarTema(BoardTheme.DUCKS)
                    4 -> mostrarRandomTemas()
                }
            }
            .show()
    }

    override fun onPause() {
        super.onPause()
        guardarPostIts()
    }
}