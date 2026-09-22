
# DSY1107 - Cloud Native Project Stock 360

Este repositorio contiene una arquitectura unificada (**Stock360**) que integra un backend desarrollado en **Spring Boot** y un frontend moderno con **Vite**. Ambos entornos están configurados para coexistir en un mismo repositorio y ejecutarse en paralelo de forma eficiente.

---

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

### 1. Inicialización del Backend
* Se generó la estructura base de **Spring Boot** utilizando dependencias esenciales (`Spring Web`, `Spring Data JPA`, `Validation`, `Lombok`, `DevTools`).
* Se organizaron los archivos dentro de la subcarpeta independiente `/Backend`.

### 2. Inicialización del Frontend
* Se creó la SPA (Single Page Application) utilizando **Vite** en la subcarpeta `/frontend`.
* Se seleccionó **Oxlint** como linter por defecto debido a su alta velocidad y optimización basada en Rust.
* Se instalaron con éxito todas las dependencias del ecosistema Node (`npm install`).

### 3. Configuración del Repositorio (Git)
* Se unificaron los entornos inicializando Git directamente en la **carpeta raíz**.
* Se normalizaron los saltos de línea (`LF` a `CRLF`) automáticos para asegurar la compatibilidad multiplataforma en entornos Windows.
* Se realizó el primer commit de seguimiento integrando ambos proyectos.

---

## 💻 Comandos de Control Interno (Desarrollo Individual)

Mientras se configura el arranque global automatizado en la raíz, los proyectos pueden controlarse individualmente ingresando a sus carpetas:

* **Para correr el Backend:**
  ```bash
  cd Backend
  ./mvnw spring-boot:run
  ```
* **Para correr el Frontend:**
  ```bash
  cd frontend
  npm run dev
  ```

## Configuración de Stock 360

El frontend usa React + Vite y Microsoft Entra ID mediante MSAL. Copia `frontend/.env.example` a `frontend/.env.local` y configura el registro de aplicación, el scope expuesto por el backend y la URL pública de AWS API Gateway.

El backend valida los JWT como OAuth2 Resource Server. En EC2 configura `AZURE_ISSUER_URI`, `AZURE_API_AUDIENCE`, `CORS_ALLOWED_ORIGINS` y las variables `SPRING_DATASOURCE_*` de la base PostgreSQL administrada. Para desarrollo, los valores por defecto usan H2 en memoria.

> Firebase Authentication no es el mismo IDaaS que Microsoft Entra ID. Esta implementación sigue la especificación de la evaluación (Azure AD/Entra ID); no se deben mezclar tokens Firebase con el issuer de Entra.
