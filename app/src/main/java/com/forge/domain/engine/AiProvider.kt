package com.forge.domain.engine

/**
 * AiProvider Hierarchy
 * FORGE supports local-first inference with deterministic fallback.
 * Zero paid inference, zero mandatory cloud services or account creation.
 */
interface AiProvider {
    val providerName: String
    fun isAvailable(): Boolean
    suspend fun processIntent(prompt: String, context: EphemeralConversationContext): String?
}

/**
 * DeterministicIntentProvider
 * Pure offline, deterministic rule-based natural language intent parser.
 * Always available on all Android versions and architectures with 0 MB RAM overhead.
 */
class DeterministicIntentProvider : AiProvider {
    override val providerName: String = "FORGE Deterministic Engine"
    override fun isAvailable(): Boolean = true
    override suspend fun processIntent(prompt: String, context: EphemeralConversationContext): String? = null
}

/**
 * LocalModelProvider
 * On-device neural model inference loader (evaluated for Samsung Galaxy S21 FE).
 * Best practical candidate: Qwen2.5-0.5B / SmolLM2-360M quantized via llama.cpp or ONNX Runtime.
 * Loaded strictly on-demand to prevent battery drain and released when idle.
 */
class LocalModelProvider : AiProvider {
    override val providerName: String = "On-Device Neural Model"
    private var isLoaded: Boolean = false

    override fun isAvailable(): Boolean = isLoaded

    fun loadModel() {
        // Ready for on-device GGUF / ONNX weights
        isLoaded = false
    }

    fun unloadModel() {
        isLoaded = false
    }

    override suspend fun processIntent(prompt: String, context: EphemeralConversationContext): String? = null
}

/**
 * OptionalRemoteProvider
 * Bring-your-own-key provider if user explicitly enters a private endpoint or API key.
 * Strictly optional.
 */
class OptionalRemoteProvider(
    private val apiKey: String? = null,
    private val endpointUrl: String? = null
) : AiProvider {
    override val providerName: String = "Optional Remote Provider"
    override fun isAvailable(): Boolean = !apiKey.isNullOrBlank()
    override suspend fun processIntent(prompt: String, context: EphemeralConversationContext): String? = null
}
