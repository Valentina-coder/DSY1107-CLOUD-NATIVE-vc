import React from "react";
import { useAuth0 } from "@auth0/auth0-react";

export function BannerSeguridad({ lastStatus, feedback }) {
  const { isAuthenticated, user } = useAuth0();
  const successfulStatus = lastStatus === 200 || lastStatus === 201 || lastStatus === 204;
  const hasError = (lastStatus !== null && lastStatus >= 400)
    || feedback.startsWith("No se pudo")
    || feedback.startsWith("Auth0 no pudo");
  const statusClass = hasError
    ? "security-status-error"
    : lastStatus === null
      ? "security-status-idle"
      : successfulStatus
        ? "security-status-success"
        : "security-status-idle";

  return (
    <section className="security-banner" aria-label="Estado de seguridad y conexión">
      <div className="security-main">
        <span className="security-shield" aria-hidden="true">✓</span>
        <div>
          <strong>Tu espacio está protegido</strong>
          <p>Autenticación y conexión API</p>
        </div>
      </div>
      <div className="security-details">
        <span className={`auth-state ${isAuthenticated ? "auth-state-on" : ""}`}>
          <span className="security-dot" />
          {isAuthenticated ? "Sesión segura" : "Sesión pública"}
        </span>
      {isAuthenticated && (
          <span className="security-user">{user?.name || user?.email}</span>
      )}
        <span className="http-status">
          <span className="http-label">API</span>
          <strong className={statusClass}>{lastStatus ?? (hasError ? "Error" : "Lista")}</strong>
        </span>
      </div>
      {feedback && (
        <p className={`security-feedback ${hasError ? "feedback-error" : "feedback-success"}`} role={hasError ? "alert" : "status"}>
          {feedback}
        </p>
      )}
    </section>
  );
}