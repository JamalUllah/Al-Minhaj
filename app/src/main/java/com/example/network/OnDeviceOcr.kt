package com.example.network

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.arabic.ArabicTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume

object OnDeviceOcr {
    private const val TAG = "OnDeviceOcr"

    // Instantiate Arabic/Urdu script-compatible on-device reader
    private val recognizer = TextRecognition.getClient(ArabicTextRecognizerOptions.Builder().build())

    /**
     * Runs on-device OCR on the given bitmap.
     * Maps ML Kit lines and bounding boxes to the exact JSON structure expected by the viewer UI,
     * maintaining 100% features like instant sentence click-to-highlight/copy/translate.
     */
    suspend fun runOcr(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    try {
                        val jsonRoot = JSONObject()
                        val linesArray = JSONArray()

                        val imgWidth = bitmap.width.toFloat()
                        val imgHeight = bitmap.height.toFloat()

                        for (block in visionText.textBlocks) {
                            for (line in block.lines) {
                                val text = line.text
                                val box = line.boundingBox
                                
                                val boundingBoxArray = if (box != null && imgWidth > 0 && imgHeight > 0) {
                                    val ymin = ((box.top / imgHeight) * 1000).toInt().coerceIn(0, 1000)
                                    val xmin = ((box.left / imgWidth) * 1000).toInt().coerceIn(0, 1000)
                                    val ymax = ((box.bottom / imgHeight) * 1000).toInt().coerceIn(0, 1000)
                                    val xmax = ((box.right / imgWidth) * 1000).toInt().coerceIn(0, 1000)
                                    
                                    JSONArray(listOf(ymin, xmin, ymax, xmax))
                                } else {
                                    null
                                }

                                if (text.isNotBlank()) {
                                    val lineObj = JSONObject()
                                    lineObj.put("text", text)
                                    if (boundingBoxArray != null) {
                                        lineObj.put("box_2d", boundingBoxArray)
                                    }
                                    linesArray.put(lineObj)
                                }
                            }
                        }

                        jsonRoot.put("lines", linesArray)
                        val resultJson = jsonRoot.toString(2)
                        Log.d(TAG, "On-device OCR complete: $resultJson")
                        
                        if (continuation.isActive) {
                            continuation.resume(resultJson)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed formatting JSON OCR: ${e.message}")
                        if (continuation.isActive) {
                            continuation.resume("Error formatting OCR results: ${e.localizedMessage}")
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "On-device OCR processing failed: ${exception.message}")
                    if (continuation.isActive) {
                        continuation.resume("OCR failed: ${exception.localizedMessage}")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "On-device OCR setup failed: ${e.message}")
            if (continuation.isActive) {
                continuation.resume("OCR error: ${e.localizedMessage}")
            }
        }
    }
}
