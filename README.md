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

## Text Recognition and Alignment
While ML Kit performs well on general text, it struggles with structured, tabular data, such as receipts. Typically, item names and prices are detected as separate text blocks, making it difficult to maintain alignment
 - Text is captured in a hierarchy of Blocks, Lines, and Elements. Each [Text.Element](https://developers.google.com/android/reference/com/google/mlkit/vision/text/Text.Element) includes its position (`Rect`), which helps reconstruct the layout by placing the elements based on their coordinates
 - When an image isn’t perfectly aligned (e.g., tilted or rotated), matching item names with their respective prices becomes difficult as they may not appear on the same horizontal line. To fix this, the app first scans the image to determine the average angle of the text `Text.Element.getAngle()`. The source image is then rotated based on this angle

## Tech Stack
- **Language:** Kotlin, XML
- **Libraries:**
  - [ML Kit](https://developers.google.com/ml-kit) for OCR (Local)
  - [Android-Image-Cropper](https://github.com/CanHub/Android-Image-Cropper)
