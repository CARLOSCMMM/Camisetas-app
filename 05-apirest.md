## Diseño de la API REST: endpoints, DTOs, códigos HTTP

En este apartado se define el **contrato REST** de la aplicación de reservas de pistas deportivas: qué recursos existen, qué endpoints se exponen, qué datos se intercambian mediante **DTOs**, y qué **códigos HTTP** se deben devolver en cada operación. Esta definición es clave para desacoplar el frontend del backend y para garantizar una API coherente, predecible y fácilmente testeable.

El diseño se basa en el modelo ya establecido:

* `Instalacion` (documento independiente)
* `Usuario` (documento independiente)
* `Reserva` (documento principal), que embebe `Horario` y el snapshot de instalación, y referencia a usuario mediante `usuarioId`


### Principios de diseño aplicados

1. **Recursos como sustantivos**
   Las rutas representan recursos (`/reservas`, `/usuarios`, `/instalaciones`) y las operaciones se expresan con métodos HTTP.

2. **JSON como formato de intercambio**
   Todas las peticiones y respuestas utilizan JSON.

3. **Separación entre entidades y DTOs**
   Las clases del paquete `model` no se exponen directamente en la API. Se diseñan DTOs específicos de entrada y salida.

4. **Códigos HTTP coherentes**
   Cada endpoint debe responder con el código más apropiado, evitando respuestas ambiguas.

5. **Validación en el borde (API)**
   Los DTOs de entrada se validan con `@Valid` y anotaciones de Bean Validation para evitar guardar datos inválidos.


### Recursos y endpoints

#### 1) Recurso: Instalaciones

Las instalaciones representan las pistas o instalaciones deportivas disponibles.

##### Endpoints propuestos

| Método | Ruta                      | Descripción                                   | Respuesta       |
| ------ | ------------------------- | --------------------------------------------- | --------------- |
| GET    | `/api/instalaciones`      | Listar instalaciones (con filtros opcionales) | 200 + lista     |
| GET    | `/api/instalaciones/{id}` | Obtener una instalación                       | 200 / 404       |
| POST   | `/api/instalaciones`      | Crear instalación                             | 201 / 400       |
| PUT    | `/api/instalaciones/{id}` | Actualizar instalación                        | 200 / 400 / 404 |
| DELETE | `/api/instalaciones/{id}` | Eliminar instalación                          | 204 / 404       |

Filtros recomendados:

* `?ciudad=Jaén`
* `?q=pista` (búsqueda por nombre)

#### 2) Recurso: Usuarios

Los usuarios se almacenan como documentos independientes y se referencian desde la reserva por `usuarioId`.

##### Endpoints propuestos

| Método | Ruta                 | Descripción        | Respuesta             |
| ------ | -------------------- | ------------------ | --------------------- |
| GET    | `/api/usuarios`      | Listar usuarios    | 200 + lista           |
| GET    | `/api/usuarios/{id}` | Obtener usuario    | 200 / 404             |
| POST   | `/api/usuarios`      | Crear usuario      | 201 / 400 / 409       |
| PUT    | `/api/usuarios/{id}` | Actualizar usuario | 200 / 400 / 404 / 409 |
| DELETE | `/api/usuarios/{id}` | Eliminar usuario   | 204 / 404             |

Se recomienda tratar `email` como identificador lógico único (aunque el índice único se puede introducir más adelante como extensión).


#### 3) Recurso: Reservas

La reserva es el recurso principal del sistema.

* Embebe `Horario`
* `Horario` embebe `InstalacionSnapshot`
* Referencia a usuario por `usuarioId`

##### Endpoints propuestos

| Método | Ruta                 | Descripción                    | Respuesta             |
| ------ | -------------------- | ------------------------------ | --------------------- |
| GET    | `/api/reservas`      | Listar reservas (con filtros)  | 200 + lista           |
| GET    | `/api/reservas/{id}` | Obtener una reserva            | 200 / 404             |
| POST   | `/api/reservas`      | Crear reserva                  | 201 / 400 / 404 / 409 |
| PUT    | `/api/reservas/{id}` | Actualizar reserva (reemplazo) | 200 / 400 / 404 / 409 |
| DELETE | `/api/reservas/{id}` | Eliminar reserva               | 204 / 404             |

Filtros recomendados para `GET /api/reservas`:

* `?usuarioId=...`
* `?instalacionId=...`
* `?dia=2026-01-18`
* combinación: `?instalacionId=...&dia=...` (base para evitar solapes)

##### Conflicto por solape (409)

Si se intenta crear/modificar una reserva que se solapa con otra reserva existente de la misma instalación y mismo día, el endpoint debe responder:

* **409 Conflict**
* cuerpo JSON con detalle del motivo


### DTOs (Data Transfer Objects)

Se definen DTOs de entrada (request) y salida (response). Los DTOs de entrada se diseñan con los campos mínimos necesarios, evitando que el cliente tenga que construir manualmente objetos embebidos como el snapshot de instalación. El backend se encarga de construir el snapshot consultando la instalación.

#### DTOs para Instalaciones

##### `InstalacionRequest`

Campos requeridos para crear/actualizar:

* `nombre`
* `direccion`
* `ciudad`

##### `InstalacionResponse`

Incluye además el `id`.

#### DTOs para Usuarios

##### `UsuarioRequest`

* `nombre`
* `email`

##### `UsuarioResponse`

Incluye `id`.


#### DTOs para Reservas

La reserva necesita:

* Referencia a usuario (`usuarioId`)
* Referencia a instalación (`instalacionId`) para construir el snapshot en el backend
* Día y franja horaria

##### `ReservaCreateRequest` (entrada)

* `usuarioId`
* `instalacionId`
* `dia`
* `horaInicio`
* `horaFin`

##### `ReservaResponse` (salida)

* `id`
* `fechaReserva` (timestamp)
* `usuarioId`
* `horario` con snapshot de instalación embebido

Se recomienda que el response incluya el horario embebido completo, ya que es precisamente el valor añadido del modelo documental.


### Códigos HTTP y comportamiento esperado

#### Respuestas típicas

* **200 OK**
  Operación correcta en GET/PUT.

* **201 Created**
  Recurso creado correctamente (POST).
  Recomendación: devolver el recurso creado.

* **204 No Content**
  Eliminación correcta (DELETE) sin cuerpo de respuesta.

* **400 Bad Request**
  Datos inválidos (validación), formato incorrecto o parámetros incoherentes.

* **404 Not Found**
  Recurso inexistente (por ejemplo usuarioId inválido al reservar).

* **409 Conflict**
  Violación de regla de negocio, típicamente:

  * Solape de reserva
  * Email duplicado (si se impone unicidad)

#### Estructura de errores

Para mantener uniformidad, se recomienda una estructura JSON única para los errores, gestionada por `@ControllerAdvice`:

```json
{
  "timestamp": "2026-01-18T18:45:00Z",
  "status": 400,
  "error": "VALIDATION",
  "message": "Datos no válidos",
  "details": [
    "dia: es obligatorio",
    "horaFin: debe ser posterior a horaInicio"
  ]
}
```

### Ejemplos de intercambio (contrato)

#### Crear reserva (POST `/api/reservas`)

Request:

```json
{
  "usuarioId": "65a199f3e0b1c2d3e4f56789",
  "instalacionId": "65a18888aa11223344556677",
  "dia": "2026-01-18",
  "horaInicio": "18:00",
  "horaFin": "19:00"
}
```

Respuestas:

* 201 si se crea
* 404 si el usuario o la instalación no existen
* 409 si la franja se solapa

### Preparación para el siguiente apartado

Tras definir el contrato REST, el siguiente paso es implementarlo:

1. Crear los DTOs de entrada/salida.
2. Mapear entidades ↔ DTOs.
3. Implementar controladores y servicios aplicando:

   * Validación (`@Valid`)
   * Códigos HTTP correctos
   * Gestión uniforme de errores (`ControllerAdvice`)
   * Regla de negocio de solape (409 Conflict)

En el siguiente apartado se desarrollarán los **DTOs concretos y su validación**, dejando los controladores listos para implementar los endpoints de la API.

\pagebreak