import React from 'react';
import ReactDOM from 'react-dom/client';
import { MsalProvider } from '@azure/msal-react';
import App from './App';
import { msalInstance } from './auth/authConfig'; 


msalInstance.initialize().then(() => {
  ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
      <MsalProvider instance={msalInstance}>
        <App />
      </MsalProvider>
    </React.StrictMode>
  );
}).catch((error) => {
  console.error("Error al inicializar MSAL:", error);
});