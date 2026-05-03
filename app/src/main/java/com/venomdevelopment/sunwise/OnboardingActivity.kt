package com.venomdevelopment.sunwise

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro2
import com.github.appintro.AppIntroFragment
import com.github.appintro.AppIntroPageTransformerType

class OnboardingActivity : AppIntro2() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTransformer(AppIntroPageTransformerType.Fade)
        isWizardMode = true

        // set colors
        setIndicatorColor(
            getColor(R.color.white),
            getColor(R.color.md_theme_outline)
        )

        // make slides, probably not the best idea to put all the arguments in addSlide because it can get messy but I'm kinda lazy
        addSlide(
            AppIntroFragment.createInstance(
                "Welcome to Sunwise",
                "Weather, simplified.",
                R.drawable.ic_launcher_foreground,
                0,  //you see why now lol
                R.color.white,
                R.color.white,
                R.font.montsemibold,
                R.font.montsemibold,
                R.drawable.gradient
            )
        )

        addSlide(
            AppIntroFragment.createInstance(
                "Your location, at a glance",
                "Allow access to your location to get weather data for where you are, on the go.",
                R.drawable.swlocdetect,
                0,  //you see why now lol
                R.color.white,
                R.color.white,
                R.font.montsemibold,
                R.font.montsemibold,
                R.drawable.gradient_clear_night
            )
        )

        addSlide(
            AppIntroFragment.createInstance(
                "Search with ease",
                "Search for locations within the United States and get simple weather forecasts.",
                R.drawable.swsearch,
                0,
                R.color.white,
                R.color.white,
                R.font.montsemibold,
                R.font.montsemibold,
                R.drawable.gradient_thunderstorm_day
            )
        )

        addSlide(
            AppIntroFragment.createInstance(
                "Save locations for later",
                "Keep track of your locations by saving them, only a tap away.",
                R.drawable.baseline_bookmark_24,
                0,
                R.color.white,
                R.color.white,
                R.font.montsemibold,
                R.font.montsemibold,
                R.drawable.gradient_fog_night
            )
        )

        addSlide(
            AppIntroFragment.createInstance(
                "Get Started",
                "You're all set! Hope you enjoy the app!",
                R.drawable.baseline_check_circle_24,
                R.color.onboarding_done,
                R.color.white,
                R.color.white,
                R.font.montsemibold,
                R.font.montsemibold
            )
        )

        askForPermissions(
            permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            slideNumber = 2,
            required = true)


    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        finishOnboarding()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        finishOnboarding()
    }

    private fun finishOnboarding() {
        // Mark onboarding as completed
        val prefs = getSharedPreferences("SunwiseSettings", MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_completed", true).apply()

        // Start main activity
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}