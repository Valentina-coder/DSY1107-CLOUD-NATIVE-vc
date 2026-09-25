# Stock360 - Sistema de Gestión de Inventario

Stock360 es una aplicación web (Single Page Application) diseñada para administrar un inventario de productos y categorías. Cuenta con un sistema de acceso público para consultar categorías y un panel de administración protegido mediante autenticación para realizar operaciones CRUD (Crear, Leer, Actualizar, Eliminar) sobre los productos.

## 🛠️ Estructura del Proyecto

La estructura actual del directorio principal se organiza de la siguiente manera:

```text
DSY1107-CLOUD-NATIVE-vc/
│
├── .firebase/                      <-- Cache y archivos temporales de Firebase CLI
├── .env.example                    <-- Plantilla de variables de entorno del proyecto
├── .firebaserc                     <-- Configuración de identificadores/proyectos en Firebase
├── .gitignore                      <-- Exclusiones globales de Git para la raíz
├── firebase.json                   <-- Configuración del despliegue en Firebase Hosting
├── package.json                    <-- Script de automatización y orquestación de la raíz
├── README.md                       <-- Documentación general del proyecto
│
├── Backend/                        <-- API REST en Java con Spring Boot (Puerto: 8080)
│   ├── .mvn/wrapper/               <-- Archivos para ejecutar Maven sin instalación previa
│   │   └── maven-wrapper.properties
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/Backend/
│   │   │   │   ├── config/         <-- Configuraciones del servidor (Seguridad/CORS)
│   │   │   │   │   └── SeguridadConfig.java
│   │   │   │   ├── controller/     <-- Controladores REST (Endpoints para la API)
│   │   │   │   │   ├── CategoriaController.java
│   │   │   │   │   └── ProductoController.java
│   │   │   │   ├── model/          <-- Entidades y modelos de datos (JPA/Hibernate)
│   │   │   │   │   ├── Categoria.java
│   │   │   │   │   └── Producto.java
│   │   │   │   ├── repository/     <-- Capa de persistencia y consultas a BD (Spring Data JPA)
│   │   │   │   │   ├── CategoriaRepository.java
│   │   │   │   │   └── ProductoRepository.java
│   │   │   │   └── BackendApplication.java <-- Punto de entrada de la aplicación Spring Boot
│   │   │   └── resources/          <-- Recursos estáticos y archivos de propiedades
│   │   │       ├── application.properties  <-- Configuración de BD, puertos y propiedades
│   │   │       └── data.sql        <-- Script SQL con datos iniciales de prueba
│   │   └── test/                   <-- Pruebas unitarias e integración del Backend
│   │       └── java/com/example/Backend/
│   │           └── BackendApplicationTests.java
│   ├── .gitattributes
│   ├── .gitignore
│   ├── mvnw / mvnw.cmd             <-- Ejecutables Wrapper de Maven (Linux/macOS y Windows)
│   └── pom.xml                     <-- Archivo de dependencias y plugins Maven
│
└── frontend/                       <-- Aplicación web SPA con React + Vite (Puerto: 5173)
    ├── dist/                       <-- Build estático generado para producción
    ├── public/                     <-- Recursos públicos globales (favicons, íconos)
    ├── src/                        <-- Código fuente del cliente web
    │   ├── assets/                 <-- Imágenes y vectores importados en React
    │   ├── auth/                   <-- Configuración de autenticación del cliente
    │   │   └── authConfig.js
    │   ├── components/             <-- Componentes reutilizables de la UI
    │   │   ├── BannerSeguridad.jsx
    │   │   └── Navbar.jsx
    │   ├── services/               <-- Módulos para peticiones HTTP al Backend
    │   │   └── api.js
    │   ├── App.css / App.jsx       <-- Componente principal de la interfaz y sus estilos
    │   ├── index.css               <-- Estilos globales de la aplicación
    │   └── main.jsx                <-- Punto de inicio de la app React
    ├── .gitignore
    ├── .oxlintrc.json              <-- Configuración de linter (Oxlint)
    ├── index.html                  <-- Plantilla HTML base servida por Vite
    ├── package.json                <-- Dependencias de npm y scripts del frontend
    ├── README.md                   <-- Documentación específica del frontend
    └── vite.config.js              <-- Configuración de compilación y servidor Vite
## 🚀 Pasos Realizados Hasta el Momento

## 🚀 Tecnologías Utilizadas

**Frontend:**
*   **React (con Vite):** Librería principal para construir la interfaz de usuario.
*   **Auth0 (@auth0/auth0-react):** Plataforma de identidad para gestionar el inicio de sesión y la generación de tokens JWT.
*   **Axios:** Cliente HTTP para realizar las peticiones a la API.

**Backend:**
*   **AWS (Amazon Web Services):** Infraestructura Serverless utilizando Amazon API Gateway y AWS Lambda.
*   **Base de datos:** [Añade aquí tu BD, ej: MySQL / PostgreSQL / DynamoDB].

---

## ⚙️ Características Principales

1.  **Área Pública:** Consulta del listado de categorías disponibles sin necesidad de iniciar sesión.
2.  **Autenticación Segura:** Inicio y cierre de sesión gestionados por Auth0, protegiendo las rutas de escritura.
3.  **Gestión de Productos (Admin):** 
    *   Consultar el inventario completo.
    *   Agregar nuevos productos asignándolos a categorías existentes.
    *   Editar el nombre, precio y categoría de productos existentes.
    *   Eliminar productos del sistema.
4.  **Sincronización de Tokens:** Intercepción automática de tokens JWT para adjuntarlos en las cabeceras `Authorization` de las peticiones protegidas.

---

## 🛠️ Requisitos Previos

Antes de ejecutar este proyecto localmente, asegúrate de tener instalado:
*   [Node.js](https://nodejs.org/) (versión 16 o superior).
*   Una cuenta activa en [Auth0](https://auth0.com/) con una Single Page Application configurada.
*   El backend desplegado y operativo en AWS.

---

## 💻 Instalación y Configuración Local

**1. Clonar el repositorio**
```bash
git clone [https://github.com/tu-usuario/stock360.git](https://github.com/tu-usuario/stock360.git)
cd stock360

2. Instalar dependencias
npm install

3. Configurar variables de entorno
Crea un archivo .env en la raíz del proyecto (al mismo nivel que package.json) y agrega las siguientes variables. Reemplaza los valores con las credenciales de tu tenant de Auth0 y la URL de tu API en AWS:
# URL base de tu backend en AWS API Gateway
VITE_API_URL=[https://ih7rm87w2i.execute-api.us-east-1.amazonaws.com](https://ih7rm87w2i.execute-api.us-east-1.amazonaws.com)

# Credenciales de Auth0
VITE_AUTH0_DOMAIN=tu-dominio.auth0.com
VITE_AUTH0_CLIENT_ID=tu_client_id_generado_por_auth0
VITE_AUTH0_AUDIENCE=https://stock360-api

4. Iniciar el servidor de desarrollo

npm run dev

🐛 Solución de Problemas Comunes 
Error 401 Unauthorized al crear/editar productos:

Verifica que has iniciado sesión.

Asegúrate de que el valor de AUDIENCE_API en App.jsx coincida exactamente con el Identificador de la API registrado en el panel de Auth0. Si no coinciden, Auth0 emitirá un token opaco en lugar de un JWT válido.

Error 500 Internal Server Error en peticiones (ej: /api/public/categorias):

Este es un error del lado del servidor (AWS). La petición desde React está llegando correctamente al backend, pero la función está fallando al procesarla.

Solución: Ingresa a tu consola de AWS, ve al servicio CloudWatch, busca los "Log groups" de tu función Lambda asociada y revisa el registro exacto del error (generalmente suele ser un error de conexión a la base de datos o un fallo de sintaxis en el código backend).

Error de CORS (Cross-Origin Resource Sharing):

Asegúrate de que tu API Gateway en AWS tenga configurado CORS (Mocks de respuesta para el método OPTIONS) y permita explícitamente el origen http://localhost:5173.
