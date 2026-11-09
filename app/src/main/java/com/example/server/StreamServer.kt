package com.example.server

import android.util.Log
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

/**
 * Serveur HTTP & WebSocket embarqué minimaliste et ultra-performant.
 * - Diffusion en temps réel vers OBS Studio / vMix
 * - Page Overlay HTML/JS transparente servie sur http://<ip_table>:8080/
 * - Flux WebSocket sur ws://<ip_table>:8080/ws
 */
class StreamServer(private val port: Int = 8080) {

    private var serverSocket: ServerSocket? = null
    @Volatile private var isRunning = false
    private val clientSockets = CopyOnWriteArrayList<Socket>()
    @Volatile private var currentMatchJson: String = "{}"

    fun start(initialStateJson: String = "{}") {
        if (isRunning) return
        this.currentMatchJson = initialStateJson
        isRunning = true

        thread(name = "StreamServerThread", isDaemon = true) {
            try {
                serverSocket = ServerSocket(port)
                Log.d("StreamServer", "Serveur démarré sur le port $port")
                while (isRunning) {
                    val socket = serverSocket?.accept() ?: break
                    thread(name = "ClientHandler-${socket.remoteSocketAddress}") {
                        handleClient(socket)
                    }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e("StreamServer", "Erreur serveur socket", e)
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            for (socket in clientSockets) {
                socket.close()
            }
            clientSockets.clear()
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            Log.e("StreamServer", "Erreur arrêt serveur", e)
        }
    }

    fun broadcastState(jsonState: String) {
        this.currentMatchJson = jsonState
        val payload = encodeWebSocketTextFrame(jsonState)
        val iterator = clientSockets.iterator()
        while (iterator.hasNext()) {
            val socket = iterator.next()
            try {
                if (socket.isClosed || !socket.isConnected) {
                    clientSockets.remove(socket)
                    continue
                }
                val out = socket.getOutputStream()
                out.write(payload)
                out.flush()
            } catch (e: Exception) {
                clientSockets.remove(socket)
                try { socket.close() } catch (_: Exception) {}
            }
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            val input = socket.getInputStream()
            val output = socket.getOutputStream()
            val headerString = readHeaderString(input) ?: run {
                socket.close()
                return
            }

            if (headerString.contains("GET /ws") && headerString.contains("Upgrade: websocket")) {
                // WebSocket Handshake
                val key = extractHeader(headerString, "Sec-WebSocket-Key") ?: run {
                    socket.close()
                    return
                }
                val acceptKey = computeWebSocketAccept(key)
                val handshakeResponse = "HTTP/1.1 101 Switching Protocols\r\n" +
                        "Upgrade: websocket\r\n" +
                        "Connection: Upgrade\r\n" +
                        "Sec-WebSocket-Accept: $acceptKey\r\n\r\n"
                output.write(handshakeResponse.toByteArray(Charsets.UTF_8))
                output.flush()

                clientSockets.add(socket)
                // Envoyer immédiatement l'état actuel au nouveau client connecté
                output.write(encodeWebSocketTextFrame(currentMatchJson))
                output.flush()

                // Rester en écoute sur le socket pour détecter la déconnexion
                val buffer = ByteArray(1024)
                while (isRunning && !socket.isClosed) {
                    val read = input.read(buffer)
                    if (read == -1) break
                }
                clientSockets.remove(socket)
                socket.close()
            } else if (headerString.contains("GET /api/state")) {
                val bodyBytes = currentMatchJson.toByteArray(Charsets.UTF_8)
                val response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/json; charset=utf-8\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Content-Length: ${bodyBytes.size}\r\n\r\n"
                output.write(response.toByteArray(Charsets.UTF_8))
                output.write(bodyBytes)
                output.flush()
                socket.close()
            } else {
                // Servir la page HTML de l'Overlay OBS
                val html = getOverlayHtmlPage()
                val htmlBytes = html.toByteArray(Charsets.UTF_8)
                val response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html; charset=utf-8\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Content-Length: ${htmlBytes.size}\r\n\r\n"
                output.write(response.toByteArray(Charsets.UTF_8))
                output.write(htmlBytes)
                output.flush()
                socket.close()
            }
        } catch (e: Exception) {
            clientSockets.remove(socket)
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun readHeaderString(input: InputStream): String? {
        val sb = StringBuilder()
        val buffer = ByteArray(1024)
        while (true) {
            val bytesRead = input.read(buffer)
            if (bytesRead <= 0) break
            sb.append(String(buffer, 0, bytesRead, Charsets.UTF_8))
            if (sb.contains("\r\n\r\n")) break
            if (sb.length > 8192) break
        }
        return if (sb.isNotEmpty()) sb.toString() else null
    }

    private fun extractHeader(headers: String, key: String): String? {
        val lines = headers.split("\r\n")
        for (line in lines) {
            if (line.startsWith("$key:", ignoreCase = true)) {
                return line.substringAfter(":").trim()
            }
        }
        return null
    }

    private fun computeWebSocketAccept(key: String): String {
        val magic = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
        val md = MessageDigest.getInstance("SHA-1")
        val hashed = md.digest((key + magic).toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hashed)
    }

    private fun encodeWebSocketTextFrame(message: String): ByteArray {
        val rawData = message.toByteArray(Charsets.UTF_8)
        val length = rawData.size

        val frameHeader: ByteArray
        if (length <= 125) {
            frameHeader = ByteArray(2)
            frameHeader[0] = 0x81.toByte() // FIN + Text Frame
            frameHeader[1] = length.toByte()
        } else if (length <= 65535) {
            frameHeader = ByteArray(4)
            frameHeader[0] = 0x81.toByte()
            frameHeader[1] = 126.toByte()
            frameHeader[2] = (length shr 8 and 0xFF).toByte()
            frameHeader[3] = (length and 0xFF).toByte()
        } else {
            frameHeader = ByteArray(10)
            frameHeader[0] = 0x81.toByte()
            frameHeader[1] = 127.toByte()
            for (i in 0..7) {
                frameHeader[2 + i] = (length.toLong() shr ((7 - i) * 8) and 0xFF).toByte()
            }
        }

        val result = ByteArray(frameHeader.size + rawData.size)
        System.arraycopy(frameHeader, 0, result, 0, frameHeader.size)
        System.arraycopy(rawData, 0, result, frameHeader.size, rawData.size)
        return result
    }

    /**
     * Modèle de page Overlay HTML/JS professionnelle pour OBS Studio / vMix
     */
    private fun getOverlayHtmlPage(): String {
        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Overlay OBS - Billard Carambole</title>
                <style>
                    @import url('https://fonts.googleapis.com/css2?family=Outfit:wght@400;600;800;900&display=swap');
                    
                    * {
                        box-sizing: border-box;
                        margin: 0;
                        padding: 0;
                    }
                    body {
                        background: transparent;
                        font-family: 'Outfit', sans-serif;
                        color: #ffffff;
                        overflow: hidden;
                        display: flex;
                        justify-content: center;
                        align-items: flex-end;
                        height: 100vh;
                        padding-bottom: 24px;
                    }

                    .scoreboard-overlay {
                        display: flex;
                        align-items: stretch;
                        background: rgba(11, 15, 25, 0.92);
                        backdrop-filter: blur(12px);
                        border: 2px solid rgba(255, 255, 255, 0.15);
                        border-radius: 16px;
                        box-shadow: 0 16px 40px rgba(0, 0, 0, 0.6);
                        overflow: hidden;
                        min-width: 820px;
                        position: relative;
                    }

                    .player-card {
                        flex: 1;
                        padding: 14px 24px;
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        position: relative;
                        transition: all 0.3s ease;
                    }

                    .player-card.active {
                        background: linear-gradient(135deg, rgba(16, 185, 129, 0.25) 0%, rgba(13, 92, 58, 0.4) 100%);
                        border-bottom: 4px solid #10B981;
                    }

                    .player-info {
                        display: flex;
                        align-items: center;
                        gap: 14px;
                    }

                    .ball-indicator {
                        width: 22px;
                        height: 22px;
                        border-radius: 50%;
                        border: 2px solid rgba(255, 255, 255, 0.8);
                        box-shadow: 0 2px 8px rgba(0,0,0,0.4);
                    }

                    .player-name {
                        font-size: 22px;
                        font-weight: 800;
                        letter-spacing: 0.5px;
                        text-transform: uppercase;
                        max-width: 180px;
                        white-space: nowrap;
                        overflow: hidden;
                        text-overflow: ellipsis;
                    }

                    .player-stats {
                        font-size: 13px;
                        color: #94A3B8;
                        font-weight: 600;
                        display: flex;
                        gap: 10px;
                        margin-top: 2px;
                    }

                    .player-score {
                        font-size: 52px;
                        font-weight: 900;
                        line-height: 1;
                        font-variant-numeric: tabular-nums;
                    }

                    .run-badge {
                        background: #F59E0B;
                        color: #000000;
                        font-weight: 900;
                        font-size: 16px;
                        padding: 4px 10px;
                        border-radius: 8px;
                        margin-left: 12px;
                        animation: pulse 1.5s infinite;
                    }

                    .center-divider {
                        width: 140px;
                        background: rgba(18, 24, 36, 0.95);
                        border-left: 1px solid rgba(255, 255, 255, 0.1);
                        border-right: 1px solid rgba(255, 255, 255, 0.1);
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        padding: 8px 12px;
                    }

                    .game-mode-label {
                        font-size: 11px;
                        font-weight: 800;
                        color: #10B981;
                        text-transform: uppercase;
                        letter-spacing: 1px;
                    }

                    .innings-display {
                        font-size: 14px;
                        font-weight: 700;
                        color: #CBD5E1;
                        margin: 2px 0;
                    }

                    .timer-display {
                        font-size: 20px;
                        font-weight: 900;
                        color: #F59E0B;
                        background: rgba(0, 0, 0, 0.4);
                        padding: 2px 10px;
                        border-radius: 20px;
                    }

                    .timer-display.low {
                        color: #EF4444;
                        animation: flash 0.6s infinite alternate;
                    }

                    @keyframes pulse {
                        0% { transform: scale(1); }
                        50% { transform: scale(1.08); }
                        100% { transform: scale(1); }
                    }

                    @keyframes flash {
                        from { opacity: 1; }
                        to { opacity: 0.3; }
                    }
                </style>
            </head>
            <body>

            <div class="scoreboard-overlay" id="overlay">
                <!-- Joueur A -->
                <div class="player-card" id="cardA">
                    <div class="player-info">
                        <div class="ball-indicator" id="ballA" style="background: #FFFFFF;"></div>
                        <div>
                            <div class="player-name" id="nameA">Joueur A</div>
                            <div class="player-stats">
                                <span>MOY: <b id="avgA">0.00</b></span>
                                <span>SÉR: <b id="highA">0</b></span>
                            </div>
                        </div>
                    </div>
                    <div style="display: flex; align-items: center;">
                        <div class="player-score" id="scoreA">0</div>
                        <div class="run-badge" id="runA" style="display: none;">+0</div>
                    </div>
                </div>

                <!-- Centre -->
                <div class="center-divider">
                    <div class="game-mode-label" id="gameMode">3-BANDES</div>
                    <div class="innings-display">REP: <span id="inningsCount">0</span> / <span id="targetScore">40</span></div>
                    <div class="timer-display" id="timer">40s</div>
                </div>

                <!-- Joueur B -->
                <div class="player-card" id="cardB">
                    <div style="display: flex; align-items: center;">
                        <div class="run-badge" id="runB" style="display: none;">+0</div>
                        <div class="player-score" id="scoreB">0</div>
                    </div>
                    <div class="player-info">
                        <div>
                            <div class="player-name" id="nameB" style="text-align: right;">Joueur B</div>
                            <div class="player-stats" style="justify-content: flex-end;">
                                <span>SÉR: <b id="highB">0</b></span>
                                <span>MOY: <b id="avgB">0.00</b></span>
                            </div>
                        </div>
                        <div class="ball-indicator" id="ballB" style="background: #FACC15;"></div>
                    </div>
                </div>
            </div>

            <script>
                function connectWS() {
                    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
                    const wsUrl = protocol + '//' + window.location.host + '/ws';
                    const ws = new WebSocket(wsUrl);

                    ws.onmessage = function(event) {
                        try {
                            const state = JSON.parse(event.data);
                            updateUI(state);
                        } catch(e) {
                            console.error('Erreur JSON:', e);
                        }
                    };

                    ws.onclose = function() {
                        setTimeout(connectWS, 2000);
                    };

                    ws.onerror = function() {
                        ws.close();
                    };
                }

                function updateUI(s) {
                    document.getElementById('nameA').textContent = s.a_name || 'Joueur A';
                    document.getElementById('scoreA').textContent = s.a_score;
                    document.getElementById('ballA').style.background = s.a_color || '#FFFFFF';
                    const avgA = s.a_innings > 0 ? (s.a_score / s.a_innings).toFixed(2) : '0.00';
                    document.getElementById('avgA').textContent = avgA;
                    document.getElementById('highA').textContent = s.a_high_run;

                    document.getElementById('nameB').textContent = s.b_name || 'Joueur B';
                    document.getElementById('scoreB').textContent = s.b_score;
                    document.getElementById('ballB').style.background = s.b_color || '#FACC15';
                    const avgB = s.b_innings > 0 ? (s.b_score / s.b_innings).toFixed(2) : '0.00';
                    document.getElementById('avgB').textContent = avgB;
                    document.getElementById('highB').textContent = s.b_high_run;

                    document.getElementById('gameMode').textContent = s.game_mode || 'CARAMBOLE';
                    document.getElementById('inningsCount').textContent = Math.max(s.a_innings, s.b_innings);
                    document.getElementById('targetScore').textContent = s.target_score;

                    const timerEl = document.getElementById('timer');
                    timerEl.textContent = s.timer_remaining + 's';
                    if (s.timer_remaining <= 10) {
                        timerEl.classList.add('low');
                    } else {
                        timerEl.classList.remove('low');
                    }

                    const cardA = document.getElementById('cardA');
                    const cardB = document.getElementById('cardB');
                    const runA = document.getElementById('runA');
                    const runB = document.getElementById('runB');

                    if (s.current_turn === 'PLAYER_A') {
                        cardA.classList.add('active');
                        cardB.classList.remove('active');
                        if (s.a_current_run > 0) {
                            runA.style.display = 'inline-block';
                            runA.textContent = '+' + s.a_current_run;
                        } else {
                            runA.style.display = 'none';
                        }
                        runB.style.display = 'none';
                    } else {
                        cardB.classList.add('active');
                        cardA.classList.remove('active');
                        if (s.b_current_run > 0) {
                            runB.style.display = 'inline-block';
                            runB.textContent = '+' + s.b_current_run;
                        } else {
                            runB.style.display = 'none';
                        }
                        runA.style.display = 'none';
                    }
                }

                connectWS();
            </script>
            </body>
            </html>
        """.trimIndent()
    }
}
