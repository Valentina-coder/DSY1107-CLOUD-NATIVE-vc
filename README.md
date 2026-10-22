# Stock360 — Sistema de gestión de inventario

Stock360 es una aplicación web para consultar categorías y administrar productos. El frontend es una SPA en React y Vite; el backend es una API REST con Spring Boot, Java 25 y una base de datos H2 para desarrollo local. Auth0 protege las operaciones de escritura. El backend publica eventos de productos a RabbitMQ y ofrece una API administrativa protegida para gestionar colas, exchanges y bindings.

## Tecnologías

| Área | Tecnologías |
| --- | --- |
| Frontend | React 19, Vite, Axios, Auth0 React SDK |
| Backend | Java 25, Spring Boot 4, Maven Wrapper, Spring MVC, Spring Data JPA |
| Persistencia local | H2; PostgreSQL también está disponible como driver |
| Mensajería | RabbitMQ 4 con plugin de administración; Spring AMQP |
| Hosting frontend | Firebase Hosting |

## Estructura

```text
.
├── Backend/
│   ├── .mvn/wrapper/                 # Maven Wrapper y su versión
│   ├── src/main/java/com/example/Backend/
│   │   ├── config/                   # Seguridad, datos iniciales y configuración AMQP
│   │   ├── controller/               # API REST de categorías, productos y administración RabbitMQ
│   │   ├── messaging/                # Evento, productor, consumidor, topología y gestión AMQP
│   │   ├── model/                    # Entidades JPA
│   │   └── repository/               # Repositorios Spring Data
│   ├── src/main/resources/           # application.properties y recursos SQL
│   ├── src/test/java/                # Pruebas de integración y mensajería
│   └── pom.xml
├── frontend/
│   ├── public/
│   ├── src/
│   │   ├── auth/                     # Configuración Auth0
│   │   ├── components/
│   │   └── services/                 # Cliente HTTP de la API
│   ├── .env.example
│   └── package.json
├── docker-compose.yml                # Backend y clúster RabbitMQ de dos nodos
├── rabbitmq/rabbitmq.conf            # Descubrimiento de nodos RabbitMQ
├── firebase.json
└── package.json                      # Comandos de desarrollo
```

## Funcionalidad

- Consulta pública de categorías.
- Inicio de sesión con Auth0 y envío de JWT a la API.
- Operaciones protegidas para crear categorías y consultar, crear, actualizar y eliminar productos.
- Publicación de `producto.creado` al crear un producto.
- Consumo asíncrono de eventos desde RabbitMQ.
- Reintentos de mensajes ante errores transitorios; mensajes rechazados se registran en los logs y se enrutan a una cola de mensajes fallidos (DLQ).
- API administrativa protegida para crear y eliminar colas, exchanges y bindings.

## Requisitos

- JDK 25 para compilar y ejecutar el backend.
- Node.js y npm para el frontend.
- Docker Desktop iniciado para ejecutar RabbitMQ localmente.
- Una aplicación SPA y una API configuradas en Auth0 para probar la autenticación.

## Configuración local

### 1. Configurar Auth0

En la API de Auth0, el **Identifier** debe coincidir con `VITE_AUTH0_AUDIENCE`. Configura los permisos que solicita la aplicación, por ejemplo `producto:read` y `recurso:write`. Habilita RBAC y la inclusión de permisos en el access token, y asigna al usuario los permisos necesarios.

Para las operaciones administrativas de RabbitMQ, el access token debe incluir el permiso `admin` o el usuario debe autenticarse con el rol `ROLE_ADMIN`. `recurso:write` permite las operaciones administrativas del inventario, pero no concede administración de RabbitMQ.

En la aplicación SPA de Auth0, configura las URLs de callback, cierre de sesión y origen web permitidos, incluyendo:

```text
http://localhost:5173
http://127.0.0.1:5173
```

### 2. Configurar el frontend

Desde PowerShell, copia la plantilla:

```powershell
Copy-Item frontend\.env.example frontend\.env
```

Revisa `frontend\.env` y ajusta las variables a tu tenant de Auth0 y a la URL de la API:

```dotenv
VITE_AUTH0_DOMAIN=tu-tenant.us.auth0.com
VITE_AUTH0_CLIENT_ID=tu-client-id
VITE_AUTH0_AUDIENCE=https://tu-api-identifier
VITE_AUTH0_API_SCOPE=producto:read recurso:write
VITE_API_URL=http://localhost:8080
```

Los nombres de permisos deben coincidir exactamente con los creados en Auth0. No guardes secretos, contraseñas ni tokens en el frontend; las variables `VITE_*` quedan incluidas en el bundle del navegador. No subas el archivo `.env`.

### 3. Iniciar RabbitMQ

Desde la raíz del repositorio:

```powershell
docker compose up -d rabbitmq1 rabbitmq2
docker compose ps
```

Si ya tienes un contenedor usando los puertos predeterminados, no lo detengas a ciegas: usa las variables de puertos alternativos indicadas en la [guía de RabbitMQ](./docs/guia-rabbitmq.md#4-iniciar-rabbitmq).

Espera a que `stock360-rabbitmq1` y `stock360-rabbitmq2` estén saludables. Confirma el clúster con `docker exec stock360-rabbitmq1 rabbitmqctl cluster_status`. Las consolas están en [http://localhost:15672](http://localhost:15672) y [http://localhost:15673](http://localhost:15673). Las credenciales predeterminadas `stock360` / `stock360-local` son solo para desarrollo local; cámbialas para cualquier otro entorno.

Puedes configurar `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME` y `RABBITMQ_PASSWORD` en el entorno del backend. Los valores predeterminados apuntan a `localhost:5672`.

### 4. Iniciar el backend

En una terminal de PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.2"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
cd Backend
.\mvnw.cmd spring-boot:run
```

El backend escucha en `http://localhost:8080`. En los logs confirma que arranca con Java 25, que se conecta a `localhost:5672` y que Spring Boot informa que la aplicación inició correctamente.

La base H2 local persiste en `Backend/data/`. No borres ni modifiques esa base mientras el backend esté ejecutándose. Para usar PostgreSQL u otra base, configura `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` y `SPRING_DATASOURCE_DRIVER`.

### 5. Iniciar el frontend

En otra terminal, desde la raíz:

```powershell
npm ci --prefix frontend
npm run frontend
```

Abre [http://localhost:5173](http://localhost:5173). Para detener RabbitMQ al terminar:

```powershell
docker compose down
```

`docker compose stop` detiene los servicios sin eliminar sus datos. `docker compose down` elimina los contenedores, pero conserva los volúmenes nombrados. En un entorno de prueba, `docker compose down -v` también elimina los datos persistidos.

Para el recorrido completo de implementación y la prueba guiada del productor, consumidor, reintentos, DLQ y API administrativa, consulta [Guía paso a paso de RabbitMQ](./docs/guia-rabbitmq.md).

Para el entregable académico de diseño asíncrono —contexto, justificación, diagrama Mermaid y manejo de errores— consulta [Diseño de integración asíncrona con RabbitMQ](./docs/actividad-diseno-integracion-rabbitmq.md).

## RabbitMQ: productor, consumidor y DLQ

Al guardar un producto, el backend publica un JSON de este tipo en el exchange `stock360.productos.exchange`:

```json
{"productoId":42,"nombre":"Teclado"}
```

La clave de enrutamiento es `producto.creado`. El exchange tipo `topic` enruta el mensaje a la cola durable `stock360.productos`. El consumidor valida el evento y registra su procesamiento.

La topología también declara tres colas durables, cada una con un exchange `direct` de dead-letter y su propia DLQ:

| Cola | Routing key | Exchange DLX `direct` | DLQ |
| --- | --- | --- | --- |
| `stock360.productos` | `producto.creado` | `stock360.productos.dlx` | `stock360.productos.dlq` |
| `stock360.inventario` | `inventario.actualizado` | `stock360.inventario.dlx` | `stock360.inventario.dlq` |
| `stock360.notificaciones` | `notificacion.solicitada` | `stock360.notificaciones.dlx` | `stock360.notificaciones.dlq` |

Las colas de inventario y notificaciones son demostrativas: sus consumidores validan JSON y registran eventos, pero no ejecutan tareas de negocio ni envían notificaciones. Un mensaje inválido se rechaza sin reencolar y RabbitMQ lo enruta a su DLQ. Las DLQ no tienen consumidores automáticos.

### Probar el flujo desde RabbitMQ Management

1. Abre [http://localhost:15672](http://localhost:15672) e inicia sesión con las credenciales locales configuradas.
2. En **Exchanges**, selecciona `stock360.productos.exchange`.
3. Usa **Publish message**, con routing key `producto.creado` y este payload:

   ```json
   {"productoId":987654,"nombre":"Prueba RabbitMQ"}
   ```

4. Confirma en la terminal del backend el log `Evento de producto procesado`. El mensaje normalmente desaparece rápidamente de la cola de productos porque el consumidor está activo.
5. Para probar la DLQ de productos, publica `{json-malformado` con routing key `producto.creado`; revisa `stock360.productos.dlq`.
6. Para probar las otras DLQ, publica `no-es-json` con las routing keys `inventario.actualizado` y `notificacion.solicitada`; revisa sus respectivas DLQ.
7. En **Queues and Streams**, usa **Get messages** para inspeccionar los mensajes. Ten cuidado al confirmarlos o eliminarlos.

También puedes probar el productor creando un producto desde la aplicación; la publicación RabbitMQ ocurre en el backend después de persistir el producto.

## API administrativa de RabbitMQ

Todas las rutas tienen el prefijo `/api/admin/rabbitmq` y requieren un JWT con el permiso Auth0 `admin` o el rol `ROLE_ADMIN`. Envía el token en `Authorization: Bearer <token>`. El permiso `recurso:write` no autoriza estas rutas.

| Operación | Método y ruta |
| --- | --- |
| Crear cola | `POST /api/admin/rabbitmq/queues` |
| Eliminar cola | `DELETE /api/admin/rabbitmq/queues?name=mi-cola` |
| Crear exchange | `POST /api/admin/rabbitmq/exchanges` |
| Eliminar exchange | `DELETE /api/admin/rabbitmq/exchanges?name=mi-exchange` |
| Crear binding | `POST /api/admin/rabbitmq/bindings` |
| Eliminar binding | `DELETE /api/admin/rabbitmq/bindings?queue=mi-cola&exchange=mi-exchange&routingKey=mi.clave` |

Ejemplos de cuerpos JSON:

```json
{
  "name": "mi-cola",
  "durable": true,
  "exclusive": false,
  "autoDelete": false,
  "deadLetterExchange": "mi-dlx",
  "deadLetterRoutingKey": "fallido"
}
```

```json
{
  "name": "mi-exchange",
  "type": "TOPIC",
  "durable": true,
  "autoDelete": false
}
```

```json
{
  "queue": "mi-cola",
  "exchange": "mi-exchange",
  "routingKey": "evento.creado"
}
```

Los tipos de exchange admitidos son `DIRECT`, `FANOUT`, `TOPIC` y `HEADERS`. Para configurar una DLQ en una cola, envía juntos `deadLetterExchange` y `deadLetterRoutingKey`. La API impide gestionar recursos reservados `amq.*` y las colas/exchanges internos del flujo `stock360.productos*`.

## Pruebas y calidad

Desde `Backend`, ejecuta las pruebas con Java 25:

```powershell
.\mvnw.cmd clean test
```

Las pruebas usan H2 en memoria para no bloquear ni modificar la base local. Incluyen persistencia, publicación/validación de eventos, gestión de recursos RabbitMQ y autorización administrativa.

Para compilar el frontend y ejecutar el linter:

```powershell
npm run build --prefix frontend
npm run lint --prefix frontend
```

## Despliegue

Firebase Hosting sirve el frontend desde `frontend/dist` según `firebase.json`. Para el escenario académico en AWS, `docker compose up -d --build` levanta el backend y los dos nodos RabbitMQ en EC2. Configura `VITE_API_URL` con la URL de Amazon API Gateway antes de compilar y desplegar el frontend. Sigue [Despliegue AWS con API Gateway y Auth0](./docs/despliegue-aws.md) para crear la HTTP API/JWT authorizer, apuntar Firebase al gateway y preparar la demostración. En AWS usa credenciales fuertes y limita el acceso a RabbitMQ Management por grupo de seguridad; el backend debe seguir validando JWT y permisos.

## Solución de problemas

### `UnsupportedClassVersionError` al iniciar el backend

Maven está usando un JDK anterior al utilizado para compilar. Configura `JAVA_HOME` y `PATH` para Java 25 en la misma terminal y vuelve a compilar:

```powershell
java -version
.\mvnw.cmd -version
.\mvnw.cmd clean spring-boot:run
```

Ambas versiones deben indicar Java 25.

### No se puede conectar a RabbitMQ

- Comprueba que Docker Desktop esté iniciado.
- Ejecuta `docker compose ps` y espera el estado `healthy` de ambos nodos.
- Confirma que los puertos `5672`, `15672` y `15673` estén disponibles.
- Revisa que `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME` y `RABBITMQ_PASSWORD` coincidan con la configuración del contenedor.
- Revisa los logs con `docker compose logs rabbitmq1 rabbitmq2`.

### Error 401 o 403 de Auth0

- `401`: inicia sesión de nuevo y confirma dominio, client ID, audience y URLs permitidas en Auth0.
- `403` en operaciones de inventario: confirma que el access token tenga `recurso:write`.
- `403` en administración de RabbitMQ: el token necesita `admin` o el rol `ROLE_ADMIN`.
- Los permisos del API de Auth0 deben coincidir exactamente con `VITE_AUTH0_API_SCOPE`. Reinicia Vite tras cambiar `frontend/.env` y vuelve a iniciar sesión para obtener un token nuevo.

### Error de CORS

Configura `CORS_ALLOWED_ORIGINS` en el backend con el origen exacto del frontend, por ejemplo `http://localhost:5173`, y confirma que el backend se haya reiniciado después del cambio.

### La base H2 está bloqueada

No ejecutes otra instancia del backend ni las pruebas contra `Backend/data/` mientras el servidor local esté utilizando esa base. La suite `mvnw clean test` configura H2 en memoria automáticamente.
