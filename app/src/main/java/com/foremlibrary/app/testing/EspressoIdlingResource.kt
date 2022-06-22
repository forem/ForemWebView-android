package com.foremlibrary.app.testing

import androidx.test.espresso.idling.CountingIdlingResource

/**
 * Idling resource to handle espresso tests run correctly.
 * Resource: https://www.youtube.com/watch?v=_96FT7E6PL4&ab_channel=CodingWithMitch
 *
 * This is used only for testing purposes.
 */
object EspressoIdlingResource {
    private const val RESOURCE = "GLOBAL"
    @JvmField
    val countingIdlingResource = CountingIdlingResource(RESOURCE)

    /** Increments the counter of CountingIdlingResource. */
    fun increment() {
        countingIdlingResource.increment()
    }

    /** Decrements the counter of CountingIdlingResource. */
    fun decrement() {
        if(!countingIdlingResource.isIdleNow){
            countingIdlingResource.decrement()
        }
    }
}