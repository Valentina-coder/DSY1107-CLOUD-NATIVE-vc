import React from "react";

export const BannerSeguridad = ({ lastStatus, lastError, isAuthenticated, user }) => {
  return (
    <div style={{ padding: "16px", background: "#0f172a", color: "#ffffff", borderRadius: "8px", marginBottom: "20px" }}>
      <h3 style={{ margin: "0 0 8px 0" }}>🛡️ Panel de Seguridad & Estado JWT</h3>
      <p style={{ margin: "4px 0" }}><strong>Autenticado:</strong> {isAuthenticated ? "✅ SÍ" : "❌ NO"}</p>
      {isAuthenticated && (
        <p style={{ margin: "4px 0" }}><strong>Usuario:</strong> {user?.email} ({user?.displayName})</p>
      )}
      {lastStatus && (
        <p style={{ margin: "4px 0" }}>
          <strong>Último Código HTTP:</strong>{" "}
          <span style={{ 
            color: lastStatus >= 200 && lastStatus < 300 ? "#10b981" : "#ef4444", 
            fontWeight: "bold" 
          }}>
            {lastStatus}
          </span>
        </p>
      )}
      {lastError && (
        <p role="alert" style={{ margin: "4px 0", color: "#fecaca" }}>
          <strong>Error:</strong> {lastError}
        </p>
      )}
    </div>
  );
};