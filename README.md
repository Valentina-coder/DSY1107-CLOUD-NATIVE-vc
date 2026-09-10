# DSY1107-CLOUD-NATIVE-vc
# DSY1107 - Cloud Native Project 

Este repositorio contiene una arquitectura unificada (**Stock360**) que integra un backend desarrollado en **Spring Boot** y un frontend moderno con **Vite**. Ambos entornos están configurados para coexistir en un mismo repositorio y ejecutarse en paralelo de forma eficiente.

---

## 🛠️ Estructura del Proyecto

La estructura actual del directorio principal se organiza de la siguiente manera:

```text
DSY1107-CLOUD-NATIVE-vc/
│
├── Backend/            <-- API Rest en Java con Spring Boot (Puerto: 8080)
│   ├── src/
│   └── pom.xml
│
├── frontend/           <-- Aplicación web SPA con Vite (Puerto: 5173)
│   ├── src/
│   └── package.json
│
├── .gitignore          <-- Configuración para evitar subir dependencias pesadas
└── package.json        <-- Script de automatización de la raíz
```

---

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
