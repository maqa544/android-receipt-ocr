package com.example.receiptocr.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.example.receiptocr.R
import com.example.receiptocr.base.BaseFragment
import com.example.receiptocr.databinding.FragmentHomeBinding
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
    lateinit var cropped: Bitmap
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
                val image = InputImage.fromBitmap(cropped, 0)
                val result = recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        processResultText(visionText)
                        showOutputImage()
                    } //No text or image
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "No text found", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(requireContext(), "Add image first", Toast.LENGTH_SHORT).show()
            }
        }
        super.observeView()
    }

    fun processResultText(vText: Text){
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

        val sortedText = mutableListOf<String>()
        for (line in organizeElements(elementsText)) { // Sorting elements
            sortedText.add(line.joinToString(separator = " ") { it.text })
        }
        binding.tvOutput.text = sortedText.joinToString(separator = "\n") // Displaying sorted text
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

    fun organizeElements(elements: List<Text.Element>): List<List<Text.Element>> {
        val sortedLines = mutableListOf<MutableList<Text.Element>>()

        for (element in elements) {
            var addedToLine = false

            // Try to find a line this element fits into
            for (line in sortedLines) {
                if (isSameLine(line.first(), element)) {
                    // Add to existing line and sort
                    line.add(element)
                    line.sortWith(Comparator { t1, t2 -> compare(t1, t2) })
                    addedToLine = true
                    break
                }
            }

            // If the element doesn't fit into an existing line, create a new line
            if (!addedToLine) {
                sortedLines.add(mutableListOf(element))
            }
        }

        return sortedLines
    }

    fun compare(t1: Text.Element, t2: Text.Element): Int { // Comparing top and left coordinates to sort elements
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

    private fun isSameLine(t1: Text.Element, t2: Text.Element): Boolean {
        val diffOfTops = t1.boundingBox!!.top - t2.boundingBox!!.top

        val height = ((t1.boundingBox!!.height() + t2.boundingBox!!
            .height()) * 0.35).toInt()

        if (abs(diffOfTops.toDouble()) > height) {
            return false
        }
        return true
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
