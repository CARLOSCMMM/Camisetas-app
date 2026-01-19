## Validación y control de errores (`@ControllerAdvice`)

En una API REST, la validación de datos y la gestión homogénea de errores son elementos esenciales para garantizar que el sistema:

* Rechaza entradas inválidas antes de persistirlas.
* Responde con **códigos HTTP correctos** y mensajes consistentes.
* Facilita el consumo desde el frontend (manejo de errores predecible).
* Simplifica el mantenimiento: los controladores no se llenan de `try/catch`.

En este apartado se implementan dos bloques:

1. **Validación de DTOs** mediante Bean Validation (`jakarta.validation`) y `@Valid`.
2. **Manejo centralizado de excepciones** mediante `@ControllerAdvice`.


### 1) Validación de entrada en la API

La validación se realiza en los **DTOs de entrada** (request), no en las entidades del modelo persistente. Esto permite:

* Separar contrato de API y persistencia.
* Validar solo lo que el cliente debe enviar.
* Mantener reglas coherentes aunque la persistencia evolucione.

#### Dependencia necesaria

Debe estar incluida la dependencia:

* `spring-boot-starter-validation`

#### Activación de validación en controladores

En cada endpoint que reciba datos, se debe usar:

* `@Valid` en el parámetro del DTO.
* `@RequestBody` para deserializar JSON.

Ejemplo conceptual:

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public ReservaResponse crear(@Valid @RequestBody ReservaCreateRequest req) { ... }
```

Si el DTO no cumple las restricciones, Spring lanzará automáticamente una `MethodArgumentNotValidException`.


### 2) DTOs con anotaciones de Bean Validation

A continuación se incluyen los DTOs mínimos para el dominio, con validación.

#### `dto/InstalacionRequest.java`

```java
package com.dam.reservas.dto;

import jakarta.validation.constraints.NotBlank;

public record InstalacionRequest(
    @NotBlank(message = "nombre es obligatorio") String nombre,
    @NotBlank(message = "direccion es obligatoria") String direccion,
    @NotBlank(message = "ciudad es obligatoria") String ciudad
) {}
```

#### `dto/UsuarioRequest.java`

```java
package com.dam.reservas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UsuarioRequest(
    @NotBlank(message = "nombre es obligatorio") String nombre,
    @NotBlank(message = "email es obligatorio") @Email(message = "email no válido") String email
) {}
```

#### `dto/ReservaCreateRequest.java`

Se valida que los campos existan. La relación `horaInicio < horaFin` es una regla de negocio; se validará en la capa de servicio para poder responder con 400/409 según corresponda.

```java
package com.dam.reservas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaCreateRequest(
    @NotBlank(message = "usuarioId es obligatorio") String usuarioId,
    @NotBlank(message = "instalacionId es obligatorio") String instalacionId,
    @NotNull(message = "dia es obligatorio") LocalDate dia,
    @NotNull(message = "horaInicio es obligatoria") LocalTime horaInicio,
    @NotNull(message = "horaFin es obligatoria") LocalTime horaFin
) {}
```


### 3) Modelo de error unificado

Se define un formato de respuesta común para los errores. Esto permite al frontend:

* Mostrar mensajes coherentes.
* Leer detalles campo a campo cuando procede.
* Diferenciar tipos de error (`VALIDATION`, `NOT_FOUND`, etc.).

#### `web/ApiError.java`

```java
package com.dam.reservas.web;

import java.time.Instant;
import java.util.List;

public record ApiError(
    Instant timestamp,
    int status,
    String error,
    String message,
    List<String> details
) {}
```

### 4) Excepciones de dominio

Se definen excepciones específicas que se lanzarán desde la capa de servicio:

* `NotFoundException` $\rightarrow$ 404
* `BadRequestException` $\rightarrow$ 400
* `ConflictException` $\rightarrow$ 409 (por solapes u otras reglas)

#### `web/NotFoundException.java`

```java
package com.dam.reservas.web;

public class NotFoundException extends RuntimeException {
  public NotFoundException(String message) { super(message); }
}
```

#### `web/BadRequestException.java`

```java
package com.dam.reservas.web;

public class BadRequestException extends RuntimeException {
  public BadRequestException(String message) { super(message); }
}
```

#### `web/ConflictException.java`

```java
package com.dam.reservas.web;

public class ConflictException extends RuntimeException {
  public ConflictException(String message) { super(message); }
}
```


### 5) Manejo centralizado con `@ControllerAdvice`

Con `@ControllerAdvice` se interceptan excepciones lanzadas en cualquier controlador y se transforman en respuestas HTTP con el formato establecido.

#### `web/GlobalExceptionHandler.java`

```java
package com.dam.reservas.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
        new ApiError(Instant.now(), 404, "NOT_FOUND", ex.getMessage(), List.of())
    );
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
        new ApiError(Instant.now(), 400, "BAD_REQUEST", ex.getMessage(), List.of())
    );
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(
        new ApiError(Instant.now(), 409, "CONFLICT", ex.getMessage(), List.of())
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {

    List<String> details = ex.getBindingResult().getAllErrors().stream()
        .map(err -> {
          if (err instanceof FieldError fe) {
            return fe.getField() + ": " + fe.getDefaultMessage();
          }
          return err.getDefaultMessage();
        })
        .toList();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
        new ApiError(Instant.now(), 400, "VALIDATION", "Datos no válidos", details)
    );
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGeneric(Exception ex) {
    // En un entorno docente puede resultar útil devolver el mensaje.
    // En producción se recomienda no exponer detalles internos.
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        new ApiError(Instant.now(), 500, "INTERNAL_ERROR", "Error interno del servidor", List.of(ex.getMessage()))
    );
  }
}
```


### 6) Reglas de negocio y códigos HTTP

No toda validación es “campo requerido”. En una aplicación real aparecen reglas como:

* `horaInicio` debe ser anterior a `horaFin` $\rightarrow$ **400 Bad Request**
* No se puede reservar una pista si ya hay una reserva solapada $\rightarrow$ **409 Conflict**
* No se puede reservar para un usuario inexistente $\rightarrow$ **404 Not Found**

Estas reglas se implementarán en la capa de servicio y lanzarán las excepciones definidas, para que el `@ControllerAdvice` las convierta en respuestas coherentes.



### Resultado de este apartado

Al finalizar este apartado se dispone de:

* DTOs de entrada con validación.
* Un formato de error uniforme (`ApiError`).
* Excepciones de dominio.
* Un `@ControllerAdvice` que convierte errores en respuestas HTTP coherentes.

En el siguiente apartado se implementará **CORS y configuración por entornos**, garantizando que el frontend pueda consumir la API de forma segura y controlada en desarrollo y despliegue.

\pagebreak