# Receipt OCR Scanner App

An Android app built to scan receipts using ML Kit's OCR capabilities. The main goal is to extract
data from the receipt such as product names and their prices.

## Done so far

- Getting Receipt Items, Total Cost and Tax from organized receipt text
- Getting image, Recognizing text, showing processed text from a receipt
- Rotating image/text lines and organizing text elements similar to a receipt

## Goals

- ✅ Extracting Item Names and Prices from receipts.
- ✅️ Extracting the Total Amount of receipts.
- Additional:
  - ✅️ Tax
  - ⏩️ Receipt date
  - ⏩ Store name
  - ⏩ Store address

## Preview

|                      Adding Receipt                      |                      Found Elements                      |                     Receipt Details                      |                       Receipt Text                       |
|:--------------------------------------------------------:|:--------------------------------------------------------:|:--------------------------------------------------------:|:--------------------------------------------------------:|
| <img src="https://i.imgur.com/LszKOvb.png" width="200"/> | <img src="https://i.imgur.com/TcfJXeX.png" width="200"/> | <img src="https://i.imgur.com/OsEYnu3.png" width="200"/> | <img src="https://i.imgur.com/ARmHj2i.png" width="200"/> |

## Item Recognition and Text Alignment
While ML Kit performs well on general text, it struggles with structured, tabular data, such as receipts. Typically, item names and prices are detected as separate text blocks, making it difficult to maintain alignment
 - Text is captured in a hierarchy of Blocks, Lines, and Elements. Each [Text.Element](https://developers.google.com/android/reference/com/google/mlkit/vision/text/Text.Element) includes its position (`Rect`), which helps reconstruct the layout by placing the elements based on their coordinates
 - When an image isn’t perfectly aligned (e.g., tilted or rotated), matching item names with their respective prices becomes difficult as they may not appear on the same horizontal line. To fix this, the app first scans the image to determine the average angle of the text `Text.Element.getAngle()`. The source image is then rotated based on this angle
- Regex pattern to find receipt items in text : `(?<!\d)([A-Za-z ]{3,})\s+\$?\s*(\d{1,4}\.\d{2})` (
  So far)

## Tech Stack

- **Language:** Kotlin, Views/XML
- **Libraries:**
  - [ML Kit - Text recognition v2](https://developers.google.com/ml-kit/vision/text-recognition/v2)
    for OCR (Local)
  - [ImagePicker](https://github.com/Dhaval2404/ImagePicker)
