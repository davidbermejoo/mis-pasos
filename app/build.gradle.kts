plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.abuelo.pasos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.abuelo.pasos"
        minSdk = 26          // Android 8.0 o superior
        targetSdk = 35
        versionCode = 4
        versionName = "1.3"
    }

    // Firma fija: así cada versión nueva se instala encima de la anterior sin perder los datos.
    signingConfigs {
        getByName("debug") {
            storeFile = file("firma.jks")
            storePassword = "mispasos"
            keyAlias = "mispasos"
            keyPassword = "mispasos"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

// Sin dependencias externas: solo Android. Ni anuncios, ni analíticas, ni internet.
