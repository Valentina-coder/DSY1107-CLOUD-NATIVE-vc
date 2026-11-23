import axios from "axios";
import { configureAuthToken } from "./authToken";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080",
});

export const setAuthToken = (token) => configureAuthToken(api, token);

export default api;