package com.example.domain

enum class MonitoredApp(
    val displayName: String,
    val packages: Set<String>
) {
    WHATSAPP(
        displayName = "WhatsApp",
        packages = setOf("com.whatsapp", "com.whatsapp.w4b")
    ),
    INSTAGRAM(
        displayName = "Instagram",
        packages = setOf("com.instagram.android")
    );

    companion object {
        fun fromPackage(packageName: String): MonitoredApp? {
            return entries.firstOrNull { packageName in it.packages }
        }
    }
}
