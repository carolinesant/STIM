package no.uio.ifi.team16.stim

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import no.uio.ifi.team16.stim.util.Options
import java.util.*

class OpeningScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.opening_screen)
        window.statusBarColor = getColor(R.color.dark_skyblue)

        val preferences = getSharedPreferences(Options.SHARED_PREFERENCES_KEY, MODE_PRIVATE)
        val editor = preferences.edit()

        Timer().schedule(object : TimerTask() {
            override fun run() {
                if (preferences.getBoolean("NotFirst", false)) {
                    startActivity(Intent(applicationContext, MainActivity::class.java))
                } else {
                    editor.apply {
                        putBoolean("NotFirst", true)
                        apply()
                    }
                    startActivity(Intent(applicationContext, TutorialActivity::class.java))
                }
            }
        }, 3000)
    }
}