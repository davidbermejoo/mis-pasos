package com.abuelo.pasos

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

class MainActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var txtPasos: TextView
    private lateinit var txtKm: TextView
    private lateinit var txtAviso: TextView
    private lateinit var lista: LinearLayout
    private var diaPintado: LocalDate? = null
    private var zancadaPintada = -1

    private val verde = Color.parseColor("#2E7D32")
    private val azul = Color.parseColor("#1565C0")
    private val rojo = Color.parseColor("#C62828")
    private val gris = Color.parseColor("#F1F1F1")

    private val es = Locale("ca", "ES")
    private val diesSetmana = arrayOf("Dilluns", "Dimarts", "Dimecres", "Dijous", "Divendres", "Dissabte", "Diumenge")
    private val mesos = arrayOf("gener", "febrer", "març", "abril", "maig", "juny", "juliol", "agost", "setembre", "octubre", "novembre", "desembre")

    private fun dataCatala(d: LocalDate): String {
        val mes = mesos[d.monthValue - 1]
        val de = if (mes[0] in "aeiou") "d'" else "de "
        return "${diesSetmana[d.dayOfWeek.value - 1]} ${d.dayOfMonth} $de$mes"
    }

    private val refresco = object : Runnable {
        override fun run() {
            pintar()
            handler.postDelayed(this, 3000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        construirPantalla()
        if (savedInstanceState == null) pedirPermisos()
    }

    override fun onResume() {
        super.onResume()
        if (StepStore.permisoOk(this)) arrancarServicio()
        handler.post(refresco)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refresco)
    }

    // ---------- Permisos y servicio ----------

    private fun pedirPermisos() {
        val faltan = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 29 && !StepStore.permisoOk(this)) {
            faltan += Manifest.permission.ACTIVITY_RECOGNITION
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            faltan += Manifest.permission.POST_NOTIFICATIONS
        }
        if (faltan.isNotEmpty()) {
            requestPermissions(faltan.toTypedArray(), 1)
        } else {
            arrancarServicio()
            pedirBateria()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (StepStore.permisoOk(this)) {
            arrancarServicio()
            pedirBateria()
        }
        pintar()
    }

    private fun arrancarServicio() {
        if (!StepStore.haySensor(this)) return
        try {
            startForegroundService(Intent(this, StepService::class.java))
        } catch (e: Exception) {
        }
    }

    /** Pide (una sola vez) que Android no cierre la app para ahorrar batería. */
    private fun pedirBateria() {
        val p = StepStore.prefs(this)
        if (p.getBoolean("bateriaPedida", false)) return
        p.edit().putBoolean("bateriaPedida", true).apply()
        val pm = getSystemService(PowerManager::class.java) ?: return
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
                )
            } catch (e: Exception) {
            }
        }
    }

    // ---------- Pantalla ----------

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun ancho() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun texto(tam: Float, negrita: Boolean = false, color: Int = Color.BLACK) =
        TextView(this).apply {
            textSize = tam
            setTextColor(color)
            gravity = Gravity.CENTER_HORIZONTAL
            if (negrita) setTypeface(typeface, Typeface.BOLD)
        }

    private fun construirPantalla() {
        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(32))
        }

        raiz.addView(texto(28f, true).apply { text = "Avui" }, ancho())

        txtPasos = texto(80f, true, verde)
        raiz.addView(txtPasos, ancho())
        raiz.addView(texto(26f).apply { text = "passos" }, ancho())

        txtKm = texto(44f, true, azul).apply { setPadding(0, dp(16), 0, 0) }
        raiz.addView(txtKm, ancho())

        txtAviso = texto(20f, true, rojo).apply {
            setPadding(0, dp(16), 0, 0)
            visibility = View.GONE
            setOnClickListener {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                )
            }
        }
        raiz.addView(txtAviso, ancho())

        raiz.addView(texto(26f, true).apply {
            text = "Últims 30 dies"
            setPadding(0, dp(40), 0, dp(12))
        }, ancho())

        lista = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        raiz.addView(lista, ancho())

        val boton = Button(this).apply {
            text = "Canviar la llargada del pas"
            textSize = 18f
            isAllCaps = false
            setOnClickListener { dialogoZancada() }
        }
        raiz.addView(boton, ancho().apply { topMargin = dp(32) })

        val scroll = ScrollView(this).apply {
            fitsSystemWindows = true
            setBackgroundColor(Color.WHITE)
            addView(raiz, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }
        setContentView(scroll)
    }

    private fun numero(n: Int): String = NumberFormat.getIntegerInstance(es).format(n)

    private fun km(pasos: Int, cm: Int): String = String.format(es, "%.1f", StepStore.km(pasos, cm))

    private fun aviso(msg: String) {
        txtAviso.text = msg
        txtAviso.visibility = View.VISIBLE
    }

    private fun pintar() {
        val hoy = LocalDate.now()
        val cm = StepStore.zancadaCm(this)
        val pasosHoy = StepStore.pasos(this, hoy)
        txtPasos.text = numero(pasosHoy)
        txtKm.text = "${km(pasosHoy, cm)} km"

        when {
            !StepStore.haySensor(this) -> aviso("Aquest mòbil no té sensor de passos.")
            !StepStore.permisoOk(this) -> aviso("Falta el permís d'\"Activitat física\".\nToca aquí per donar-lo.")
            else -> txtAviso.visibility = View.GONE
        }

        if (hoy != diaPintado || cm != zancadaPintada) {
            pintarLista(hoy, cm)
            diaPintado = hoy
            zancadaPintada = cm
        }
    }

    private fun pintarLista(hoy: LocalDate, cm: Int) {
        lista.removeAllViews()
        for (i in 1..30) {
            val dia = hoy.minusDays(i.toLong())
            val p = StepStore.pasos(this, dia)
            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(12))
                if (i % 2 == 1) setBackgroundColor(gris)
            }
            val nombre = if (i == 1) "Ahir" else dataCatala(dia)
            fila.addView(TextView(this).apply {
                text = nombre
                textSize = 18f
                setTextColor(Color.DKGRAY)
            })
            fila.addView(TextView(this).apply {
                text = "${numero(p)} passos  ·  ${km(p, cm)} km"
                textSize = 22f
                setTextColor(Color.BLACK)
                setTypeface(typeface, Typeface.BOLD)
            })
            lista.addView(fila, ancho())
        }
    }

    private fun dialogoZancada() {
        val campo = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(StepStore.zancadaCm(this@MainActivity).toString())
            textSize = 24f
            gravity = Gravity.CENTER
        }
        val caja = FrameLayout(this).apply {
            setPadding(dp(24), dp(8), dp(24), 0)
            addView(campo)
        }
        AlertDialog.Builder(this)
            .setTitle("Llargada del pas (cm)")
            .setMessage(
                "Per mesurar-la: camina 10 passes normals, mesura la distància en centímetres " +
                    "i divideix-la entre 10. El normal és entre 55 i 75 cm."
            )
            .setView(caja)
            .setPositiveButton("Desar") { _, _ ->
                val v = campo.text.toString().toIntOrNull()
                if (v != null && v in 30..120) {
                    StepStore.setZancada(this, v)
                    pintar()
                } else {
                    Toast.makeText(this, "Posa un número entre 30 i 120", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel·lar", null)
            .show()
    }
}
