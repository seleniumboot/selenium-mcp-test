package com.saddy.mcptest.client;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimal MCP client that communicates with a Python MCP server over stdio.
 * Handles the JSON-RPC 2.0 protocol used by the MCP spec (newline-delimited JSON).
 */
public class McpStdioClient implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(McpStdioClient.class);
    public static final ObjectMapper MAPPER = new ObjectMapper();

    private final Process process;
    private final PrintWriter writer;
    private final AtomicInteger idCounter = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();
    private final ExecutorService readerThread = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "mcp-reader");
        t.setDaemon(true);
        return t;
    });

    /**
     * Start the MCP server at the given path using the given Python command.
     *
     * @param pythonCmd  Python executable (e.g. "python", "python3", "py")
     * @param serverPath Absolute path to server.py
     */
    public McpStdioClient(String pythonCmd, String serverPath) throws Exception {
        Path srcDir = Path.of(serverPath).getParent();

        ProcessBuilder pb = new ProcessBuilder(pythonCmd, serverPath);
        pb.directory(srcDir.toFile());
        pb.environment().put("PYTHONUNBUFFERED", "1");

        process = pb.start();
        writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(process.getOutputStream())), true);

        startStderrDrain();
        readerThread.submit(() -> readLoop(process.getInputStream()));

        // Give the server a moment to initialize its event loop before handshake
        Thread.sleep(800);
        handshake();
    }

    // ------------------------------------------------------------------ //
    //  Public API                                                          //
    // ------------------------------------------------------------------ //

    /** Call a tool by name with given arguments and return the full result node. */
    public JsonNode callTool(String name, ObjectNode args) throws Exception {
        ObjectNode params = MAPPER.createObjectNode();
        params.put("name", name);
        params.set("arguments", args);

        JsonNode resp = request("tools/call", params);
        if (resp.has("error")) {
            throw new RuntimeException("Tool '" + name + "' failed: " + resp.get("error").toPrettyString());
        }
        return resp.path("result");
    }

    /** Convenience: call a tool and return the first text content item. */
    public String callToolText(String name, ObjectNode args) throws Exception {
        JsonNode result = callTool(name, args);
        JsonNode content = result.path("content");
        if (content.isArray() && !content.isEmpty()) {
            return content.get(0).path("text").asText();
        }
        return result.asText();
    }

    /** List all tools exposed by the server. */
    public JsonNode listTools() throws Exception {
        JsonNode resp = request("tools/list", MAPPER.createObjectNode());
        return resp.path("result").path("tools");
    }

    /** Create a fresh empty arguments node. */
    public ObjectNode args() {
        return MAPPER.createObjectNode();
    }

    // ------------------------------------------------------------------ //
    //  Internal                                                            //
    // ------------------------------------------------------------------ //

    private void handshake() throws Exception {
        ObjectNode params = MAPPER.createObjectNode();
        params.put("protocolVersion", "2025-03-26");
        params.putObject("clientInfo")
              .put("name", "selenium-mcp-java-test")
              .put("version", "1.0.0");
        params.putObject("capabilities");

        JsonNode resp = request("initialize", params);
        if (resp.has("error")) {
            throw new RuntimeException("MCP initialization failed: " + resp.get("error"));
        }
        sendNotification("notifications/initialized");
        log.info("MCP handshake OK — server: {}", resp.path("result").path("serverInfo"));
    }

    private JsonNode request(String method, JsonNode params) throws Exception {
        int id = idCounter.getAndIncrement();
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pending.put(id, future);

        ObjectNode req = MAPPER.createObjectNode();
        req.put("jsonrpc", "2.0");
        req.put("id", id);
        req.put("method", method);
        req.set("params", params);

        String json = MAPPER.writeValueAsString(req);
        log.debug("→ {}", json);
        writer.println(json);

        try {
            return future.get(60, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            pending.remove(id);
            throw new RuntimeException("Timeout waiting for MCP response to: " + method, e);
        }
    }

    private void sendNotification(String method) throws Exception {
        ObjectNode msg = MAPPER.createObjectNode();
        msg.put("jsonrpc", "2.0");
        msg.put("method", method);
        writer.println(MAPPER.writeValueAsString(msg));
    }

    private void readLoop(InputStream in) {
        try (JsonParser parser = MAPPER.getFactory().createParser(in)) {
            while (!Thread.currentThread().isInterrupted()) {
                JsonNode msg = MAPPER.readTree(parser);
                if (msg == null || msg.isMissingNode()) break;

                log.debug("← {}", msg);

                if (msg.has("id")) {
                    int id = msg.get("id").asInt();
                    CompletableFuture<JsonNode> f = pending.remove(id);
                    if (f != null) f.complete(msg);
                }
                // notifications (no id) are silently ignored
            }
        } catch (Exception e) {
            if (!Thread.currentThread().isInterrupted()) {
                log.warn("MCP reader stopped: {}", e.getMessage());
            }
        } finally {
            pending.values().forEach(f ->
                f.completeExceptionally(new IOException("MCP server stream closed")));
        }
    }

    private void startStderrDrain() {
        Thread t = new Thread(() -> {
            try (BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = err.readLine()) != null) {
                    log.debug("[MCP stderr] {}", line);
                }
            } catch (IOException ignored) {}
        }, "mcp-stderr");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void close() {
        try { writer.close(); } catch (Exception ignored) {}
        try { process.waitFor(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        process.destroyForcibly();
        readerThread.shutdownNow();
    }
}
