package com.agmente

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.agmente.appserverclient.*
import com.agmente.appserverclient.config.AppServerClientConfiguration
import com.agmente.appserverclient.config.AppServerConnectionState
import com.agmente.appserverclient.model.*
import com.agmente.appserverclient.websocket.JavaWebSocketProvider
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * End-to-end test against a running Codex app-server at localhost:8788.
 *
 * Prerequisites:
 *   - Codex app-server running: codex app-server --listen ws://127.0.0.1:8788
 *   - ADB reverse port forwarding: adb reverse tcp:8788 tcp:8788
 *
 * Run from host:
 *   cd AgmenteAndroid && ./gradlew :app:connectedDebugAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.class=com.agmente.CodexE2ETest
 */
@RunWith(AndroidJUnit4::class)
class CodexE2ETest {

    companion object {
        private const val TAG = "CodexE2ETest"
        private const val ENDPOINT = "ws://192.168.1.6:8788"
        private const val TIMEOUT_CONNECT_SEC = 10L
        private const val TIMEOUT_RESPONSE_SEC = 120L
        private const val TEST_PROMPT = "Say exactly: Hello from Agmente test"
    }

    @Test
    fun rawWebSocketConnects() {
        Log.i(TAG, "=== Raw Java-WebSocket test ===")

        val openLatch = CountDownLatch(1)
        val messageLatch = CountDownLatch(1)
        val errorRef = arrayOfNulls<Throwable>(1)
        val responseRef = arrayOfNulls<String>(1)

        val uri = java.net.URI(ENDPOINT)
        val ws = object : org.java_websocket.client.WebSocketClient(uri) {
            override fun onOpen(handshakedata: org.java_websocket.handshake.ServerHandshake?) {
                Log.i(TAG, "Raw WS opened")
                openLatch.countDown()
                val msg = """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"clientInfo":{"name":"RawTest","version":"1.0"}}}"""
                send(msg)
                Log.i(TAG, "Sent initialize")
            }

            override fun onMessage(message: String?) {
                Log.i(TAG, "Raw WS message: $message")
                responseRef[0] = message
                messageLatch.countDown()
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.i(TAG, "Raw WS closed: $code $reason remote=$remote")
            }

            override fun onError(ex: Exception?) {
                Log.e(TAG, "Raw WS error: ${ex?.message}", ex)
                errorRef[0] = ex
                openLatch.countDown()
                messageLatch.countDown()
            }
        }
        ws.connect()

        assertTrue("WS open timed out", openLatch.await(TIMEOUT_CONNECT_SEC, TimeUnit.SECONDS))
        assertNull("WS failed to connect: ${errorRef[0]?.message}", errorRef[0])

        assertTrue("No message received", messageLatch.await(TIMEOUT_CONNECT_SEC, TimeUnit.SECONDS))
        assertNotNull("Response was null", responseRef[0])
        Log.i(TAG, "Initialize response: ${responseRef[0]}")
        assertTrue("Response should contain result", responseRef[0]!!.contains("result"))

        ws.close()
        Log.i(TAG, "=== Raw Java-WebSocket test PASSED ===")
    }

    @Test
    fun fullCodexFlow() {
        runBlocking {
            Log.i(TAG, "=== Starting Codex E2E test ===")

            val client = AppServerClient(JavaWebSocketProvider())
            val service = AppServerService(client)
            val events = mutableListOf<AppServerEvent>()
            val turnCompletedLatch = CountDownLatch(1)
            val connectedLatch = CountDownLatch(1)
            val responseDelta = StringBuilder()

            val delegate = object : AppServerServiceDelegate {
                override fun onEvent(event: AppServerEvent) {
                    Log.i(TAG, "Event: ${event::class.simpleName}")
                    events.add(event)
                    when (event) {
                        is AppServerEvent.AgentMessageDelta -> {
                            responseDelta.append(event.delta)
                            Log.d(TAG, "Delta: ${event.delta}")
                        }
                        is AppServerEvent.TurnCompleted -> {
                            Log.i(TAG, "Turn completed for thread ${event.threadId}")
                            turnCompletedLatch.countDown()
                        }
                        is AppServerEvent.ApprovalRequested -> {
                            Log.i(TAG, "Auto-approving request ${event.requestId}")
                            runBlocking {
                                service.sendResponse(event.requestId, JSONValue.obj(mapOf(
                                    "approved" to JSONValue.bool(true)
                                )))
                            }
                        }
                        else -> {}
                    }
                }

                override fun onConnectionStateChanged(state: AppServerConnectionState) {
                    Log.i(TAG, "Connection state: ${state::class.simpleName}")
                    if (state is AppServerConnectionState.Connected) {
                        connectedLatch.countDown()
                    }
                }

                override fun onError(error: Throwable) {
                    Log.e(TAG, "Service error: ${error.message}", error)
                }
            }

            service.setDelegate(delegate)

            // Step 1: Connect
            Log.i(TAG, "--- Step 1: Connecting to $ENDPOINT ---")
            val config = AppServerClientConfiguration(endpoint = ENDPOINT)
            client.connect(config)
            assertTrue(
                "Connection timed out after ${TIMEOUT_CONNECT_SEC}s",
                connectedLatch.await(TIMEOUT_CONNECT_SEC, TimeUnit.SECONDS)
            )
            Log.i(TAG, "Connected successfully")

            // Step 2: Initialize
            Log.i(TAG, "--- Step 2: Initializing ---")
            val initPayload = AppServerInitializePayload(
                clientInfo = AppServerClientInfo("Agmente E2E Test", "1.0.0")
            )
            val initResponse = service.initialize(initPayload)
            val initResult = AppServerResponseParser.parseInitialize(initResponse)
            assertNotNull("Initialize returned null result", initResult)
            Log.i(TAG, "Initialized: server=${initResult!!.serverName}, version=${initResult.serverVersion}")

            service.sendNotification(AppServerMethods.INITIALIZED)

            // Step 3: Fetch models (optional, may not be supported)
            Log.i(TAG, "--- Step 3: Fetching models ---")
            var selectedModel: String? = null
            try {
                val modelResponse = service.modelList()
                Log.i(TAG, "model/list raw response: $modelResponse")
                val modelResult = AppServerResponseParser.parseModelList(modelResponse)
                if (modelResult != null) {
                    selectedModel = modelResult.defaultModel ?: modelResult.models.firstOrNull()?.id
                    Log.i(TAG, "Models: ${modelResult.models.map { it.id }}, default=${modelResult.defaultModel}")
                } else {
                    Log.w(TAG, "model/list returned null result, continuing without model selection")
                }
            } catch (e: Exception) {
                Log.w(TAG, "model/list failed (non-fatal): ${e.message}")
            }

            // Step 4: Create thread
            Log.i(TAG, "--- Step 4: Creating thread ---")
            val threadPayload = AppServerThreadStartPayload(
                cwd = null,
                model = selectedModel,
                persistExtendedHistory = false
            )
            val threadResponse = service.threadStart(threadPayload)
            Log.i(TAG, "thread/start raw response: $threadResponse")
            val thread = AppServerResponseParser.parseThreadStart(threadResponse)
            assertNotNull("thread/start returned null", thread)
            val threadId = thread!!.id
            Log.i(TAG, "Thread created: id=$threadId")

            // Step 5: Send prompt
            Log.i(TAG, "--- Step 5: Sending prompt: '$TEST_PROMPT' ---")
            val turnPayload = AppServerTurnStartPayload(
                threadId = threadId,
                input = listOf(AppServerUserInput.Text(TEST_PROMPT)),
                model = selectedModel,
                approvalPolicy = AppServerApprovalPolicy.NEVER,
                sandboxPolicy = AppServerSandboxPolicy(AppServerSandboxMode.DANGER_FULL_ACCESS)
            )
            service.turnStart(turnPayload)

            // Step 6: Wait for turn completion
            Log.i(TAG, "--- Step 6: Waiting for response (up to ${TIMEOUT_RESPONSE_SEC}s) ---")
            assertTrue(
                "Turn did not complete within ${TIMEOUT_RESPONSE_SEC}s",
                turnCompletedLatch.await(TIMEOUT_RESPONSE_SEC, TimeUnit.SECONDS)
            )

            val fullResponse = responseDelta.toString()
            Log.i(TAG, "Full response: $fullResponse")
            assertTrue("Response was empty", fullResponse.isNotBlank())

            // Step 7: Verify events
            val turnStartedEvents = events.filterIsInstance<AppServerEvent.TurnStarted>()
            val turnCompletedEvents = events.filterIsInstance<AppServerEvent.TurnCompleted>()
            val deltaEvents = events.filterIsInstance<AppServerEvent.AgentMessageDelta>()

            assertTrue("Expected at least one TurnStarted event", turnStartedEvents.isNotEmpty())
            assertTrue("Expected at least one TurnCompleted event", turnCompletedEvents.isNotEmpty())
            assertTrue("Expected at least one AgentMessageDelta event", deltaEvents.isNotEmpty())
            Log.i(TAG, "Events verified: ${turnStartedEvents.size} TurnStarted, " +
                    "${deltaEvents.size} deltas, ${turnCompletedEvents.size} TurnCompleted")

            // Step 8: Clean up
            Log.i(TAG, "--- Step 8: Disconnecting ---")
            client.disconnect()
            Log.i(TAG, "=== Codex E2E test PASSED ===")
        }
    }
}
