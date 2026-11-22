package com.abuelo.pasos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Vuelve a poner en marcha el contador cuando se enciende el móvil. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        val accion = intent.action
        if (accion == Intent.ACTION_BOOT_COMPLETED || accion == Intent.ACTION_MY_PACKAGE_REPLACED) {
            if (StepStore.permisoOk(c) && StepStore.haySensor(c)) {
                try {
                    c.startForegroundService(Intent(c, StepService::class.java))
                } catch (e: Exception) {
                }
            }
        }
    }
}
