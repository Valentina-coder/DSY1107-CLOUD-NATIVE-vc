import { useIsAuthenticated, useMsal } from '@azure/msal-react';

export default function Navbar() {
  const { instance, accounts } = useMsal();
  const isAuthenticated = useIsAuthenticated();
  const user = accounts[0];

  if (isAuthenticated) {
    return (
      <div>
        <p>Bienvenido, {user?.name || user?.username}</p>
        <button onClick={() => instance.logoutRedirect()}>
          Cerrar Sesión
        </button>
      </div>
    );
  }

  return <button onClick={() => instance.loginRedirect()}>Iniciar Sesión</button>;
}