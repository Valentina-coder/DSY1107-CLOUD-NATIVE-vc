# Stock360 - Cloud Native

Aplicacion full stock de inventario. El frontend usa React/Vite, Firebase Authentication y Firebase Hosting. El backend usa Java/Spring Boot, JPA y H2. La arquitectura incorpora un Gateway local y queda preparada para AWS API Gateway y EC2.

## Arquitectura

```text
Frontend React
  Firebase Hosting
  |
  | Authorization: Bearer <Firebase ID token>
  v
AWS API Gateway (objetivo cloud)
  Gateway local :8081 (desarrollo)
  |
  v
Spring Boot :8080 / EC2 (objetivo cloud)
  |
  v
H2/JPA
```

- Frontend: `frontend/`, Vite en `http://localhost:5173`.
- Hosting: `https://stock360-5953a.web.app`.
- Backend: `Backend/`, Spring Boot en `http://localhost:8080`.
- Gateway local: `Gateway/`, proxy en `http://localhost:8081`.
- Persistencia local: H2 en `Backend/data/inventariodb.mv.db`.
- Tests: H2 en memoria aislado en `Backend/src/test/resources/application.properties`.
- CORS local: `localhost:5173` y `127.0.0.1:5173`.

## Funcionalidades implementadas

### Frontend

- Login y logout con Firebase Authentication y Google.
- Adquisicion del Firebase ID token.
- Axios agrega el token Bearer a las llamadas protegidas.
- Consulta de categorias y productos.
- Creacion, edicion y eliminacion de categorias.
- Creacion, detalle, edicion y eliminacion de productos.
- Panel de autenticacion, error y ultimo codigo HTTP.

### Backend / BFF

- Validacion de firma y vigencia del JWT mediante JWKS de Firebase.
- Validacion de `issuer` y `audience`.
- Lectura protegida para usuarios autenticados.
- Escrituras preparadas para claims administrativos `admin`.
- Respuestas `401` sin token o con token invalido y `403` sin permisos.
- Capas `Controller -> Service -> Repository`.
- CORS configurado para el frontend local.

## CRUD disponible

### Productos

| Operacion | Metodo | Endpoint | Permiso |
|---|---|---|---|
| Listar publico | GET | `/api/public/productos` | Ninguno |
| Listar autenticado | GET | `/api/productos` | Usuario autenticado |
| Buscar por ID | GET | `/api/productos/{id}` | Usuario autenticado |
| Crear | POST | `/api/productos` | Claim `admin` |
| Actualizar | PUT | `/api/productos/{id}` | Claim `admin` |
| Eliminar | DELETE | `/api/productos/{id}` | Claim `admin` |

### Categorias

| Operacion | Metodo | Endpoint | Permiso |
|---|---|---|---|
| Listar | GET | `/api/public/categorias` | Ninguno |
| Crear | POST | `/api/categorias` | Claim `admin` |
| Actualizar | PUT | `/api/categorias/{id}` | Claim `admin` |
| Eliminar | DELETE | `/api/categorias/{id}` | Claim `admin` |

## Configuracion de Firebase Authentication

Proyecto Firebase utilizado:

```text
stock360-5953a
```

En Firebase Console:

1. Abre **Authentication -> Sign-in method**.
2. Habilita el proveedor **Google**.
3. En **Authentication -> Settings -> Authorized domains**, agrega:
  ```text
  localhost
  127.0.0.1
  stock360-5953a.web.app
  stock360-5953a.firebaseapp.com
  ```
4. Usa la configuracion web de Firebase en las variables del frontend.

El backend valida los Firebase ID tokens con:

```text
issuer: https://securetoken.google.com/stock360-5953a
audience: stock360-5953a
```

## Variables de entorno

Edita `frontend/.env`:

```env
VITE_API_BASE_URL=http://localhost:8081
VITE_FIREBASE_API_KEY=TU_API_KEY
VITE_FIREBASE_AUTH_DOMAIN=stock360-5953a.firebaseapp.com
VITE_FIREBASE_PROJECT_ID=stock360-5953a
VITE_FIREBASE_STORAGE_BUCKET=stock360-5953a.firebasestorage.app
VITE_FIREBASE_MESSAGING_SENDER_ID=491517157529
VITE_FIREBASE_APP_ID=TU_APP_ID
```

El backend usa estas propiedades en `Backend/src/main/resources/application.properties`:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=${FIREBASE_ISSUER_URI:https://securetoken.google.com/stock360-5953a}
spring.security.oauth2.resourceserver.jwt.audience=${FIREBASE_PROJECT_ID:stock360-5953a}
```

Para ejecutar con variables reales en PowerShell:

```powershell
$env:FIREBASE_ISSUER_URI="https://securetoken.google.com/stock360-5953a"
$env:FIREBASE_PROJECT_ID="stock360-5953a"
```

No se deben versionar contrasenas, tokens, refresh tokens, claves privadas ni archivos de cuenta de servicio.

## Ejecucion

Terminal 1, backend:

```powershell
Set-Location Backend
./mvnw.cmd spring-boot:run
```

Terminal 2, frontend:

```powershell
Set-Location frontend
npm install
npm run dev -- --host localhost --port 5173
```

Abrir: `http://localhost:5173/`.

Terminal 3, Gateway local:

```powershell
Set-Location Gateway
npm install
npm run start
```

El orden local es: backend `8080`, Gateway `8081`, frontend `5173`.

## Gateway y API Manager

El frontend no apunta a los controladores por rutas dispersas: usa únicamente `VITE_API_BASE_URL`. Esto permite cambiar el destino sin modificar la lógica React.

### Desarrollo local

El frontend usa el Gateway local:

```env
VITE_API_BASE_URL=http://localhost:8081
```

El Gateway local vive en `Gateway/` y reenvia las rutas `/api/**` al backend en `http://localhost:8080`.

Pruebas del Gateway:

```powershell
Invoke-WebRequest http://localhost:8081/api/public/categorias
```

### AWS API Gateway

Para la presentacion cloud, AWS API Gateway debe colocarse delante del backend en EC2:

```text
Frontend React
  |
  | VITE_API_BASE_URL
  v
AWS API Gateway
  |
  v
Backend Spring Boot :8080
```

Pasos de configuración:

1. Crear una instancia EC2 con Ubuntu y Java 21.
2. Compilar y subir el JAR del backend a EC2.
3. Ejecutar Spring Boot en el puerto `8080`.
4. Crear una HTTP API en AWS API Gateway.
5. Configurar la integracion HTTP hacia `http://IP_EC2:8080`.
6. Registrar las rutas CRUD.
7. Configurar CORS para el dominio de Firebase Hosting.
8. Configurar el JWT Authorizer de Firebase:
  ```text
  Issuer: https://securetoken.google.com/stock360-5953a
  Audience: stock360-5953a
  ```
9. Mantener la validacion JWT tambien en Spring Boot.
10. Cambiar solamente la variable del frontend:

```env
VITE_API_BASE_URL=https://TU_API_ID.execute-api.TU_REGION.amazonaws.com
```

El codigo React, los servicios y los controladores no necesitan cambiar. El Gateway local demuestra el mismo patron mientras se configura AWS.

## Firebase Hosting

La configuracion esta en:

- `firebase.json`: publica `frontend/dist` y configura SPA fallback.
- `.firebaserc`: selecciona `stock360-5953a`.

Publicar:

```powershell
Set-Location frontend
npm run build
Set-Location ..
npx firebase-tools login
npx firebase-tools deploy --only hosting --project stock360-5953a
```

URL publicada:

```text
https://stock360-5953a.web.app
```

En produccion, `VITE_API_BASE_URL` no puede ser `localhost`; debe ser la URL publica de AWS API Gateway y se debe volver a ejecutar `npm run build` antes del deploy.

## Pruebas

Compilar frontend:

```powershell
Set-Location frontend
npm run build
```

Probar backend:

```powershell
Set-Location Backend
./mvnw.cmd test
```

Pruebas manuales recomendadas:

1. Consultar categorias publicas: `200`.
2. Iniciar sesion con Google/Firebase.
3. Consultar productos autenticado: `200`.
4. Crear producto: `201`.
5. Editar producto: `200`.
6. Eliminar producto: `204`.
7. Intentar una operacion protegida sin token: `401`.
8. Intentar una operacion administrativa sin claim `admin`: `403`.
9. Consultar un ID inexistente: `404`.
10. Detener el backend y probar el Gateway: `502`.

Pruebas ya verificadas localmente:

```text
Gateway /api/public/categorias -> 200
Gateway /api/productos sin token -> 401
Gateway ruta inexistente -> 404
Frontend build -> correcto
Backend tests -> 1 test, 0 fallos, 0 errores
```

## Estructura principal

```text
Backend/
  src/main/java/com/example/Backend/
    config/SeguridadConfig.java
    controller/CategoriaController.java
    controller/ProductoController.java
    model/
    repository/
  src/main/resources/application.properties
frontend/
  src/auth/firebase.js
  src/services/api.js
  src/App.jsx
  src/main.jsx
  .env
Gateway/
  server.js
  package.json
firebase.json
.firebaserc
```
