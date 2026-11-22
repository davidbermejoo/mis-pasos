package com.abuelo.pasos

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import java.time.LocalDate
import java.util.Locale

/**
 * Servicio que se queda escuchando el sensor de pasos todo el día
 * (gasta muy poca batería: el chip del móvil cuenta los pasos por sí solo).
 * Muestra una notificación fija con los pasos de hoy.
 */
class StepService : Service(), SensorEventListener {

    companion object {
        private const val CANAL = "pasos"
        private const val ID_NOTIF = 1
    }

    private var sensorManager: SensorManager? = null
    private var ultimaNotif = 0L
    private val es = Locale("ca", "ES")

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        crearCanal()
        sensorManager = getSystemService(SensorManager::class.java)
        sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(ID_NOTIF, notificacion(), ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
            } else {
                startForeground(ID_NOTIF, notificacion())
            }
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        StepStore.onCounter(this, event.values[0])
        val ahora = System.currentTimeMillis()
        if (ahora - ultimaNotif > 15_000L) {
            ultimaNotif = ahora
            getSystemService(NotificationManager::class.java)?.notify(ID_NOTIF, notificacion())
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        sensorManager?.unregisterListener(this)
        super.onDestroy()
    }

    private fun crearCanal() {
        val canal = NotificationChannel(CANAL, "Comptador de passos", NotificationManager.IMPORTANCE_LOW)
        canal.setShowBadge(false)
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(canal)
    }

    private fun notificacion(): Notification {
        val pasos = StepStore.pasos(this, LocalDate.now())
        val km = StepStore.km(pasos, StepStore.zancadaCm(this))
        val abrir = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CANAL)
            .setSmallIcon(R.drawable.ic_paso)
            .setContentTitle("Avui: $pasos passos")
            .setContentText(String.format(es, "%.1f km", km))
            .setContentIntent(abrir)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()
    }
}
