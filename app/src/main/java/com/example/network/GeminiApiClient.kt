package com.example.network

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// --- Direct REST API Models ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null,
    val error: ErrorDetails? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content
)

@JsonClass(generateAdapter = true)
data class ErrorDetails(
    val code: Int,
    val message: String,
    val status: String
)

// --- Retrofit API Service ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        // Use Moshi properly configured with KotlinJsonAdapterFactory
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

// --- Helper Functions ---

fun Bitmap.toBase64(): String {
    val maxDimension = 1024
    val scaledBitmap = if (width > maxDimension || height > maxDimension) {
        val aspectRatio = width.toFloat() / height.toFloat()
        val (newWidth, newHeight) = if (width > height) {
            Pair(maxDimension, (maxDimension / aspectRatio).toInt())
        } else {
            Pair((maxDimension * aspectRatio).toInt(), maxDimension)
        }
        Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
    } else {
        this
    }

    val outputStream = ByteArrayOutputStream()
    // Compress at 80% quality to optimize transfer size while keeping excellent text details for modern models
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    
    // Recycle newly created scaled down bitmap to free memory
    if (scaledBitmap != this) {
        scaledBitmap.recycle()
    }
    return base64String
}

object GeminiApiRepository {
    private fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    private suspend fun <T> retryOnRateLimit(block: suspend () -> T): T {
        var delayMs = 1500L
        for (attempt in 1..3) {
            try {
                return block()
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 429 && attempt < 3) {
                    kotlinx.coroutines.delay(delayMs)
                    delayMs *= 2
                } else {
                    throw e
                }
            } catch (e: Exception) {
                // If it's a generic network issue or timeout, retry as well if attempt < 3
                val errorMsg = e.localizedMessage ?: ""
                if ((errorMsg.contains("429") || errorMsg.contains("rate limit") || errorMsg.contains("too many requests")) && attempt < 3) {
                    kotlinx.coroutines.delay(delayMs)
                    delayMs *= 2
                } else {
                    throw e
                }
            }
        }
        return block()
    }

    /**
     * Extracts text from a scanned PDF page image using Gemini-3.5-Flash
     */
    suspend fun extractArabicUrduText(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Gemini API key is missing. Please set it in AI Studio secrets panel."
        }

        val base64Image = bitmap.toBase64()
        val prompt = "You are an expert OCR and book scanning assistant specializing in classical Arabic and Urdu scholarly texts. " +
                "Transcribe the text of the page line-by-line, and detect the 2D bounding box [ymin, xmin, ymax, xmax] of each text line. " +
                "These coordinates must be normalized integers scaled from 0 (top/left) to 1000 (bottom/right). " +
                "Return ONLY a valid JSON object matching the following structure. No explanation or notes, no conversational phrases. " +
                "JSON template:\n{\n  \"lines\": [\n    { \"text\": \"exact transcribed line content with correct diacritics/harakat\", \"box_2d\": [ymin, xmin, ymax, xmax] }\n  ]\n}"

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                    )
                )
            )
        )

        try {
            retryOnRateLimit {
                val response = RetrofitClient.service.generateContent(apiKey, request)
                if (response.error != null) {
                    if (response.error.code == 429) {
                        throw Exception("Rate limit error: 429 too many requests")
                    }
                    return@retryOnRateLimit "API Error (${response.error.code}): ${response.error.message}"
                }
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                text ?: "Unable to extract text from the image."
            }
        } catch (e: Exception) {
            "Network Error: ${e.localizedMessage ?: "Please try again later"}"
        }
    }

    /**
     * Explains a specific word/phrase (e.g. grammar synthetic logic like Nahw/Sarf)
     */
    suspend fun explainPhrase(phrase: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Gemini API key is missing."
        }

        val prompt = "Explain this classical Arabic/Urdu phrase specifically in a Madrassah research context: \"$phrase\". " +
                "Detail its lexical meaning, grammatical analysis (Sarf inflection or Nahw position if relevant), and how it is used in classical textbooks. Reply in clear Urdu or English."

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(text = prompt))
                )
            )
        )

        try {
            retryOnRateLimit {
                val response = RetrofitClient.service.generateContent(apiKey, request)
                if (response.error != null) {
                    if (response.error.code == 429) {
                        throw Exception("Rate limit error: 429 too many requests")
                    }
                    return@retryOnRateLimit "API Error (${response.error.code}): ${response.error.message}"
                }
                response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No explanation available."
            }
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }

    /**
     * Translates a selected block of text
     */
    suspend fun translateText(text: String, targetLanguage: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Gemini API key is missing."
        }

        val prompt = if (targetLanguage.equals("Urdu", ignoreCase = true)) {
            "You are an expert translator specializing in classical Islamic texts, Arabic grammar books, and Urdu commentary (Sharh). " +
            "Please translate the following classical text into clear, polished academic Urdu translation (اردو ترجمہ). " +
            "Ensure the translation is fluent, natural, uses standard Urdu characters/grammar, and maintains the sacred/scholarly tone " +
            "appropriate for contemporary Madrassah studies. Here is the text to translate:\n\n\"$text\""
        } else {
            "Translate this classical Arabic/Urdu text to $targetLanguage. Provide a faithful, " +
            "sentence-by-sentence translation suitable for academic research: \"$text\""
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(text = prompt))
                )
            )
        )

        try {
            retryOnRateLimit {
                val response = RetrofitClient.service.generateContent(apiKey, request)
                if (response.error != null) {
                    if (response.error.code == 429) {
                        throw Exception("Rate limit error: 429 too many requests")
                    }
                    return@retryOnRateLimit "API Error (${response.error.code}): ${response.error.message}"
                }
                response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No translation available."
            }
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }
}
