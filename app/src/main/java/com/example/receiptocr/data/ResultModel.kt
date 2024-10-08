package com.example.receiptocr.data

data class ResultModel (
    val totalPrice: Float = 0f,
    val tax: Float = 0f,
    val receiptItem: List<ReceiptItem>,
)