package com.onepercent.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Application entry point. Annotated with [HiltAndroidApp] to trigger Hilt's code generation. */
@HiltAndroidApp
class OnePercentApp : Application()
