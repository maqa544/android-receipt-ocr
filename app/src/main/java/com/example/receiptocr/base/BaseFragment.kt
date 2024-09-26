package com.example.receiptocr.base

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

abstract class BaseFragment<VB : ViewDataBinding> : Fragment() {

    private var mViewBinding: VB? = null
    val binding get() = mViewBinding!!

    abstract fun getLayoutID(): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parseArguments()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        mViewBinding = DataBindingUtil.inflate(inflater, getLayoutID(), container, false)
        setUpViews()
        observeView()
        observeData()
        return binding.root
    }

    open fun setUpViews() {}

    open fun observeView() {}

    open fun observeData() {}

    open fun parseArguments() {}

    override fun onDestroyView() {
        super.onDestroyView()
        mViewBinding = null
    }


    internal fun logNotify(@StringRes message: Int) =
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()

    internal fun logNotify(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        Log.d("notify", message)
    }
}