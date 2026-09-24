// src/main.jsx (Prueba directa sin .env)
import React from "react";
import ReactDOM from "react-dom/client";
import { Auth0Provider } from "@auth0/auth0-react";
import App from "./App";

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <Auth0Provider
      domain="dev-mh13n.us.auth0.com"
      clientId="Tq30nY20IofMYeKGiO7dg37iiLwBDhVR"
      authorizationParams={{
        redirect_uri: window.location.origin,
        audience: "https://stock360-api",
      }}
    >
      <App />
    </Auth0Provider>
  </React.StrictMode>
);