package com.forem.android.presentation.explore

import com.forem.android.app.model.Forem

interface DeleteForemViaAccessibilityListener {
    fun onDeleteForemClicked(forem: Forem, position: Int,)
}
