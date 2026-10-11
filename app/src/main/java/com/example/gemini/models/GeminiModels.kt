package com.example.gemini.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null,
    @Json(name = "tools") val tools: List<GeminiTool>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "role") val role: String? = null,
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null,
    @Json(name = "functionCall") val functionCall: GeminiFunctionCall? = null,
    @Json(name = "functionResponse") val functionResponse: GeminiFunctionResponse? = null
)

@JsonClass(generateAdapter = true)
data class GeminiFunctionCall(
    @Json(name = "name") val name: String,
    @Json(name = "args") val args: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiFunctionResponse(
    @Json(name = "name") val name: String,
    @Json(name = "response") val response: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiThinkingConfig(
    @Json(name = "thinkingLevel") val thinkingLevel: String = "HIGH"
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "topP") val topP: Float? = null,
    @Json(name = "thinkingConfig") val thinkingConfig: GeminiThinkingConfig? = null
)

@JsonClass(generateAdapter = true)
data class PropertySchema(
    @Json(name = "type") val type: String,
    @Json(name = "description") val description: String
)

@JsonClass(generateAdapter = true)
data class FunctionParameters(
    @Json(name = "type") val type: String = "OBJECT",
    @Json(name = "properties") val properties: Map<String, PropertySchema> = emptyMap(),
    @Json(name = "required") val required: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class FunctionDeclaration(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String,
    @Json(name = "parameters") val parameters: FunctionParameters? = null
)

@JsonClass(generateAdapter = true)
data class GeminiTool(
    @Json(name = "functionDeclarations") val functionDeclarations: List<FunctionDeclaration>
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null,
    @Json(name = "finishReason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate> = emptyList()
)
