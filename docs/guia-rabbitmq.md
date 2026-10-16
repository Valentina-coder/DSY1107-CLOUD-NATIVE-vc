# Guía paso a paso: RabbitMQ en Stock360

Esta guía describe qué se implementó, dónde vive cada parte del código y cómo comprobar el flujo completo con RabbitMQ ejecutándose localmente.

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
stock360.productos.exchange
       |
       v
stock360.productos  ---> ProductoEventConsumer
       |
       | mensaje inválido tras los reintentos
       v
stock360.productos.dlx
       |
       v
stock360.productos.dlq  ---> queda disponible para inspección/reenvío
```

## 2. Qué se agregó al proyecto

1. **Dependencia AMQP:** `Backend/pom.xml` incluye `spring-boot-starter-amqp`, que integra Spring Boot con RabbitMQ.
2. **Conexión y reintentos:** `Backend/src/main/resources/application.properties` configura host, puerto, usuario, contraseña, tres intentos con espera exponencial y el rechazo sin reencolar. Las credenciales se pueden sustituir con variables `RABBITMQ_*`.
3. **Broker local:** `docker-compose.yml` ejecuta RabbitMQ 4 con el plugin Management y expone AMQP en `5672` y la consola en `15672`.
4. **Topología declarada por código:** `RabbitMqConfig.java` declara automáticamente el exchange `topic`, la cola de productos, su exchange/cola DLQ y ambos bindings.
5. **Contrato del evento:** `ProductoCreadoEvent.java` define los datos publicados: identificador y nombre del producto.
6. **Productor:** `ProductoEventPublisher.java` serializa el evento y lo publica en `stock360.productos.exchange` con la routing key `producto.creado`. `ProductoController.java` lo invoca al crear un producto.
7. **Consumidor:** `ProductoEventConsumer.java` recibe y valida el JSON. Los eventos válidos se registran como procesados; los inválidos se registran como errores y se rechazan para que RabbitMQ los mande a la DLQ.
8. **Gestión administrativa:** `RabbitAdminController.java` y `RabbitResourceService.java` exponen operaciones para crear y eliminar colas, exchanges y bindings. Las rutas requieren el permiso `admin` o el rol `ROLE_ADMIN`; además, bloquean los recursos internos y los reservados por RabbitMQ.
9. **Pruebas:** las pruebas en `Backend/src/test/java/com/example/Backend/messaging/` verifican productor, consumidor, DLQ/rechazo, configuración y validaciones administrativas. `BackendApplicationTests.java` comprueba además que las rutas de administración exijan autorización.

### Recursos del flujo

| Tipo | Nombre | Routing key |
| --- | --- | --- |
| Exchange `topic` | `stock360.productos.exchange` | `producto.creado` |
| Cola de trabajo | `stock360.productos` | — |
| Exchange DLX `direct` | `stock360.productos.dlx` | `producto.fallido` |
| Cola de mensajes fallidos | `stock360.productos.dlq` | — |

## 3. Requisitos previos

- Docker Desktop iniciado.
- JDK 25 instalado.
- Para probar la aplicación completa: Node.js y una configuración Auth0 local válida.
- Puertos locales disponibles: `5672` (AMQP), `15672` (Management), `8080` (backend) y `5173` (Vite).

## 4. Iniciar RabbitMQ

Desde la raíz del repositorio, en PowerShell:

```powershell
docker compose up -d rabbitmq
docker compose ps
```

Espera a que `stock360-rabbitmq` muestre estado `healthy`. También puedes revisar el arranque:

```powershell
docker compose logs --tail 50 rabbitmq
```

Abre [http://localhost:15672](http://localhost:15672) e inicia sesión con `stock360` / `stock360-local` si no has cambiado las variables `RABBITMQ_USERNAME` y `RABBITMQ_PASSWORD`. Son credenciales de desarrollo; no las uses en producción.

## 5. Iniciar el backend

En una segunda terminal PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.2"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Set-Location .\Backend
.\mvnw.cmd spring-boot:run
```

La ruta de `JAVA_HOME` es un ejemplo: ajústala a la ubicación real del JDK 25. Mantén abierta esta terminal para ver los logs. Al arrancar, espera mensajes equivalentes a:

```text
Starting BackendApplication using Java 25
Attempting to connect to: [localhost:5672]
Created new connection
Started BackendApplication
```

El backend declara las colas, exchanges y bindings en RabbitMQ al iniciar. Verifica con `docker compose ps` o en **Queues and Streams** que aparezcan `stock360.productos` y `stock360.productos.dlq`.

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

## 7. Probar reintentos, logs y DLQ

1. En el mismo exchange, publica un mensaje con routing key `producto.creado` y payload inválido:

   ```text
   {json-malformado
   ```

2. Observa la terminal del backend. Aparecerá el error del evento inválido para cada intento; el listener está configurado con un máximo de tres intentos.
3. Abre **Queues and Streams** y selecciona `stock360.productos.dlq`.
4. La métrica de mensajes debe aumentar. En **Get messages**, solicita un mensaje para revisar el payload.
5. No confirmes ni elimines el mensaje si quieres conservarlo para inspección o reenvío.

La DLQ no tiene un listener automático: los mensajes se quedan allí. Para limpiar una prueba de descarte, usa la acción de purge de la interfaz solo después de confirmar que estás en `stock360.productos.dlq` y que no necesitas conservar sus mensajes.

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
- Para detener el broker conservando el contenedor local: `docker compose stop rabbitmq`.
- `docker compose down` elimina el contenedor y esta configuración no declara un volumen de datos para RabbitMQ; los mensajes de prueba del broker podrían perderse. Al volver a iniciar el backend, la topología declarada por código se crea de nuevo.

## 12. Si algo falla

- **No conecta a RabbitMQ:** confirma que el contenedor esté `healthy`, los puertos `5672` y `15672` estén libres, y las variables `RABBITMQ_*` coincidan en Docker y backend.
- **No aparecen las colas:** confirma en los logs del backend la conexión AMQP y el arranque completo de Spring Boot.
- **El mensaje válido no llega a la cola principal:** comprueba exchange, routing key exacta `producto.creado`, binding y consumidores.
- **El mensaje inválido no está en la DLQ:** revisa errores del consumidor y espera que terminen los reintentos; después confirma que la cola principal tenga configurados el DLX `stock360.productos.dlx` y la clave `producto.fallido`.
- **Error 401/403 en operaciones administrativas:** usa un JWT para la audiencia correcta que contenga `admin`; `recurso:write` no otorga administración de RabbitMQ.
- **Java class version incorrecta:** ejecuta `java -version` y `.\mvnw.cmd -version` en la misma terminal; ambos deben usar JDK 25.
