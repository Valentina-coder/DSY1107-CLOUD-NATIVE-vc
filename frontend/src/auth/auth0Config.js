export const auth0Config = {
  domain: import.meta.env.VITE_AUTH0_DOMAIN || "dev-mh13n.us.auth0.com",
  clientId: import.meta.env.VITE_AUTH0_CLIENT_ID || "Tq30nY20IofMYeKGiO7dg37iiLwBDhVR",
  audience: import.meta.env.VITE_AUTH0_AUDIENCE || "https://stock360-api",
  scope: import.meta.env.VITE_AUTH0_API_SCOPE || "producto:read recurso:write",
};
