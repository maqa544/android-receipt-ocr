package com.example.receiptocr.dialogs.image

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.example.receiptocr.R
import com.example.receiptocr.databinding.DialogImageBinding
import com.github.dhaval2404.imagepicker.ImagePicker


class ImageDialog( //Dialog for getting image (Cam or gallery)
) : DialogFragment() {
    private lateinit var binding: DialogImageBinding
    private var imageListener: ImageListener? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.dialog_image, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        val startForProfileImageResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                val resultCode = result.resultCode
                val data = result.data
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        //Image Uri will not be null for RESULT_OK
                        val uri = data?.data!!
                        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoder.decodeBitmap(
                                ImageDecoder.createSource(
                                    requireContext().contentResolver,
                                    uri
                                )
                            )
                        } else {
                            MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
                        }
                        passImage(bitmap)
                        dialog?.dismiss()

                    }

                    ImagePicker.RESULT_ERROR -> {
                        Toast.makeText(
                            requireContext(),
                            ImagePicker.getError(data),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    else -> {
                        Toast.makeText(requireContext(), "Task Cancelled", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }

        binding.btnClose.setOnClickListener {
            dialog?.dismiss()
        }

        binding.btnCamera.setOnClickListener {
            ImagePicker.with(this)
                .crop()
                .cameraOnly()
                .createIntent { intent ->
                    startForProfileImageResult.launch(intent)
                }
        }

        binding.btnGallery.setOnClickListener {
            ImagePicker.with(this)
                .crop()
                .galleryOnly()
                .createIntent { intent ->
                    startForProfileImageResult.launch(intent)
                }
        }

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(STYLE_NORMAL, R.style.Dialog_Custom)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        super.onResume()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        imageListener = try {
            parentFragment as ImageListener
        } catch (e: ClassCastException) {
            throw ClassCastException(
                (parentFragment.toString() +
                        " must implement DialogListener")
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
        imageListener = null
    }

    companion object {
        const val TAG = "ImageDialog"
    }

    private fun passImage(bitmap: Bitmap) {
        imageListener?.onImageReceived(bitmap)
    }
}

interface ImageListener {
    fun onImageReceived(bitmap: Bitmap)
}