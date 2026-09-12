import { useAuth0 } from '@auth0/auth0-react';

export default function LoginButton() {
  const { loginWithRedirect, logout, isAuthenticated, user } = useAuth0();

  if (isAuthenticated) {
    return (
      <div>
        <p>Bienvenido, {user.name}</p>
        <button onClick={() => logout({ logoutParams: { returnTo: window.location.origin } })}>
          Cerrar Sesión
        </button>
      </div>
    );
  }

  return <button onClick={() => loginWithRedirect()}>Iniciar Sesión</button>;
}