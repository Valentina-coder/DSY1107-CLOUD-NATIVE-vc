import axios from "axios";

const rawBaseURL = import.meta.env.VITE_API_URL || "https://ih7rm87w2i.execute-api.us-east-1.amazonaws.com/prod";
const cleanBaseURL = rawBaseURL.replace(/\/+$/, "");

const api = axios.create({
  baseURL: cleanBaseURL,
  headers: {
    "Content-Type": "application/json",
  },
});

// Variable en memoria para el token JWT
let currentToken = null;

export const setAuthToken = (token) => {
  currentToken = token;
  if (token) {
    console.log("🔑 Token JWT adjuntado con éxito a Axios");
  } else {
    console.warn("🔒 Sin token JWT en Axios (modo público)");
  }
};

// Interceptor: Inyecta el token activo a la cabecera Authorization
api.interceptors.request.use(
  (config) => {
    if (currentToken) {
      config.headers.Authorization = `Bearer ${currentToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

export default api;