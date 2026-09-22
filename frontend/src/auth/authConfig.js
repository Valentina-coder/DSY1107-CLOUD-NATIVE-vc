import { PublicClientApplication } from "@azure/msal-browser";

export const msalConfig = {
  auth: {
    clientId: import.meta.env.VITE_MSAL_CLIENT_ID || "c1718da8-4cfb-44ff-a81d-2bd87375c22d",
    authority: `https://login.microsoftonline.com/${import.meta.env.VITE_MSAL_TENANT_ID || "60b6d162-a31e-41c7-94d1-c42651cc5a31"}`,
    redirectUri: window.location.origin,
  },
  cache: {
    cacheLocation: "sessionStorage",
    storeAuthStateInCookie: false,
  },
};

// 1. Scopes básicos para que Microsoft permita el login sin errores
export const loginRequest = {
  scopes: ["User.Read", "openid", "profile"],
};

// 2. Scope específico de tu Backend para obtener el token JWT
export const tokenRequest = {
  scopes: [import.meta.env.VITE_API_SCOPE || "api://7505e446-c5fa-4bce-ad90-271d80380996/recurso.read"],
};

export const msalInstance = new PublicClientApplication(msalConfig);