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
import com.example.receiptocr.databinding.FragmentHomeBinding
import com.example.receiptocr.dialogs.ResultDialog
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.math.abs


class HomeFragment : BaseFragment<FragmentHomeBinding>() {
    private val blocksRect = mutableListOf<Rect>() //Rects for displaying lines on output image
    private val linesRect = mutableListOf<Rect>()
    private val elementsRect = mutableListOf<Rect>()
    private val elementsText = mutableListOf<Text.Element>() //All text elements
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var originalText:String = ""
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
                val image = InputImage.fromBitmap(cropped, rotation)
                val result = recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        processResultText(visionText, it)
                        showOutputImage()
                        originalText = visionText.text
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error -> ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                showSnackBar("Add image first", it)
            }
        }
        binding.btnResult.setOnClickListener {
            ResultDialog(originalText).show(childFragmentManager, ResultDialog.TAG)
        }


        super.observeView()
    }

    fun processResultText(vText: Text, contextView: View){
        if(vText.text.isBlank()) {
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

        binding.tvOutput.text = organizeElements(elementsText).joinToString(separator = "\n") // Displaying organized text
    }

    fun showOutputImage(){
        val mutableBitmap = cropped.copy(Bitmap.Config.ARGB_8888, true)

        with(Canvas(mutableBitmap)) {  // Show elements on image (output)
            blocksRect.forEach { drawRect(it, Paint().apply {
                color = Color.BLUE
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }) }
            linesRect.forEach { drawRect(it, Paint().apply {
                color = Color.GREEN
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }) }
            elementsRect.forEach { drawRect(it, Paint().apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }) }
        }
        binding.ivOutImage.setImageBitmap(mutableBitmap)
    }

    fun organizeElements(elements: List<Text.Element>): List<String> {
        // Sort elements first based on their vertical position (top of the bounding box),
        // then based on the horizontal position (left of the bounding box).
        val sortedElements = elements.sortedWith { t1, t2 -> compareEls(t1, t2) }

        val lines = mutableListOf<MutableList<Text.Element>>()

        // Group elements into lines
        for (element in sortedElements) {
            var foundLine = false
            // Check if element belongs to an existing line
            for (line in lines) {
                if (isElsSameLine(line[0], element)) {
                    line.add(element)
                    foundLine = true
                    break
                }
            }
            // If element does not belong to any line, start a new line
            if (!foundLine) {
                lines.add(mutableListOf(element))
            }
        }

        // Convert each line of elements into a string with spacing
        val result = mutableListOf<String>()
        for (line in lines) {
            // Sort each line's elements horizontally (left to right)
            val sortedLine = line.sortedBy { it.boundingBox!!.left }

            // Combine elements into a single string with only one space between them
            val lineText = sortedLine.joinToString(" ") { it.text }

            result.add(lineText)
        }

        return result
    }


    fun compareEls(t1: Text.Element, t2: Text.Element): Int { // Comparing top and left coordinates to sort elements
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

    fun showSnackBar(text: String, contextView: View){
        Snackbar.make(contextView, text, 800)
            .show()

    }
}



//for (block in vText.textBlocks) {
//    val blockText = block.text
//    val blockCornerPoints = block.cornerPoints
//    val blockFrame = block.boundingBox
//    for (line in block.lines) {
//        val lineText = line.text
//        val lineCornerPoints = line.cornerPoints
//        val lineFrame = line.boundingBox
//        linesRect.add(line.boundingBox!!)
//        for (element in line.elements) {
//            val elementText = element.text
//            val elementCornerPoints = element.cornerPoints
//            val elementFrame = element.boundingBox
//            elementsRect.add(element.boundingBox!!)
//        }
//    }
//}
