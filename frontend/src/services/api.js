import axios from "axios";
import { msalInstance, loginRequest } from "../auth/authConfig";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
});

api.interceptors.request.use(async (config) => {
  const account = msalInstance.getActiveAccount() || msalInstance.getAllAccounts()[0];
  if (account) {
    try {
      const response = await msalInstance.acquireTokenSilent({
        ...loginRequest,
        account,
      });
      config.headers.Authorization = `Bearer ${response.accessToken}`;
    } catch (error) {
      console.warn("No se pudo obtener el token de forma silenciosa:", error);
    }
  }
  return config;
}, (error) => Promise.reject(error));

export default api;