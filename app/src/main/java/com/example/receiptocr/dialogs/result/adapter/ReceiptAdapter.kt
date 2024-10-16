package com.example.receiptocr.dialogs.result.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptocr.data.ReceiptItem
import com.example.receiptocr.databinding.RvitemReceiptItemBinding

class ReceiptAdapter() :
    RecyclerView.Adapter<ReceiptAdapter.ResultVH>() {
    private val receiptItems: MutableList<ReceiptItem> = mutableListOf()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultVH {
        val itemBinding =
            RvitemReceiptItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return ResultVH(itemBinding)
    }

    override fun onBindViewHolder(holder: ResultVH, position: Int) {
        val receiptItem: ReceiptItem = receiptItems[position]
        holder.bind(receiptItem, position)
    }

    override fun getItemCount(): Int = receiptItems.size

    inner class ResultVH(private val itemBinding: RvitemReceiptItemBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(receiptItem: ReceiptItem, position: Int) {
            //Views
            itemBinding.name = receiptItem.name
            itemBinding.price = receiptItem.price.toString()
        }
    }

    @SuppressLint("NotifyDataSetChanged") // No time
    fun setData(resultItemList: List<ReceiptItem>) {
        this.receiptItems.clear()
        this.receiptItems.addAll(resultItemList)
        notifyDataSetChanged()
    }
}