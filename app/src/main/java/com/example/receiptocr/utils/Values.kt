package com.example.receiptocr.utils

val receiptItemRegex = Regex("""(?<!\d)([A-Za-z ]{3,})(?<!lb|LB)\s*\$?\s*(\d{1,4}\.\d{2})""")
