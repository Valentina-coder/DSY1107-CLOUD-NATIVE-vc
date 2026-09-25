import axios from "axios";

const api = axios.create({
  baseURL: "https://ih7rm87w2i.execute-api.us-east-1.amazonaws.com"
});

export const setAuthToken = (token) => {
  if (token) {
    api.defaults.headers.common["Authorization"] = `Bearer ${token}`;
  } else {
    delete api.defaults.headers.common["Authorization"];
  }
};

export default api;