# Despliegue de Stock360 en AWS con Auth0 y Firebase

Este procedimiento usa Firebase Hosting para React, una instancia EC2 con Docker Compose para el backend y un clúster RabbitMQ de dos nodos, y Amazon API Gateway HTTP API como punto de entrada. Auth0 es el proveedor de identidad existente; no se migra a Cognito.

Los recursos de AWS se crean en la consola. Este repositorio prepara el backend y RabbitMQ, pero no puede crear recursos en tu cuenta sin acceso y valores de la cuenta AWS.

## 1. Preparar el tenant y la aplicación de Auth0

En Auth0 confirma que:

- El issuer es exactamente `https://dev-mh13n.us.auth0.com/`.
- El Identifier de la API es `https://stock360-api`.
- La aplicación SPA solicita los scopes `producto:read` y `recurso:write`.
- La URL de Firebase está añadida a **Allowed Callback URLs**, **Allowed Logout URLs** y **Allowed Web Origins**.
- Existe al menos un usuario de prueba y puede iniciar sesión en la aplicación.

El tenant y los usuarios se verifican en el dashboard de Auth0. No copies access tokens ni contraseñas a este repositorio ni a las diapositivas.

## 2. Levantar backend y RabbitMQ en EC2

Usa una instancia Ubuntu con Docker Engine y el plugin Docker Compose instalados. Clona el repositorio y, en la raíz del proyecto, crea un archivo `.env` que no subirás a Git:

```dotenv
RABBITMQ_USERNAME=stock360admin
RABBITMQ_PASSWORD=<contraseña-aleatoria-larga>
RABBITMQ_ERLANG_COOKIE=<cookie-aleatoria-larga-compartida-por-los-dos-nodos>
CORS_ALLOWED_ORIGINS=https://stock360-5953a.web.app
```

Usa valores generados para esa instancia; no reutilices las credenciales de desarrollo que aparecen en el Compose. Desde la raíz:

```bash
docker compose up -d --build
docker compose ps
docker exec stock360-rabbitmq1 rabbitmqctl cluster_status
```

`docker-compose.yml` crea dos nodos RabbitMQ, una base H2 persistente para el backend y el contenedor Spring Boot. El estado del clúster debe mostrar `rabbit@rabbitmq1` y `rabbit@rabbitmq2`. La aplicación queda disponible en el puerto `8080`.

En el grupo de seguridad de EC2:

- Permite SSH (`22`) solo desde la IP del equipo que administra la instancia.
- Permite los puertos de Management (`15672` y `15673`) solo desde la IP del equipo que hará la demostración.
- No abras el puerto AMQP (`5672`) a Internet; los contenedores se comunican por la red privada de Docker.
- Para la demostración sencilla con integración HTTP pública, el puerto `8080` debe ser accesible para API Gateway. El backend sigue validando JWT y permisos. Para producción, evita exponerlo directamente y usa una integración privada mediante VPC Link con un balanceador.

## 3. Crear API Gateway y validar JWT en el gateway

En AWS API Gateway crea una **HTTP API**, una integración HTTP con el backend de EC2 (`http://<IP-PUBLICA-EC2>:8080`) y un authorizer de tipo **JWT**:

| Configuración | Valor |
| --- | --- |
| Identity source | `$request.header.Authorization` |
| Issuer URL | `https://dev-mh13n.us.auth0.com/` |
| Audience | `https://stock360-api` |

Configura las rutas y asígnales la integración:

| Ruta de API Gateway | Autorización |
| --- | --- |
| `GET /api/public/categorias` | Sin authorizer |
| `GET /api/productos` | JWT authorizer; scope `producto:read` |
| `POST /api/admin/categorias` | JWT authorizer; scope `recurso:write` |
| `POST /api/admin/productos` | JWT authorizer; scope `recurso:write` |
| `PUT /api/admin/productos/{id}` | JWT authorizer; scope `recurso:write` |
| `DELETE /api/admin/productos/{id}` | JWT authorizer; scope `recurso:write` |
| `ANY /api/{proxy+}` | JWT authorizer como protección para otras rutas |

No añadas un scope común a la ruta comodín: los permisos del backend son distintos según la ruta. Las operaciones administrativas de RabbitMQ, además, requieren `admin` o `ROLE_ADMIN` en Spring Security.

Configura CORS en la HTTP API para el origen exacto de Firebase, los métodos `GET`, `POST`, `PUT`, `DELETE`, y los encabezados `Authorization` y `Content-Type`. Despliega la API y copia su invoke URL, similar a `https://<api-id>.execute-api.<region>.amazonaws.com`.

API Gateway verifica firma, issuer, audience y vigencia del JWT; Spring Security vuelve a verificar el token y autoriza los permisos de negocio. Para la prueba, una llamada protegida sin token o con token inválido debe ser rechazada por API Gateway; con token válido y scope autorizado debe llegar al backend. Un token válido pero sin el permiso requerido debe recibir `403` del backend.

## 4. Apuntar Firebase al API Gateway

Desde PowerShell en la raíz local del proyecto, compila el frontend con la URL de API Gateway:

```powershell
$env:VITE_API_URL = "https://<api-id>.execute-api.<region>.amazonaws.com"
npm run build --prefix frontend
firebase deploy --only hosting
```

La variable `VITE_API_URL` queda incorporada al bundle durante el build; cambiarla después no modifica el sitio desplegado. Comprueba en el navegador que las llamadas de la pestaña Network usan el dominio `execute-api`, y que login, lectura y una operación protegida funcionan.

## 5. Evidencia para la presentación

Prepara estas pantallas antes de exponer:

1. **EC2:** instancia en estado `running`.
2. **Docker Compose:** salida de `docker compose ps` con backend y ambos nodos activos.
3. **Clúster:** `rabbitmqctl cluster_status` mostrando los dos nodos.
4. **RabbitMQ Management:** exchanges `topic` y `direct`, las tres colas de trabajo y sus tres DLQ; publica mensajes inválidos para mostrar cada DLQ.
5. **API Gateway:** HTTP API activa, integración al backend, authorizer Auth0 y rutas protegidas.
6. **Auth0:** tenant y usuario de prueba, sin revelar contraseñas ni tokens.
7. **Firebase Hosting:** dominio activo y frontend usando el invoke URL de API Gateway.
8. **JWT:** compara una petición sin token (rechazada) con una llamada desde el frontend autenticado (aceptada).

Las comprobaciones de seguridad del gateway y los recursos activos solo se pueden confirmar después de crear y desplegar estos recursos en las cuentas reales de AWS, Auth0 y Firebase.
