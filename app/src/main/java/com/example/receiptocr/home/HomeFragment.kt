package com.example.receiptocr.home

import android.graphics.BitmapFactory
import androidx.activity.result.ActivityResultLauncher
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.example.receiptocr.R
import com.example.receiptocr.base.BaseFragment
import com.example.receiptocr.databinding.FragmentHomeBinding


class HomeFragment : BaseFragment<FragmentHomeBinding>() {
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
                val cropped =
                    BitmapFactory.decodeFile(result.getUriFilePath(requireContext(), true))
                binding.ivImage.setImageBitmap(cropped) // Displaying image in home screen
            }
        }

    }

    override fun observeView() {
        binding.btnScan.setOnClickListener { // Getting image for OCR
            cropImage.launch(cropImageContractOptions)
        }
        super.observeView()
    }
}
