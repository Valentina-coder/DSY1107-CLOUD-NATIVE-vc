# Guía paso a paso: RabbitMQ en Stock360

Esta guía describe qué se implementó, dónde vive cada parte del código y cómo comprobar el flujo completo con un clúster RabbitMQ de dos nodos, tres colas y sus DLQ.

## 1. Flujo implementado

```text
POST de producto
       |
       v
ProductoController guarda el producto
       |
       v
ProductoEventPublisher publica producto.creado
       |
       v
stock360.productos.exchange (topic)
       |
       +--> stock360.productos ------> ProductoEventConsumer
       |       | rechazo
       |       v
       |    stock360.productos.dlx (direct) --> stock360.productos.dlq
       |
       +--> stock360.inventario -----> consumidor de validación
       |       | rechazo
       |       v
       |    stock360.inventario.dlx (direct) --> stock360.inventario.dlq
       |
       +--> stock360.notificaciones -> consumidor de validación
               | rechazo
               v
            stock360.notificaciones.dlx (direct) --> stock360.notificaciones.dlq
```

## 2. Qué se agregó al proyecto

1. **Dependencia AMQP:** `Backend/pom.xml` incluye `spring-boot-starter-amqp`, que integra Spring Boot con RabbitMQ.
2. **Conexión y reintentos:** `Backend/src/main/resources/application.properties` configura host, puerto, usuario, contraseña, tres intentos con espera exponencial y el rechazo sin reencolar. Las credenciales se pueden sustituir con variables `RABBITMQ_*`.
3. **Broker:** `docker-compose.yml` ejecuta dos nodos RabbitMQ 4 con Management y almacenamiento persistente. `rabbitmq/rabbitmq.conf` configura el descubrimiento y la formación del clúster.
4. **Topología declarada por código:** `RabbitMqConfig.java` declara un exchange `topic`, tres colas durables, un exchange `direct` y una DLQ para cada cola, con sus bindings.
5. **Contrato del evento:** `ProductoCreadoEvent.java` define los datos publicados: identificador y nombre del producto.
6. **Productor:** `ProductoEventPublisher.java` serializa el evento y lo publica en `stock360.productos.exchange` con la routing key `producto.creado`. `ProductoController.java` lo invoca al crear un producto.
7. **Consumidores:** `ProductoEventConsumer.java` valida el contrato del evento de producto. Las colas de inventario y notificaciones son de demostración: sus listeners validan que el cuerpo sea un objeto JSON no vacío y registran el evento, sin ejecutar lógica de inventario ni enviar notificaciones. Los mensajes inválidos se rechazan hacia su DLQ.
8. **Gestión administrativa:** `RabbitAdminController.java` y `RabbitResourceService.java` exponen operaciones para crear y eliminar colas, exchanges y bindings. Las rutas requieren el permiso `admin` o el rol `ROLE_ADMIN`; además, bloquean los recursos internos y los reservados por RabbitMQ.
9. **Pruebas:** las pruebas en `Backend/src/test/java/com/example/Backend/messaging/` verifican productor, consumidor, DLQ/rechazo, configuración y validaciones administrativas. `BackendApplicationTests.java` comprueba además que las rutas de administración exijan autorización.

### Recursos del flujo

| Tipo | Nombre | Routing key |
| --- | --- | --- |
| Exchange `topic` | `stock360.productos.exchange` | `producto.creado` |
| Cola de trabajo | `stock360.productos` | — |
| Exchange DLX `direct` | `stock360.productos.dlx` | `producto.fallido` |
| Cola de mensajes fallidos | `stock360.productos.dlq` | — |
| Cola de trabajo | `stock360.inventario` | `inventario.actualizado` |
| Exchange DLX `direct` | `stock360.inventario.dlx` | `inventario.fallido` |
| Cola de mensajes fallidos | `stock360.inventario.dlq` | — |
| Cola de trabajo | `stock360.notificaciones` | `notificacion.solicitada` |
| Exchange DLX `direct` | `stock360.notificaciones.dlx` | `notificacion.fallida` |
| Cola de mensajes fallidos | `stock360.notificaciones.dlq` | — |

## 3. Requisitos previos

- Docker Desktop iniciado.
- JDK 25 instalado.
- Para probar la aplicación completa: Node.js y una configuración Auth0 local válida.
- Puertos locales disponibles: `5672` (AMQP), `15672` y `15673` (Management de ambos nodos), `8080` (backend) y `5173` (Vite).

## 4. Iniciar RabbitMQ

Desde la raíz del repositorio, en PowerShell:

Si otro contenedor ya está usando `5672`, `15672`, `15673` o `8080`, no lo detengas a ciegas. Puedes ejecutar este stack en puertos alternativos:

```powershell
$env:RABBITMQ_AMQP_PORT = "25672"
$env:RABBITMQ_MANAGEMENT_PORT = "25673"
$env:RABBITMQ2_MANAGEMENT_PORT = "25674"
$env:BACKEND_PORT = "28080"
docker compose up -d --build
```

En ese caso, Management queda en `http://localhost:25673` y `http://localhost:25674`, y el backend en `http://localhost:28080`; actualiza `VITE_API_URL` en `frontend/.env` para apuntar al puerto `28080`. Si los puertos predeterminados están libres, omite las variables y usa la configuración estándar.

```powershell
docker compose up -d --build
docker compose ps
```

Espera a que `stock360-rabbitmq1`, `stock360-rabbitmq2` y `stock360-backend` estén en ejecución. La primera descarga y compilación puede tardar unos minutos. Comprueba el clúster desde PowerShell:

```powershell
docker exec stock360-rabbitmq1 rabbitmqctl cluster_status
docker exec stock360-rabbitmq2 rabbitmqctl cluster_status
docker compose logs --tail 50 rabbitmq1 rabbitmq2
```

El resultado de `cluster_status` debe enumerar `rabbit@rabbitmq1` y `rabbit@rabbitmq2`. Abre [http://localhost:15672](http://localhost:15672) para el primer nodo y [http://localhost:15673](http://localhost:15673) para el segundo. Las credenciales predeterminadas (`stock360` / `stock360-local`) son solo para desarrollo; no las uses en la nube.

El backend también queda disponible en `http://localhost:8080`. Si prefieres ejecutarlo fuera de Docker para ver los logs, detén el servicio `backend` y sigue la sección 5.

## 5. Iniciar el backend

En una segunda terminal PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.2"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Set-Location .\Backend
.\mvnw.cmd spring-boot:run
```

La ruta de `JAVA_HOME` es un ejemplo: ajústala a la ubicación real del JDK 25. Este paso es opcional si ya ejecutas el backend con Docker Compose. Mantén abierta esta terminal para ver los logs. Al arrancar, espera mensajes equivalentes a:

```text
Starting BackendApplication using Java 25
Attempting to connect to: [localhost:5672]
Created new connection
Started BackendApplication
```

El backend declara las colas, exchanges y bindings en RabbitMQ al iniciar. En **Queues and Streams** deben aparecer las tres colas de trabajo y las tres DLQ.

## 6. Probar el consumidor con un evento válido

1. En RabbitMQ Management, abre **Exchanges** y selecciona `stock360.productos.exchange`.
2. En **Publish message**, indica routing key `producto.creado`.
3. Publica el siguiente payload:

   ```json
   {"productoId":987654,"nombre":"Prueba en vivo"}
   ```

4. Confirma en la terminal del backend:

   ```text
   Evento de producto procesado: productoId=987654, nombre=Prueba en vivo
   ```

5. En **Queues and Streams**, la cola principal normalmente vuelve a cero mensajes rápidamente: significa que el consumidor recibió el mensaje.

## 7. Probar las tres DLQ

1. En el mismo exchange, publica un mensaje con routing key `producto.creado` y payload inválido:

   ```text
   {json-malformado
   ```

2. Abre **Queues and Streams** y selecciona `stock360.productos.dlq`. La métrica de mensajes debe aumentar.
3. Repite con routing key `inventario.actualizado` y cuerpo `no-es-json`; revisa `stock360.inventario.dlq`.
4. Repite con routing key `notificacion.solicitada` y cuerpo `no-es-json`; revisa `stock360.notificaciones.dlq`.
5. En **Get messages**, inspecciona los payloads y no los confirmes ni elimines si deseas conservar evidencia.

Las DLQ no tienen listeners automáticos: los mensajes quedan allí para inspección o reenvío manual.

## 8. Probar el productor desde Stock360

1. Configura `frontend/.env` copiando `frontend/.env.example` y confirma que `VITE_API_URL` apunta a `http://localhost:8080`.
2. Configura Auth0 para el frontend local y verifica que el token tenga `recurso:write`, necesario para crear productos.
3. Inicia el frontend desde la raíz, en una tercera terminal:

   ```powershell
   npm run frontend
   ```

4. Abre [http://localhost:5173](http://localhost:5173), inicia sesión y crea un producto.
5. Confirma la respuesta de creación en la interfaz, el log `Evento de producto publicado: productoId=...`, y luego el log del consumidor `Evento de producto procesado: productoId=...`.

Crear el producto desde la aplicación comprueba el camino real API → base de datos → productor → exchange → cola → consumidor. Publicar manualmente desde Management comprueba el camino desde el broker hacia el consumidor sin depender de Auth0.

## 9. Probar la API administrativa

La administración de RabbitMQ se protege por separado de las funciones de inventario. Obtén un JWT que contenga el permiso `admin` (o usa un usuario con `ROLE_ADMIN`) y guárdalo localmente en PowerShell sin pegarlo en documentación ni subirlo a Git:

```powershell
$token = Read-Host "Pega el access token solo en esta terminal"
$headers = @{ Authorization = "Bearer $token" }
$baseUrl = "http://localhost:8080/api/admin/rabbitmq"
```

Crear una cola de prueba:

```powershell
$body = @{
    name = "stock360.demo.queue"
    durable = $true
    exclusive = $false
    autoDelete = $false
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "$baseUrl/queues" `
    -Headers $headers -ContentType "application/json" -Body $body
```

Crear un exchange y conectarlo a esa cola:

```powershell
$exchange = @{
    name = "stock360.demo.exchange"
    type = "DIRECT"
    durable = $true
    autoDelete = $false
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "$baseUrl/exchanges" `
    -Headers $headers -ContentType "application/json" -Body $exchange

$binding = @{
    queue = "stock360.demo.queue"
    exchange = "stock360.demo.exchange"
    routingKey = "demo.created"
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "$baseUrl/bindings" `
    -Headers $headers -ContentType "application/json" -Body $binding
```

Comprueba la cola y exchange en RabbitMQ Management. Al terminar, elimina primero el binding, después la cola y el exchange:

```powershell
Invoke-RestMethod -Method Delete `
    -Uri "$baseUrl/bindings?queue=stock360.demo.queue&exchange=stock360.demo.exchange&routingKey=demo.created" `
    -Headers $headers
Invoke-RestMethod -Method Delete `
    -Uri "$baseUrl/queues?name=stock360.demo.queue" -Headers $headers
Invoke-RestMethod -Method Delete `
    -Uri "$baseUrl/exchanges?name=stock360.demo.exchange" -Headers $headers
```

Una petición sin token debe responder `401`; un token válido sin permiso administrativo debe responder `403`. No pruebes las operaciones destructivas contra `stock360.productos*` ni con recursos `amq.*`: están reservados para el flujo interno.

## 10. Ejecutar las pruebas automatizadas

Detén cualquier otra instancia del backend que esté usando la base local antes de correr las pruebas. Desde `Backend`:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.2"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd clean test
```

Las pruebas configuran H2 en memoria; no requieren purgar ni modificar `Backend/data/`. Para comprobar el servicio completo con RabbitMQ en vivo, mantén Docker Desktop activo, inicia el broker y sigue las secciones 4–9.

## 11. Detener los servicios

- Detén Spring Boot y Vite con `Ctrl+C` en sus terminales.
- Para detener los servicios sin borrar sus datos: `docker compose stop`.
- `docker compose down` elimina los contenedores, pero mantiene los volúmenes nombrados. Para borrar también los datos persistidos solo en un entorno de prueba, ejecuta `docker compose down -v`.

## 12. Si algo falla

- **No conecta a RabbitMQ:** confirma que ambos nodos estén `healthy`, los puertos `5672`, `15672` y `15673` estén libres, y las variables `RABBITMQ_*` coincidan en los servicios.
- **No aparecen las colas:** confirma en los logs del backend la conexión AMQP y el arranque completo de Spring Boot.
- **El mensaje válido no llega a la cola principal:** comprueba exchange, routing key exacta `producto.creado`, binding y consumidores.
- **El mensaje inválido no está en su DLQ:** revisa los logs del backend y el DLX/routing key asociado a la cola que probaste.
- **Error 401/403 en operaciones administrativas:** usa un JWT para la audiencia correcta que contenga `admin`; `recurso:write` no otorga administración de RabbitMQ.
- **Java class version incorrecta:** ejecuta `java -version` y `.\mvnw.cmd -version` en la misma terminal; ambos deben usar JDK 25.
