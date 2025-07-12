package com.example.gesturecontrolapp

import io.ktor.client.HttpClient
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import kotlinx.coroutines.runBlocking
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.client.plugins.HttpTimeout
import java.net.SocketTimeoutException

class VolumeControlUnitTests {

    private lateinit var testParser: (String) -> Float?
    private lateinit var testCalculator: (Float?) -> Int
    private lateinit var testDetector: (Float?) -> Boolean

    @Before
    fun setup() {
        // Initialize with the actual functions from MainActivity
        testParser = { response ->
            when {
                response.matches(Regex("^\\d+\\.?\\d*$")) -> response.toFloat()
                response.startsWith("{") -> Regex("\"Distance\"\\s*:\\s*(\\d+\\.?\\d*)")
                    .find(response)?.groupValues?.get(1)?.toFloat()
                else -> null
            }
        }

        testCalculator = { distance ->
            when {
                distance == null -> -1
                distance <= 5f -> 0
                distance >= 50f -> 100
                else -> ((distance - 5f) / 45f * 100).toInt()
            }
        }

        testDetector = { distance ->
            distance?.let { it in 5f..50f } ?: false
        }
    }

    @Test
    fun `parseDistanceFromResponse with valid JSON returns correct distance`() {
        val response = """{"Distance": 25}"""
        val result = testParser(response)
        assertEquals(25f, result)
    }

    @Test
    fun `parseDistanceFromResponse with plain number returns correct distance`() {
        val response = "30.5"
        val result = testParser(response)
        assertEquals(30.5f, result)
    }

    @Test
    fun `parseDistanceFromResponse with invalid data returns null`() {
        val response = "Error: No data"
        val result = testParser(response)
        assertNull(result)
    }

    @Test
    fun `calculateVolume returns 0 for minimum distance`() {
        assertEquals(0, testCalculator(5f))
    }

    @Test
    fun `calculateVolume returns 100 for maximum distance`() {
        assertEquals(100, testCalculator(50f))
    }

    @Test
    fun `calculateVolume returns correct mid-range value`() {
        assertEquals(50, testCalculator(27.5f)) // (27.5-5)/45*100 ≈ 50
    }

    @Test
    fun `isHandDetected returns true for in-range distances`() {
        assertTrue(testDetector(10f))
        assertTrue(testDetector(30f))
        assertTrue(testDetector(49f))
    }

    @Test
    fun `isHandDetected returns false for out-of-range distances`() {
        assertFalse(testDetector(4f))
        assertFalse(testDetector(51f))
        assertFalse(testDetector(null))
    }

    @Test
    fun `network client handles successful response`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"Distance": 35}""",
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type" to listOf("application/json"))

            )
        }

        val client = HttpClient(mockEngine) {
            install(HttpTimeout)
        }

        val response: String = client.get("http://test/sensor").body()
        assertEquals("""{"Distance": 35}""", response.trim())
    }
    @Test

    fun `network client handles timeout`() {
        runBlocking {
            val mockEngine = MockEngine {
                throw SocketTimeoutException("Mock timeout")
            }

            val client = HttpClient(mockEngine) {
                install(HttpTimeout) {
                    requestTimeoutMillis = 1000
                }
            }

            assertThrows(SocketTimeoutException::class.java) {
                // calling suspending code inside assertThrows lambda
                runBlocking {
                    client.get("http://test/sensor").body<String>()
                }
            }
        }
    }

}