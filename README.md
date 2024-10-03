# Receipt OCR Scanner App

An Android app built to scan receipts using ML Kit's OCR capabilities. The goal of the project is to extract item names and prices from receipts.

## Done so far
- Getting image, Showing recognized text, Displaying raw text from a receipt
- Organizing text elements as on a receipt by comparing `Text.Element.boundingBox` (Item names and prices in one line)

## Goals
- Extract item names from scanned receipts.
- Extract item prices from scanned receipts.
- Additional:
  - Total amount
  - Tax
  - Store name
  - Store address
  - Receipt date

## Tech Stack
- **Language:** Kotlin, XML
- **Libraries:**
  - [ML Kit](https://developers.google.com/ml-kit) for OCR (Local)
  - [Android-Image-Cropper](https://github.com/CanHub/Android-Image-Cropper)
