# Mis Pasos – podómetro sencillo para el abuelo

- Cuenta los pasos del día con el sensor de pasos del móvil (no usa el GPS, apenas gasta batería).
- Calcula los km a partir de la longitud del paso (65 cm por defecto, se puede cambiar).
- Guarda los últimos 30 días en el propio móvil. Lo más antiguo se borra solo.
- Sin anuncios, sin cuentas y sin permiso de internet: no puede enviar datos a ningún sitio.
- Requiere Android 8.0 o superior.

## Cómo crear el APK (una sola vez, en un ordenador)
1. Instala Android Studio (gratis): https://developer.android.com/studio
2. Descomprime esta carpeta y en Android Studio elige **File > Open** y selecciona la carpeta `MisPasos`.
3. Espera a que termine de sincronizar (la primera vez descarga cosas y tarda unos minutos).
4. Menú **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.
5. Al terminar, pulsa "locate". El archivo es `app/build/outputs/apk/debug/app-debug.apk`.

## Cómo instalarlo en su móvil
1. Pasa `app-debug.apk` al móvil (cable USB, WhatsApp a ti mismo, Google Drive...).
2. Ábrelo en el móvil. Android pedirá permitir "instalar apps de origen desconocido": acéptalo para esa app.
3. Si sale el aviso de Play Protect, pulsa "Más detalles > Instalar de todas formas".
4. Abre **Mis Pasos** y acepta: *Actividad física*, *Notificaciones* y *No optimizar batería*.

Verás una notificación fija con los pasos de hoy. Debe quedarse ahí: es lo que mantiene el contador en marcha.

## Si deja de contar (móviles Xiaomi, Samsung, Huawei, Oppo…)
Algunas marcas cierran las apps para ahorrar batería. En Ajustes > Aplicaciones > Mis Pasos > Batería:
- **Xiaomi/Redmi/POCO**: "Sin restricciones" y activa "Inicio automático".
- **Samsung**: "Sin restricciones" y quítala de "Aplicaciones en suspensión".
- **Huawei/Honor**: Inicio de aplicaciones > gestionar manualmente y activar todo.

Nota: cuenta los pasos a partir del momento en que se instala; no recupera los anteriores.
