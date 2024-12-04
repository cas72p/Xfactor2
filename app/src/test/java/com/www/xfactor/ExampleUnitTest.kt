package com.www.xfactor

import android.view.WindowManager
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test

import org.junit.Assert.*
import org.junit.Rule
import org.junit.runner.RunWith

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Root
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent

import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    var activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testLoginButtonClick_withEmptyFields_showsToast() {
        onView(withId(R.id.login_button)).perform(click())

        // Verify Toast message is shown
        onView(withText("Please enter email and password"))
            .inRoot(ToastMatcher())
            .check(matches(isDisplayed()))
    }

    @Test
    fun testLoginButtonClick_withValidFields_triggersLogin() {
        onView(withId(R.id.usernme_input)).perform(typeText("test@example.com"))
        onView(withId(R.id.password_input)).perform(typeText("password123"), closeSoftKeyboard())

        onView(withId(R.id.login_button)).perform(click())

        // Verify next activity is launched
        intended(hasComponent(MenuActivity::class.java.name))
    }
}

class ToastMatcher : TypeSafeMatcher<Root>() {
    override fun matchesSafely(root: Root?): Boolean {
        if (root == null) return false
        val type = root.windowLayoutParams.get().type
        if (type == WindowManager.LayoutParams.TYPE_TOAST) {
            val windowToken = root.decorView.windowToken
            val appToken = root.decorView.applicationWindowToken
            return windowToken === appToken
        }
        return false
    }

    override fun describeTo(description: Description) {
        description.appendText("is toast")
    }
}

