package com.khiasu.docscanai

import android.app.Application

class DocScanApp : Application() {
    // Room DB and WorkManager both self-initialize lazily via their own singletons
    // (see data/AppDatabase.kt). Nothing extra required here today, but this class
    // exists so we have a hook for app-wide setup (crash reporting, etc.) later.
}
