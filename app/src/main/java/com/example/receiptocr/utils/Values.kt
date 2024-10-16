package com.example.receiptocr.utils

val receiptItemRegex = Regex("""(?<!\d)([A-Za-z ]{3,})\s+\$?\s*(\d{1,4}\.\d{2})""")
val receiptTotalsRegex =
    Regex("""(?i).*(TOTAL|PAID|TAX|VAT|CASH|BALANCE|CARD|CHARGE|TRANSACTION|VOID|TXTL|TOTL|CHNG|DISCOUNT|DEBIT|CREDIT|VISA|MASTERCARD|DISCOVER).*""")
