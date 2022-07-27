package com.foremlibrary.app.utility

import android.view.View
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.util.HumanReadables
import org.hamcrest.CoreMatchers.any
import org.hamcrest.Matcher
import java.util.concurrent.TimeoutException

/**
 * A [ViewAction] that waits up to [timeout] milliseconds for a [View]'s visibility value to change
 * to [View.GONE].
 *
 * Reference: https://stackoverflow.com/a/63454552
 */
class WaitUntilGoneAction(private val timeout: Long) : ViewAction {

    companion object {
        /**
         * @return a [WaitUntilGoneAction] instance created with the given [timeout] parameter.
         */
        fun waitUntilGone(timeout: Long): ViewAction {
            return WaitUntilGoneAction(timeout)
        }
    }

    override fun getConstraints(): Matcher<View> {
        return any(View::class.java)
    }

    override fun getDescription(): String {
        return "wait up to $timeout milliseconds for the view to be gone"
    }

    override fun perform(uiController: UiController, view: View) {

        val endTime = System.currentTimeMillis() + timeout

        do {
            if (view.visibility == View.GONE) return
            uiController.loopMainThreadForAtLeast(100)
        } while (System.currentTimeMillis() < endTime)

        throw PerformException.Builder()
            .withActionDescription(description)
            .withCause(TimeoutException("Waited $timeout milliseconds"))
            .withViewDescription(HumanReadables.describe(view))
            .build()
    }
}
