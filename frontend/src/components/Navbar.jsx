import React from 'react';
import { useIsAuthenticated, useMsal } from '@azure/msal-react';
import { loginRequest } from '../auth/authConfig'; // Importación requerida para enviar los scopes

export default function Navbar() {
  const { instance, accounts } = useMsal();
  const isAuthenticated = useIsAuthenticated();
  const user = accounts[0];

  const handleLogin = async () => {
    try {
      // Se pasa loginRequest para solicitar los scopes del backend
      await instance.loginPopup(loginRequest);
    } catch (error) {
      console.error("Error al iniciar sesión:", error);
    }
  };

  const handleLogout = async () => {
    try {
      await instance.logoutPopup();
    } catch (error) {
      console.error("Error al cerrar sesión:", error);
    }
  };

  if (isAuthenticated) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        <p style={{ margin: 0 }}>Bienvenido, <strong>{user?.name || user?.username}</strong></p>
        <button onClick={handleLogout} style={{ padding: '8px 16px', cursor: 'pointer' }}>
          Cerrar Sesión
        </button>
      </div>
    );
  }

  return (
    <button onClick={handleLogin} style={{ padding: '8px 16px', cursor: 'pointer' }}>
      Iniciar Sesión
    </button>
  );
}