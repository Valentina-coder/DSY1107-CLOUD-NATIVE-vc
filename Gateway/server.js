import http from "node:http";
import httpProxy from "http-proxy";

const gatewayPort = Number(process.env.GATEWAY_PORT || 8081);
const backendTarget = process.env.BACKEND_URL || "http://localhost:8080";
const proxy = httpProxy.createProxyServer({ target: backendTarget });

const allowedOrigins = new Set([
  "http://localhost:5173",
  "http://127.0.0.1:5173",
]);

const server = http.createServer((request, response) => {
  const origin = request.headers.origin;
  if (allowedOrigins.has(origin)) {
    response.setHeader("Access-Control-Allow-Origin", origin);
    response.setHeader("Access-Control-Allow-Credentials", "true");
    response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
    response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
  }

  if (request.method === "OPTIONS") {
    response.writeHead(204);
    response.end();
    return;
  }

  if (!request.url.startsWith("/api/")) {
    response.writeHead(404, { "Content-Type": "application/json" });
    response.end(JSON.stringify({ error: "Gateway route not found" }));
    return;
  }

  proxy.web(request, response, { target: backendTarget }, (error) => {
    console.error("Gateway proxy error:", error.message);
    if (!response.headersSent) {
      response.writeHead(502, { "Content-Type": "application/json" });
    }
    response.end(JSON.stringify({ error: "Backend unavailable" }));
  });
});

server.listen(gatewayPort, () => {
  console.log(`Stock360 Gateway running at http://localhost:${gatewayPort}`);
  console.log(`Proxy target: ${backendTarget}`);
});
