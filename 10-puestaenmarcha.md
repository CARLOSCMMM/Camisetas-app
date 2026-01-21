## Caso práctico completo (paso a paso)

En este apartado se recorre **todo el flujo funcional de la aplicación**, desde el arranque del entorno hasta la verificación final del sistema, siguiendo un orden lógico y reproducible. El objetivo es que el alumnado pueda **comprobar que todos los criterios de evaluación se cumplen** mediante un caso real y completo.

El caso práctico se desarrolla en **tres niveles**:

1. Verificación técnica del entorno.
2. Uso de la API REST (backend).
3. Uso de la interfaz web (frontend).

---

## Paso 1: Arranque del entorno

### 1.1 Abrir el proyecto en Dev Container

1. Abrir la carpeta del proyecto en VS Code.
2. Seleccionar **“Reopen in Container”**.
3. Esperar a que:

   * Se levanten los contenedores (`app`, `mongo`, `mongo-express`).
   * El contenedor de desarrollo esté operativo.

### 1.2 Verificar servicios

Desde el terminal del contenedor:

```bash
docker ps
```

Debe aparecer:

* Un contenedor MongoDB en ejecución.
* Un contenedor Mongo Express en ejecución.

Accesos:

* Mongo Express: `http://localhost:8081`
* API (cuando arranque Spring): `http://localhost:8080`

---

## Paso 2: Arranque del backend Spring Boot

Desde el terminal integrado del contenedor:

```bash
./mvnw spring-boot:run
```

Verificaciones en el log:

* Spring Boot arranca sin errores.
* Conexión correcta a MongoDB.
* API escuchando en el puerto 8080.

Este paso valida:

* **b)** Establecimiento de conexiones.
* **h)** Correcto despliegue de la aplicación.

---

## Paso 3: Creación de datos base (API REST)

Antes de usar el frontend, se crean datos base mediante la API. Esto permite comprobar el funcionamiento independiente del backend.

### 3.1 Crear instalaciones

POST `/api/instalaciones`

```json
{
  "nombre": "Pista Central",
  "direccion": "C/ Mayor 1",
  "ciudad": "Jaén"
}
```

Crear al menos dos instalaciones (por ejemplo, una pista cubierta y una al aire libre).

Verificar:

* Código HTTP **201 Created**.
* Documento almacenado en la colección `instalaciones`.

Criterios evaluados:

* **c)** Persistencia de objetos simples.
* **e)** Consultas básicas (GET).

---

### 3.2 Crear usuarios

POST `/api/usuarios`

```json
{
  "nombre": "Ana Pérez",
  "email": "ana@ejemplo.com"
}
```

Crear al menos dos usuarios distintos.

Verificar:

* Código HTTP **201 Created**.
* Documentos en la colección `usuarios`.

Criterios evaluados:

* **c)** Persistencia de objetos simples.
* **e)** Consultas.

---

## Paso 4: Creación de reservas (regla de negocio)

### 4.1 Crear una reserva válida

POST `/api/reservas`

```json
{
  "usuarioId": "ID_USUARIO",
  "instalacionId": "ID_INSTALACION",
  "dia": "2026-01-20",
  "horaInicio": "18:00",
  "horaFin": "19:00"
}
```

Verificar:

* Código HTTP **201 Created**.
* Documento en `reservas` con:

  * `horario` embebido.
  * `instalacionSnapshot` embebido.
  * `usuarioId` como referencia.

Criterios evaluados:

* **d)** Persistencia de objetos estructurados.
* **e)** Consultas sobre datos embebidos.

---

### 4.2 Intentar una reserva solapada (conflicto)

POST `/api/reservas`

```json
{
  "usuarioId": "OTRO_USUARIO",
  "instalacionId": "MISMA_INSTALACION",
  "dia": "2026-01-20",
  "horaInicio": "18:30",
  "horaFin": "19:30"
}
```

Resultado esperado:

* Código HTTP **409 Conflict**.
* Respuesta `ApiError` indicando solape de reservas.

Este paso valida:

* **f)** Modificación/gestión de objetos almacenados.
* **g)** Gestión de transacciones a nivel lógico (control de integridad).

---

## Paso 5: Consultas avanzadas

### 5.1 Consultar reservas por instalación y día

GET `/api/reservas?instalacionId=...&dia=2026-01-20`

Verificar:

* Se devuelven solo las reservas correspondientes.
* No aparecen reservas de otras instalaciones o días.

### 5.2 Consultar reservas por usuario

GET `/api/reservas?usuarioId=...`

Verificar:

* Se devuelven solo las reservas del usuario indicado.

Criterios evaluados:

* **e)** Desarrollo de consultas.
* Uso de filtros y parámetros de consulta.

---

## Paso 6: Uso del frontend (flujo completo)

Acceder desde el navegador a:

```
http://localhost:8080
```

### 6.1 Gestión de instalaciones

* Crear una nueva instalación desde el formulario.
* Verla reflejada en la tabla.
* Eliminarla y comprobar que desaparece.

### 6.2 Gestión de usuarios

* Crear un nuevo usuario.
* Verlo en la tabla y en los desplegables de reservas.
* Eliminarlo (si no tiene reservas asociadas).

### 6.3 Gestión de reservas

* Crear una reserva desde la interfaz.
* Comprobar que aparece en la tabla.
* Intentar crear una reserva solapada y observar el mensaje de error.
* Filtrar reservas por:

  * Usuario
  * Instalación
  * Día

Este paso valida:

* Integración frontend–backend.
* Gestión de errores desde el cliente.
* Consumo correcto de la API REST.

---

## Paso 7: Verificación en Mongo Express

Acceder a `http://localhost:8081` y comprobar:

* Colecciones creadas automáticamente:

  * `instalaciones`
  * `usuarios`
  * `reservas`
* Estructura de los documentos:

  * Uso de `_id` (ObjectId).
  * Documentos embebidos (`horario`, `instalacionSnapshot`).
  * Referencias (`usuarioId`).

Este paso refuerza:

* **a)** Comprensión del modelo documental.
* Diferencias frente a bases de datos relacionales.

---

## Paso 8: Documentación y conclusiones

Para cerrar el caso práctico, el alumnado debe documentar:

* Estructura del proyecto.
* Decisiones de modelado (embebido vs referencia).
* Endpoints implementados.
* Ejemplo de error 409 por solape.
* Capturas de:

  * API funcionando.
  * Interfaz web.
  * Documentos en MongoDB.

Criterio evaluado:

* **h)** Pruebas y documentación de la aplicación desarrollada.


## Resultado final del caso práctico

Al finalizar este apartado se ha demostrado, de forma práctica y verificable, que la aplicación:

* Usa MongoDB como base de datos documental.
* Gestiona objetos simples y estructurados.
* Implementa reglas de negocio reales.
* Expone una API REST coherente.
* Integra frontend y backend correctamente.
* Está correctamente probada y documentada.

En el siguiente y último apartado se presentarán **extensiones y mejoras** (paginación, búsqueda avanzada, logs, Docker, seguridad), que pueden utilizarse como ampliación, reto o trabajo voluntario.
