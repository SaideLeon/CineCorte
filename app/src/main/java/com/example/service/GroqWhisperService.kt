package com.example.service

import com.example.model.GroqTranscriptionResponse
import com.example.model.WordTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Serviço de comunicação com a API de Transcrição Whisper da Groq (whisper-large-v3-turbo).
 * Fornece marcações temporais com granularidade ao nível de cada palavra individual.
 */
class GroqWhisperService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Testa se a chave da API fornecida pelo utilizador é válida.
     */
    suspend fun testApiKey(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("A chave da API está vazia."))
        }
        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/models")
            .addHeader("Authorization", "Bearer ${apiKey.trim()}")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> Result.success("Ligação bem-sucedida! Acesso à Groq API confirmado.")
                    401 -> Result.failure(Exception("Chave da API inválida ou não autorizada (401). Verifica a chave em console.groq.com."))
                    429 -> Result.failure(Exception("Limite de taxa (429) atingido na Groq. Tenta novamente dentro de instantes."))
                    else -> Result.failure(Exception("Erro na resposta da Groq (${response.code}): ${response.message}"))
                }
            }
        } catch (e: IOException) {
            Result.failure(Exception("Sem ligação à internet ou tempo esgotado. Verifica a tua rede."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Transcreve um ficheiro de áudio com granularidade temporal por palavra.
     */
    suspend fun transcribeAudio(
        apiKey: String,
        audioFile: File,
        language: String = "pt",
        temperature: Float = 0f
    ): Result<GroqTranscriptionResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Chave da API da Groq não configurada. Abre as Definições para adicionar."))
        }
        if (!audioFile.exists() || audioFile.length() == 0L) {
            return@withContext Result.failure(IllegalArgumentException("Ficheiro de áudio vazio ou inexistente."))
        }

        val mediaType = "audio/mp4".toMediaTypeOrNull()
        val fileBody = audioFile.asRequestBody(mediaType)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("model", "whisper-large-v3-turbo")
            .addFormDataPart("response_format", "verbose_json")
            .addFormDataPart("timestamp_granularities[]", "word")
            .addFormDataPart("temperature", temperature.toString())
            .addFormDataPart("language", language)
            .addFormDataPart("file", audioFile.name, fileBody)
            .build()

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/audio/transcriptions")
            .addHeader("Authorization", "Bearer ${apiKey.trim()}")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val errorMsg = when (response.code) {
                        401 -> "Chave da API da Groq inválida ou expirada (401). Abre as Definições para corrigir."
                        429 -> "Limite de pedidos da Groq atingido (429). Aguarda alguns segundos e tenta de novo."
                        413 -> "Ficheiro de áudio demasiado grande para envio direto."
                        else -> "Erro na transcrição Groq (${response.code}): ${parseErrorMessage(bodyString, response.message)}"
                    }
                    return@withContext Result.failure(Exception(errorMsg))
                }

                // Parser do JSON de resposta verbose_json
                val json = JSONObject(bodyString)
                val task = json.optString("task", "transcribe")
                val lang = json.optString("language", "Portuguese")
                val duration = json.optDouble("duration", 0.0)
                val text = json.optString("text", "")

                val wordsList = mutableListOf<WordTimestamp>()
                val wordsArray = json.optJSONArray("words")
                if (wordsArray != null) {
                    for (i in 0 until wordsArray.length()) {
                        val wObj = wordsArray.getJSONObject(i)
                        val word = wObj.optString("word", "")
                        val start = wObj.optDouble("start", 0.0)
                        val end = wObj.optDouble("end", start + 0.1)
                        if (word.isNotBlank()) {
                            wordsList.add(WordTimestamp(word = word.trim(), start = start, end = end))
                        }
                    }
                }

                Result.success(
                    GroqTranscriptionResponse(
                        task = task,
                        language = lang,
                        duration = duration,
                        text = text,
                        words = wordsList
                    )
                )
            }
        } catch (e: IOException) {
            Result.failure(Exception("Falha de rede ao contactar a API da Groq. Verifica a tua ligação à internet."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(jsonStr: String, defaultMsg: String): String {
        return try {
            val root = JSONObject(jsonStr)
            val errObj = root.optJSONObject("error")
            errObj?.optString("message") ?: defaultMsg
        } catch (e: Exception) {
            defaultMsg
        }
    }
}
