package com.example.receiptocr.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.example.receiptocr.R
import com.example.receiptocr.data.ResultModel
import com.example.receiptocr.databinding.DialogResultBinding
import java.util.Collections
import java.util.TreeSet

class ResultDialog (private val text: String = "") : DialogFragment() {
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

        val result = getResultWithTotal(text)
        binding.receiptTotal = result.totalPrice.toString()
        binding.receiptTax = result.tax.toString()

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

fun getResultWithTotal(text: String): ResultModel {
    val originalResult = text.findFloat()
    if (originalResult.isEmpty()) return ResultModel(receiptItem = emptyList())
    else {
        val totalF = Collections.max(originalResult)
        val secondLargestF = findSecondLargestFloat(originalResult)
        val vat = if (secondLargestF == 0.0f) 0.0f else totalF - secondLargestF
        return ResultModel(totalF, vat, emptyList())
    }
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

private fun findSecondLargestFloat(input: ArrayList<Float>?): Float {
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