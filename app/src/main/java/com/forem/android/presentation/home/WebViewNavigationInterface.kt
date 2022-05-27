package com.forem.android.presentation.home

/**
 * This interface is only focused towards controlling the functionality when native-back button
 * is pressed by user. Once the web navigation history has reached the home page this interface
 * will communicate that information to its parent(activity) so that either the fragment can be
 * changed or activity can be finished.
 */
interface WebViewNavigationInterface {
    fun onWebPageHomeReached()
}
