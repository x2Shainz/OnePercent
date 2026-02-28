package com.onepercent.app

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * An empty [ComponentActivity] annotated with [@AndroidEntryPoint] for use in
 * Hilt-powered Compose instrumented tests.
 *
 * Declared in [AndroidManifest.xml] so the Compose test rule can launch it.
 * Usage: `createAndroidComposeRule<HiltTestActivity>()`
 */
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()
