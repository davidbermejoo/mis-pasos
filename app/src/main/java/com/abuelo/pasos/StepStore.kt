package com.abuelo.pasos

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import java.time.LocalDate
import kotlin.math.abs

/**
 * Guarda los pasos de cada día en el propio móvil (SharedPreferences).
 * Solo conserva los últimos 30 días; lo más antiguo se borra solo.
 */
object StepStore {

    private const val DIAS_HISTORICO = 30L
    private const val ZANCADA_POR_DEFECTO = 65 // cm

    fun prefs(c: Context): SharedPreferences =
        c.getSharedPreferences("pasos", Context.MODE_PRIVATE)

    private fun clave(d: LocalDate) = "d_$d"

    fun pasos(c: Context, d: LocalDate): Int = prefs(c).getInt(clave(d), 0)

    fun zancadaCm(c: Context): Int = prefs(c).getInt("zancada", ZANCADA_POR_DEFECTO)

    fun setZancada(c: Context, cm: Int) {
        prefs(c).edit().putInt("zancada", cm).apply()
    }

    fun km(pasos: Int, zancadaCm: Int): Double = pasos * zancadaCm / 100_000.0

    fun haySensor(c: Context): Boolean =
        c.getSystemService(SensorManager::class.java)
            ?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

    fun permisoOk(c: Context): Boolean =
        Build.VERSION.SDK_INT < 29 ||
            c.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * El sensor de Android da un total acumulado desde que se encendió el móvil.
     * Aquí calculamos cuántos pasos nuevos hay desde la última lectura y los
     * sumamos al día de hoy.
     */
    @Synchronized
    fun onCounter(c: Context, valor: Float) {
        val p = prefs(c)
        val e = p.edit()

        val arranque = System.currentTimeMillis() - SystemClock.elapsedRealtime()
        val arranqueGuardado = p.getLong("arranque", 0L)
        val seReinicio = abs(arranque - arranqueGuardado) > 60_000L
        val ultimo = p.getFloat("ultimo", -1f)

        if (ultimo >= 0f) {
            // Si el móvil se reinició, el contador vuelve a 0: todo lo que marca es nuevo.
            val nuevos = if (seReinicio || valor < ultimo) valor else valor - ultimo
            if (nuevos > 0f) {
                val k = clave(LocalDate.now())
                e.putInt(k, p.getInt(k, 0) + nuevos.toInt())
            }
        }
        e.putFloat("ultimo", valor)
        e.putLong("arranque", arranque)
        borrarAntiguos(p, e)
        e.apply()
    }

    private fun borrarAntiguos(p: SharedPreferences, e: SharedPreferences.Editor) {
        val limite = LocalDate.now().minusDays(DIAS_HISTORICO)
        for (k in p.all.keys) {
            if (!k.startsWith("d_")) continue
            val fecha = runCatching { LocalDate.parse(k.removePrefix("d_")) }.getOrNull()
            if (fecha == null || fecha.isBefore(limite)) e.remove(k)
        }
    }
}
