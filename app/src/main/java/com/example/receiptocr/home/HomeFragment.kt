package com.example.receiptocr.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions


class HomeFragment : BaseFragment<FragmentHomeBinding>() {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
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
        binding.btnScan.setOnClickListener { // Start OCR
            if (::cropped.isInitialized) {
                val image = InputImage.fromBitmap(cropped, 0)
                val result = recognizer.process(image)
                    .addOnSuccessListener { visionText ->

                        val resultText = visionText.text
                        binding.tvOutput.text = resultText

                        for (block in visionText.textBlocks) {
                            val blockText = block.text
                            val blockCornerPoints = block.cornerPoints
                            val blockFrame = block.boundingBox
                            for (line in block.lines) {
                                val lineText = line.text
                                val lineCornerPoints = line.cornerPoints
                                val lineFrame = line.boundingBox
                                for (element in line.elements) {
                                    val elementText = element.text
                                    val elementCornerPoints = element.cornerPoints
                                    val elementFrame = element.boundingBox
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(requireContext(), "Add image first", Toast.LENGTH_SHORT).show()
            }
        }

        super.observeView()
    }
}
