package com.example.receiptocr.dialogs.result

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.example.receiptocr.R
import com.example.receiptocr.data.ResultModel
import com.example.receiptocr.databinding.DialogResultBinding
import com.example.receiptocr.dialogs.result.adapter.ReceiptAdapter

class ResultDialog(private val result: ResultModel) : DialogFragment() {
    private lateinit var binding: DialogResultBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.dialog_result, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        binding.btnClose.setOnClickListener {
            dialog?.dismiss()
        }

        binding.receiptTotal = result.totalPrice.toString()
        binding.receiptTax = result.tax.toString()

        binding.receiptRv.adapter =
            ReceiptAdapter().apply {
                setData(result.receiptItems)
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
        const val TAG = "ResultDialog"
    }
}