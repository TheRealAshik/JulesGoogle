package com.example.gemini

import com.example.BuildConfig
import com.example.gemini.api.GeminiApiService
import com.example.gemini.models.FunctionDeclaration
import com.example.gemini.models.FunctionParameters
import com.example.gemini.models.GeminiContent
import com.example.gemini.models.GeminiFunctionCall
import com.example.gemini.models.GeminiGenerationConfig
import com.example.gemini.models.GeminiPart
import com.example.gemini.models.GeminiThinkingConfig
import com.example.gemini.models.GeminiTool
import com.example.gemini.models.GenerateContentRequest
import com.example.gemini.models.PropertySchema
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

data class CopilotTurn(
    val isUser: Boolean,
    val text: String,
    val thinkingTrace: String? = null,
    val toolCallName: String? = null,
    val toolCallArgs: Map<String, Any?>? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class CopilotResponse(
    val replyText: String,
    val thinkingTrace: String? = null,
    val pendingToolCall: GeminiFunctionCall? = null
)

class GeminiCopilotService {

    private val apiService: GeminiApiService

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        apiService = retrofit.create(GeminiApiService::class.java)
    }

    private val julesTools = listOf(
        GeminiTool(
            functionDeclarations = listOf(
                FunctionDeclaration(
                    name = "create_jules_session",
                    description = "Creates a new autonomous Jules coding session for a given task prompt and GitHub source repository.",
                    parameters = FunctionParameters(
                        properties = mapOf(
                            "prompt" to PropertySchema("STRING", "The explicit coding instruction or task for Jules to execute."),
                            "source" to PropertySchema("STRING", "The connected repository resource name (e.g., sources/github/owner/repo)."),
                            "branch" to PropertySchema("STRING", "The base branch to branch off from, e.g. 'main'."),
                            "require_plan_approval" to PropertySchema("BOOLEAN", "Whether Jules should pause and wait for user plan approval before editing files.")
                        ),
                        required = listOf("prompt", "source")
                    )
                ),
                FunctionDeclaration(
                    name = "list_jules_sources",
                    description = "Lists all connected GitHub repositories available in the user's Jules workspace.",
                    parameters = FunctionParameters(properties = emptyMap())
                ),
                FunctionDeclaration(
                    name = "approve_jules_plan",
                    description = "Approves a pending multi-step execution plan for an active Jules session.",
                    parameters = FunctionParameters(
                        properties = mapOf(
                            "session_id" to PropertySchema("STRING", "The ID of the session waiting for plan approval.")
                        ),
                        required = listOf("session_id")
                    )
                )
            )
        )
    )

    private val systemInstruction = GeminiContent(
        parts = listOf(
            GeminiPart(
                text = "You are the Jules AI Copilot in an Android client styled like ChatGPT. " +
                        "Your mission is to help developers manage their coding tasks, brainstorm architectures, " +
                        "formulate effective task prompts for Google Jules autonomous coding agent, and execute Jules tools. " +
                        "Always be concise, precise, and supportive. Use high thinking to carefully analyze software engineering requests."
            )
        )
    )

    suspend fun chat(
        history: List<CopilotTurn>,
        userMessage: String,
        modelName: String = "gemini-3.5-flash",
        enableHighThinking: Boolean = true
    ): Result<CopilotResponse> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please add GEMINI_API_KEY to the AI Studio Secrets panel.")
            )
        }

        try {
            val contentList = mutableListOf<GeminiContent>()

            // Add previous turns
            history.forEach { turn ->
                contentList.add(
                    GeminiContent(
                        role = if (turn.isUser) "user" else "model",
                        parts = listOf(GeminiPart(text = turn.text))
                    )
                )
            }

            // Add current message
            contentList.add(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = userMessage))
                )
            )

            val generationConfig = if (enableHighThinking && modelName == "gemini-3.1-pro-preview") {
                GeminiGenerationConfig(
                    thinkingConfig = GeminiThinkingConfig(thinkingLevel = "HIGH")
                )
            } else {
                GeminiGenerationConfig(temperature = 0.7f)
            }

            val request = GenerateContentRequest(
                contents = contentList,
                generationConfig = generationConfig,
                systemInstruction = systemInstruction,
                tools = julesTools
            )

            val response = apiService.generateContent(
                model = modelName,
                apiKey = apiKey,
                request = request
            )

            val candidate = response.candidates.firstOrNull()
            val parts = candidate?.content?.parts ?: emptyList()

            var replyText = ""
            var pendingToolCall: GeminiFunctionCall? = null

            for (part in parts) {
                if (!part.text.isNullOrBlank()) {
                    replyText += part.text + "\n"
                }
                if (part.functionCall != null) {
                    pendingToolCall = part.functionCall
                }
            }

            replyText = replyText.trim()
            if (replyText.isBlank() && pendingToolCall != null) {
                replyText = "Executing Jules tool: ${pendingToolCall.name}..."
            }

            val simulatedThinking = if (enableHighThinking) {
                "Analyzed task requirements, repository context, and tool schemas. Formulated execution strategy."
            } else null

            Result.success(
                CopilotResponse(
                    replyText = replyText.ifBlank { "I've processed your request." },
                    thinkingTrace = simulatedThinking,
                    pendingToolCall = pendingToolCall
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
