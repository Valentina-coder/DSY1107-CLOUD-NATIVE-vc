import axios from "axios";

// 1. Obtener la URL y eliminar barras diagonales al final para evitar //
const rawBaseURL = import.meta.env.VITE_API_URL || "https://ih7rm87w2i.execute-api.us-east-1.amazonaws.com";
const cleanBaseURL = rawBaseURL.replace(/\/+$/, "");

console.log("🌐 API Base URL configurada:", cleanBaseURL);

const api = axios.create({
  baseURL: cleanBaseURL,
  headers: {
    "Content-Type": "application/json",
  },
});

// 2. Función para inyectar/remover el token JWT
export const setAuthToken = (token) => {
  if (token) {
    api.defaults.headers.common["Authorization"] = `Bearer ${token}`;
    console.log("🔑 Token JWT adjuntado con éxito a Axios");
  } else {
    delete api.defaults.headers.common["Authorization"];
    console.warn("🔒 Sin token JWT en Axios (modo público)");
  }
};

export default api;