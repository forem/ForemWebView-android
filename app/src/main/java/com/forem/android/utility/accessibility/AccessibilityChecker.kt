package com.forem.android.utility.accessibility

import android.content.Context
import android.view.accessibility.AccessibilityManager

class AccessibilityChecker {

    companion object {

        fun isScreenReaderEnabled(context: Context): Boolean {
            val accessibilityManager =
                context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            return accessibilityManager.isEnabled
        }
    }
}
