package no.uio.ifi.team16.stim

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * This is required to use Hilt dependency injection
 */
@HiltAndroidApp
class BaseApplication : Application()