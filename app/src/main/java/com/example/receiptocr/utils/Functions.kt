package com.example.receiptocr.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import java.util.TreeSet

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun String.findFloat(): ArrayList<Float> {
    //get digits from result
    if (this.isBlank() || this.isEmpty()) return ArrayList()
    val originalResult = ArrayList<Float>()
    val matchedResults = Regex(pattern = "[+-]?([0-9]*[.])?[0-9]+").findAll(this)
    for (txt in matchedResults) {
        if (txt.value.isFloatAndWhole()) originalResult.add(txt.value.toFloat())
    }
    return originalResult
}

private fun String.isFloatAndWhole() = this.matches("\\d*\\.\\d*".toRegex())

fun findSecondLargestFloat(input: ArrayList<Float>?): Float {
    if (input.isNullOrEmpty() || input.size == 1) return 0.0f
    else {
        try {
            val tempSet = HashSet(input)
            val sortedSet = TreeSet(tempSet)
            return sortedSet.elementAt(sortedSet.size - 2)
        } catch (e: Exception) {
            return 0.0f
        }
    }
}