package com.example.receiptocr.dialogs.text

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.example.receiptocr.R
import com.example.receiptocr.databinding.DialogTextBinding


class TextDialog(private val text: String) : DialogFragment() { // Dialog for displaying text
    private lateinit var binding: DialogTextBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.dialog_text, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        binding.tvText.text = text

        binding.btnClose.setOnClickListener {
            dialog?.dismiss()
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

    companion object {
        const val TAG = "TextDialog"
    }

}