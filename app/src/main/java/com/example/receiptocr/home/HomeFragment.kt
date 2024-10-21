package com.example.receiptocr.home

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import android.widget.Toast
import com.example.receiptocr.R
import com.example.receiptocr.base.BaseFragment
import com.example.receiptocr.data.ReceiptItem
import com.example.receiptocr.data.ResultModel
import com.example.receiptocr.databinding.FragmentHomeBinding
import com.example.receiptocr.dialogs.image.ImageDialog
import com.example.receiptocr.dialogs.image.ImageListener
import com.example.receiptocr.dialogs.result.ResultDialog
import com.example.receiptocr.dialogs.text.TextDialog
import com.example.receiptocr.utils.findFloat
import com.example.receiptocr.utils.findSecondLargestFloat
import com.example.receiptocr.utils.getElementsList
import com.example.receiptocr.utils.receiptItemRegex
import com.example.receiptocr.utils.receiptTotalsRegex
import com.example.receiptocr.utils.rotate
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Collections
import kotlin.math.abs


class HomeFragment : BaseFragment<FragmentHomeBinding>(), ImageListener {

    private lateinit var croppedIm: Bitmap //Cropped Image and its rotation
    private var camRotationIm: Int = 0

    private val elementsRect = mutableListOf<Rect>()
    private val elementsText = mutableListOf<Text.Element>() //All text elements
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var result = ResultModel(receiptItems = emptyList())

    //TODO: Bug - Output bitmap (rotated) loosing quality/bloor effect

    override fun getLayoutID(): Int = R.layout.fragment_home

    override fun setUpViews() {
        binding.lifecycleOwner = viewLifecycleOwner
        super.setUpViews()
    }

    override fun observeView() {

        binding.btnAdd.setOnClickListener { // Getting image
            ImageDialog().show(childFragmentManager, ImageDialog.TAG)
        }

        binding.btnScan.setOnClickListener { // Start OCR if there is an image
            if (::croppedIm.isInitialized) {
                showSnackBar("Scanning...", it)
                val srcImage = InputImage.fromBitmap(croppedIm, camRotationIm)
                recognizer.process(srcImage)
                    .addOnSuccessListener { visionText ->
                        // Getting the average angle of text form the source image
                        if (visionText.text.isBlank() || visionText.text.isEmpty()) {
                            //No text found
                            showSnackBar("Text not found. Try again", it)
                            return@addOnSuccessListener
                        }

                        val elementsAngle = visionText.getElementsList().map { el -> el.angle }
                        val averageAngle = elementsAngle.average()

                        processImage(croppedIm, averageAngle.toFloat())
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            requireContext(),
                            "Error -> ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } else {
                showSnackBar("Add image first", it)
                return@setOnClickListener
            }
        }

        binding.btnResult.setOnClickListener { // Show Results Dialog
            ResultDialog(result).show(childFragmentManager, ResultDialog.TAG)
        }

        binding.btnText.setOnClickListener {
            TextDialog(result.fullText).show(childFragmentManager, ResultDialog.TAG)
        }

        super.observeView()
    }

    private fun processImage(srcBitmap: Bitmap, textAverageAngle: Float) {
        binding.angle = textAverageAngle //Angle text

        val rotatedBitmap =
            srcBitmap.rotate(-textAverageAngle) // Rotating image before getting text
        croppedIm = rotatedBitmap

        val rotatedImage = InputImage.fromBitmap(rotatedBitmap, 0)
        recognizer.process(rotatedImage)
            .addOnSuccessListener { visionText ->
                if (visionText.text.isBlank()) {
                    Toast.makeText(requireContext(), "No text found, try again", Toast.LENGTH_SHORT)
                        .show()
                    return@addOnSuccessListener
                }

                elementsText.clear()
                elementsText.addAll(visionText.getElementsList()) //Text.Elements List

                val organizedList = organizeElements(elementsText)
                val outputText = organizedList.joinToString(separator = "\n")
                result = getResults(organizedList, outputText)
                binding.btnResult.visibility = View.VISIBLE
                binding.btnText.visibility = View.VISIBLE
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error -> ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getResults(list: List<String>, wholeText: String): ResultModel {
        var total = 0f
        var tax = 0f
        val itemsList = mutableListOf<ReceiptItem>()

        val floatResults = wholeText.findFloat() // Finding Total price and Tax
        if (floatResults.isNotEmpty()) {
            val totalF = Collections.max(floatResults)
            val secondLargestF = findSecondLargestFloat(floatResults)
            val taxF = if (secondLargestF == 0.0f) 0.0f else totalF - secondLargestF
            total = totalF
            tax = taxF
        }


        for (line in list) {
            if (receiptTotalsRegex.find(line) == null) { // Check if line have a total
                val foundItem = receiptItemRegex.find(line)
                if (foundItem != null) { // If there is item
                    val itemName = foundItem.groupValues[1].trim()
                    val price = foundItem.groupValues[2].toFloat()
                    if (price > 0.0f) {
                        itemsList.add(ReceiptItem(itemName, price))
                    }
                }
            }
        }

        return ResultModel(
            totalPrice = total,
            tax = tax,
            receiptItems = itemsList,
            fullText = wholeText
        )
    }

    private fun organizeElements(elements: List<Text.Element>): List<String> {
        // Sort elements first based on their vertical position (top of the bounding box),
        // then based on the horizontal position (left of the bounding box).
        val sortedElements = elements.sortedWith { t1, t2 -> compareEls(t1, t2) }
        val lines = mutableListOf<MutableList<Text.Element>>()

        for (element in sortedElements) { // Group elements into lines
            var foundLine = false
            for (line in lines) { // Check if element belongs to an existing line
                if (isElsSameLine(line[0], element)) {
                    line.add(element)
                    foundLine = true
                    break
                }
            }
            if (!foundLine) { // If element does not belong to any line, start a new line
                lines.add(mutableListOf(element))
            }
        }

        elementsRect.clear()

        val result =
            mutableListOf<String>()  // Convert each line of elements into a string with spacing
        for (line in lines) { // Sort each line's elements horizontally (left to right)
            val sortedLine = line.sortedBy { it.boundingBox!!.left }
            elementsRect.addAll(sortedLine.map { it.boundingBox!! })
            val lineText =
                sortedLine.joinToString(" ") { it.text } // Combine elements into a single string
            result.add(lineText)
        }

        binding.ivOutImage.setImageBitmap(getOutputImage(croppedIm, elementsRect))

        return result
    }

    private fun getOutputImage(
        bitmap: Bitmap,
        eRect: List<Rect>
    ): Bitmap { // Returns output image elements on it
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        with(Canvas(mutableBitmap)) {
            eRect.forEach {
                drawRect(it, Paint().apply {
                    color = Color.GREEN
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                })
            }
        }

        return mutableBitmap
    }

    private fun compareEls(
        t1: Text.Element,
        t2: Text.Element
    ): Int { // Comparing top and left coordinates to sort elements
        val diffOfTops = t1.boundingBox!!.top - t2.boundingBox!!.top
        val diffOfLefts = t1.boundingBox!!.left - t2.boundingBox!!.left

        val height = (t1.boundingBox!!.height() + t2.boundingBox!!.height()) / 2
        val verticalDiff = (height * 0.35).toInt()

        var result = diffOfLefts
        if (abs(diffOfTops.toDouble()) > verticalDiff) {
            result = diffOfTops
        }
        return result
    }

    private fun isElsSameLine(t1: Text.Element, t2: Text.Element): Boolean {
        val diffOfTops = t1.boundingBox!!.top - t2.boundingBox!!.top

        val height = ((t1.boundingBox!!.height() + t2.boundingBox!!
            .height()) * 0.35).toInt()

        if (abs(diffOfTops.toDouble()) > height) {
            return false
        }
        return true
    }

    private fun showSnackBar(text: String, contextView: View) {
        Snackbar.make(contextView, text, 800)
            .show()

    }

    override fun onImageReceived(bitmap: Bitmap) {
        croppedIm = bitmap
        camRotationIm = 0
        binding.ivImage.setImageBitmap(croppedIm) // Displaying src image
    }
}