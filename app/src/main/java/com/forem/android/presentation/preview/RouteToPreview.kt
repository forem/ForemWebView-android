package com.forem.android.presentation.preview

import com.forem.android.app.model.Forem

interface RouteToPreview {
    fun routeToPreview(forem: Forem, deepLinkUrl: String? = null)
}
