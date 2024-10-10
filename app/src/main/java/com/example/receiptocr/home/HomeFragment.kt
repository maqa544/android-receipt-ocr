package com.example.receiptocr.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.example.receiptocr.R
import com.example.receiptocr.base.BaseFragment
import com.example.receiptocr.data.ReceiptItem
import com.example.receiptocr.data.ResultModel
import com.example.receiptocr.databinding.FragmentHomeBinding
import com.example.receiptocr.dialogs.ResultDialog
import com.example.receiptocr.utils.findFloat
import com.example.receiptocr.utils.findSecondLargestFloat
import com.example.receiptocr.utils.receiptItemRegex
import com.example.receiptocr.utils.rotate
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.Text.Element
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Collections
import kotlin.math.abs


class HomeFragment : BaseFragment<FragmentHomeBinding>() {
    private val blocksRect = mutableListOf<Rect>() //Rects for displaying elements on output image
    private val linesRect = mutableListOf<Rect>()
    private val elementsRect = mutableListOf<Rect>()
    private val elementsText = mutableListOf<Text.Element>() //All text elements
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var originalText: String = ""
    private var result = ResultModel(receiptItems = emptyList())
    lateinit var cropped: Bitmap
    var rotation: Int = 0
    private lateinit var cropImage: ActivityResultLauncher<CropImageContractOptions>
    private val cropImageContractOptions = CropImageContractOptions(
        null,
        CropImageOptions(imageSourceIncludeGallery = true, imageSourceIncludeCamera = true)
    )

    override fun getLayoutID(): Int = R.layout.fragment_home

    override fun setUpViews() {
        binding.lifecycleOwner = viewLifecycleOwner
        super.setUpViews()

        cropImage = registerForActivityResult<CropImageContractOptions, CropImageView.CropResult>(
            CropImageContract()
        ) { result: CropImageView.CropResult ->
            if (result.isSuccessful) {
                cropped =
                    BitmapFactory.decodeFile(result.getUriFilePath(requireContext(), true))
                rotation = result.rotation
                binding.ivImage.setImageBitmap(cropped) // Displaying image in home screen
            }
        }
    }

    override fun observeView() {
        binding.btnAdd.setOnClickListener { // Getting image for OCR
            cropImage.launch(cropImageContractOptions)
        }
        binding.btnScan.setOnClickListener { // Start OCR if there is an image
            if (::cropped.isInitialized) {
                showSnackBar("Scanning...", it)
                val srcImage = InputImage.fromBitmap(cropped, rotation)
                recognizer.process(srcImage)
                    .addOnSuccessListener { visionText ->
                        val elementsAngle = getElementsForText(visionText).map { el -> el.angle }
                        val averageAngle = elementsAngle.average() // Getting the average angle of text form the source image
                        processImage(cropped, averageAngle.toFloat())
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

        super.observeView()
    }

    fun processImage(srcBitmap: Bitmap, textAverageAngle: Float) {
        binding.angle = textAverageAngle.toString()
        val rotatedBitmap = srcBitmap.rotate(-textAverageAngle) // Rotating source image before getting text
        cropped = rotatedBitmap
        val rotatedImage = InputImage.fromBitmap(rotatedBitmap, 0)
        recognizer.process(rotatedImage)
            .addOnSuccessListener { visionText ->
                processResultText(visionText)
                binding.ivOutImage.setImageBitmap(getOutputImage(cropped, blocksRect, linesRect, elementsRect))
                originalText = visionText.text
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error -> ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun processResultText(vText: Text) {
        if (vText.text.isBlank()) {
            Toast.makeText(requireContext(), "No text found, try again", Toast.LENGTH_SHORT).show()
            return
        }

        blocksRect.clear()
        linesRect.clear()
        elementsRect.clear()
        elementsText.clear()

        blocksRect.addAll(vText.textBlocks.mapNotNull { it.boundingBox })

        for (block in vText.textBlocks) {
            for (line in block.lines) {
                linesRect.add(line.boundingBox!!)
                for (element in line.elements) {
                    elementsText.add(element)
                    elementsRect.add(element.boundingBox!!)
                }
            }
        }

        elementsText //Text.Elements List

        val organizedList = organizeElements(elementsText)
        val outputText = organizedList.joinToString(separator = "\n")
        result = getResults(organizedList, outputText)

        binding.tvOutput.text = outputText // Displaying organized text
    }

    fun getResults(list: List<String>, wholeText: String): ResultModel {
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
            val foundItem = receiptItemRegex.find(line)
            if (foundItem != null) {
                val itemName = foundItem.groupValues[1].trim()
                val price = foundItem.groupValues[2].toFloat()
                itemsList.add(ReceiptItem(itemName, price))
            }
        }

        return ResultModel(totalPrice = total, tax = tax, receiptItems = itemsList)
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

        val result = mutableListOf<String>()  // Convert each line of elements into a string with spacing
        for (line in lines) { // Sort each line's elements horizontally (left to right)
            val sortedLine = line.sortedBy { it.boundingBox!!.left }
            val lineText = sortedLine.joinToString(" ") { it.text } // Combine elements into a single string
            result.add(lineText)
        }

        return result
    }

    private fun getOutputImage(bitmap: Bitmap, bRect: List<Rect>, lRect: List<Rect>, eRect: List<Rect>): Bitmap { // Returns output image with blocks, lines and elements
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        with(Canvas(mutableBitmap)) {  // Show elements on image (output)
            bRect.forEach {
                drawRect(it, Paint().apply {
                    color = Color.BLUE
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                })
            }
            lRect.forEach {
                drawRect(it, Paint().apply {
                    color = Color.GREEN
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                })
            }
            eRect.forEach {
                drawRect(it, Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                })
            }
        }

        return mutableBitmap
    }

    private fun getElementsForText(text: Text): List<Element> {
        val mlElements = mutableListOf<Element>()
        for (block in text.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    mlElements.add(element)
                }
            }
        }
        return mlElements
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
}