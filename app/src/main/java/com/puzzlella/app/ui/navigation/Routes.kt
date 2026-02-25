package com.puzzlella.app.ui.navigation

object Routes {
    const val HOME = "home"
    const val PREVIEW = "preview/{imageUri}"
    const val PUZZLE = "puzzle/{imagePath}/{pieceCount}"
    const val HISTORY = "history"
    const val SAMPLES = "samples"

    fun preview(imageUri: String) = "preview/${android.net.Uri.encode(imageUri)}"
    fun puzzle(imagePath: String, pieceCount: Int) =
        "puzzle/${android.net.Uri.encode(imagePath)}/$pieceCount"
}
