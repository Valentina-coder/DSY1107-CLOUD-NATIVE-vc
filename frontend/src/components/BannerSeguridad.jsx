import React from "react";
import { useAuth0 } from "@auth0/auth0-react";

export function BannerSeguridad({ lastStatus }) {
  const { isAuthenticated, user } = useAuth0();

  return (
    <div
      style={{
        background: "#1e293b",
        color: "white",
        padding: "16px",
        borderRadius: "8px",
        marginBottom: "20px",
      }}
    >
      <h3 style={{ margin: "0 0 10px 0" }}>🛡️ Panel de Seguridad & Estado JWT</h3>
      <p style={{ margin: "4px 0" }}>
        <strong>Autenticado:</strong> {isAuthenticated ? "✅ SÍ" : "❌ NO"}
      </p>
      {isAuthenticated && (
        <p style={{ margin: "4px 0" }}>
          <strong>Usuario:</strong> {user?.name || user?.email}
        </p>
      )}
      <p style={{ margin: "4px 0" }}>
        <strong>Último Código HTTP:</strong>{" "}
        <span
          style={{
            color:
              lastStatus === 200 || lastStatus === 201
                ? "#4ade80"
                : lastStatus
                ? "#f87171"
                : "#9ca3af",
            fontWeight: "bold",
          }}
        >
          {lastStatus || "Sin peticiones aún"}
        </span>
      </p>
    </div>
  );
}