## Spring Data MongoDB: `@Document`, repositorios y consultas

En este apartado se prepara la capa de persistencia de la aplicación de **reservas de pistas deportivas** utilizando **Spring Data MongoDB**. El objetivo es dejar definidos:

* Los **documentos** (clases de dominio) mapeados a MongoDB.
* Los **repositorios** (`MongoRepository`) para operaciones CRUD.
* Un conjunto inicial de **consultas** típicas que se utilizarán más adelante en la API REST.

En el siguiente apartado se construirán los **DTOs** y los **endpoints**, por lo que aquí se evita exponer directamente las entidades como contrato de la API.

### Dependencias necesarias

Este apartado asume que el proyecto ya incluye el *starter*:

* `spring-boot-starter-data-mongodb`

y que la conexión está configurada en `application.yml` con la URI construida mediante variables de entorno.


### Diseño del modelo y decisiones de persistencia

Partimos del diagrama proporcionado, con estas decisiones explícitas:

* `Instalacion` es un **documento independiente** (colección `instalaciones`).
* `Usuario` es un **documento independiente** (colección `usuarios`).
* `Reserva` es un **documento independiente** (colección `reservas`).
* `Reserva` **embebe** `Horario`.
* `Horario` **embebe** un “snapshot” de la instalación (en vez de referenciarla directamente).
* `Reserva` **referencia** a `Usuario` mediante `usuarioId` (String que representa el `_id`).

Esto permite:

* Consultar reservas sin necesidad de joins.
* Mantener en la reserva los datos de instalación tal como estaban en el momento de la reserva.
* Evitar duplicar datos de usuario en cada reserva.

### Paquetes recomendados

Para mantener orden y escalabilidad:

```
com.dam.reservas
  model
  repo
```

### 1) Documentos y embebidos

#### `model/Instalacion.java`

```java
package com.dam.reservas.model;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "instalaciones")
public class Instalacion {

  @Id
  private String id;

  @NotBlank(message = "nombre es obligatorio")
  private String nombre;

  @NotBlank(message = "direccion es obligatoria")
  private String direccion;

  @NotBlank(message = "ciudad es obligatoria")
  private String ciudad;

  public Instalacion() {}

  public Instalacion(String nombre, String direccion, String ciudad) {
    this.nombre = nombre;
    this.direccion = direccion;
    this.ciudad = ciudad;
  }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public String getNombre() { return nombre; }
  public void setNombre(String nombre) { this.nombre = nombre; }

  public String getDireccion() { return direccion; }
  public void setDireccion(String direccion) { this.direccion = direccion; }

  public String getCiudad() { return ciudad; }
  public void setCiudad(String ciudad) { this.ciudad = ciudad; }
}
```

#### `model/Usuario.java`

```java
package com.dam.reservas.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "usuarios")
public class Usuario {

  @Id
  private String id;

  @NotBlank(message = "nombre es obligatorio")
  private String nombre;

  @NotBlank(message = "email es obligatorio")
  @Email(message = "email no válido")
  private String email;

  public Usuario() {}

  public Usuario(String nombre, String email) {
    this.nombre = nombre;
    this.email = email;
  }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public String getNombre() { return nombre; }
  public void setNombre(String nombre) { this.nombre = nombre; }

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
}
```

#### `model/InstalacionSnapshot.java` (embebido)

Este subdocumento es la “foto” de la instalación incluida dentro de la reserva, cumpliendo la composición del diagrama.

```java
package com.dam.reservas.model;

import jakarta.validation.constraints.NotBlank;

public class InstalacionSnapshot {

  @NotBlank(message = "instalacionId es obligatorio")
  private String instalacionId;

  @NotBlank(message = "nombre es obligatorio")
  private String nombre;

  @NotBlank(message = "direccion es obligatoria")
  private String direccion;

  @NotBlank(message = "ciudad es obligatoria")
  private String ciudad;

  public InstalacionSnapshot() {}

  public InstalacionSnapshot(String instalacionId, String nombre, String direccion, String ciudad) {
    this.instalacionId = instalacionId;
    this.nombre = nombre;
    this.direccion = direccion;
    this.ciudad = ciudad;
  }

  public String getInstalacionId() { return instalacionId; }
  public void setInstalacionId(String instalacionId) { this.instalacionId = instalacionId; }

  public String getNombre() { return nombre; }
  public void setNombre(String nombre) { this.nombre = nombre; }

  public String getDireccion() { return direccion; }
  public void setDireccion(String direccion) { this.direccion = direccion; }

  public String getCiudad() { return ciudad; }
  public void setCiudad(String ciudad) { this.ciudad = ciudad; }
}
```

#### `model/Horario.java` (embebido)

Se define como objeto embebido, no como documento. Para facilitar consultas y validación, se modela con tipos Java adecuados:

* `LocalDate` para el día.
* `LocalTime` para horas.

```java
package com.dam.reservas.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public class Horario {

  @NotNull(message = "dia es obligatorio")
  private LocalDate dia;

  @NotNull(message = "horaInicio es obligatoria")
  private LocalTime horaInicio;

  @NotNull(message = "horaFin es obligatoria")
  private LocalTime horaFin;

  @NotNull(message = "instalacionSnapshot es obligatorio")
  @Valid
  private InstalacionSnapshot instalacionSnapshot;

  public Horario() {}

  public Horario(LocalDate dia, LocalTime horaInicio, LocalTime horaFin, InstalacionSnapshot instalacionSnapshot) {
    this.dia = dia;
    this.horaInicio = horaInicio;
    this.horaFin = horaFin;
    this.instalacionSnapshot = instalacionSnapshot;
  }

  public LocalDate getDia() { return dia; }
  public void setDia(LocalDate dia) { this.dia = dia; }

  public LocalTime getHoraInicio() { return horaInicio; }
  public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

  public LocalTime getHoraFin() { return horaFin; }
  public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }

  public InstalacionSnapshot getInstalacionSnapshot() { return instalacionSnapshot; }
  public void setInstalacionSnapshot(InstalacionSnapshot instalacionSnapshot) { this.instalacionSnapshot = instalacionSnapshot; }

  public boolean rangoValido() {
    return horaInicio != null && horaFin != null && horaInicio.isBefore(horaFin);
  }
}
```

#### `model/Reserva.java`

Se define `Reserva` como documento. Incluye:

* `fechaReserva`: marca temporal del momento en que se registró la reserva.
* `horario`: embebido.
* `usuarioId`: referencia al usuario.

```java
package com.dam.reservas.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "reservas")
public class Reserva {

  @Id
  private String id;

  @NotNull(message = "fechaReserva es obligatoria")
  private Instant fechaReserva;

  @NotNull(message = "horario es obligatorio")
  @Valid
  private Horario horario;

  @NotBlank(message = "usuarioId es obligatorio")
  private String usuarioId;

  public Reserva() {}

  public Reserva(Instant fechaReserva, Horario horario, String usuarioId) {
    this.fechaReserva = fechaReserva;
    this.horario = horario;
    this.usuarioId = usuarioId;
  }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public Instant getFechaReserva() { return fechaReserva; }
  public void setFechaReserva(Instant fechaReserva) { this.fechaReserva = fechaReserva; }

  public Horario getHorario() { return horario; }
  public void setHorario(Horario horario) { this.horario = horario; }

  public String getUsuarioId() { return usuarioId; }
  public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
}
```

### 2) Repositorios (`MongoRepository`) y consultas

Los repositorios proporcionan CRUD automático y permiten definir consultas por convención de nombres, sin necesidad de escribir SQL.

#### `repo/InstalacionRepository.java`

Consultas típicas:

* Por ciudad
* Búsqueda parcial por nombre

```java
package com.dam.reservas.repo;

import com.dam.reservas.model.Instalacion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface InstalacionRepository extends MongoRepository<Instalacion, String> {

  List<Instalacion> findByCiudadIgnoreCase(String ciudad);

  List<Instalacion> findByNombreContainingIgnoreCase(String q);
}
```

#### `repo/UsuarioRepository.java`

Consultas típicas:

* Buscar por email (único lógico)

```java
package com.dam.reservas.repo;

import com.dam.reservas.model.Usuario;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {

  Optional<Usuario> findByEmailIgnoreCase(String email);
}
```

#### `repo/ReservaRepository.java`

Consultas típicas del dominio:

* Reservas por usuario.
* Reservas por instalación (usando el snapshot embebido).
* Reservas por día.
* Reservas por instalación y día (base para comprobar solapes).

```java
package com.dam.reservas.repo;

import com.dam.reservas.model.Reserva;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface ReservaRepository extends MongoRepository<Reserva, String> {

  List<Reserva> findByUsuarioId(String usuarioId);

  List<Reserva> findByHorario_InstalacionSnapshot_InstalacionId(String instalacionId);

  List<Reserva> findByHorario_Dia(LocalDate dia);

  List<Reserva> findByHorario_InstalacionSnapshot_InstalacionIdAndHorario_Dia(String instalacionId, LocalDate dia);
}
```

Para más información sobre métodos ed MongoRepository, acudir a su Web: <https://docs.spring.io/spring-data/mongodb/reference/mongodb/repositories/query-methods.html>.

### Consideraciones de consulta y modelado

#### Consultar por datos embebidos

MongoDB permite consultar campos dentro de documentos anidados. Spring Data lo refleja mediante el uso del guion bajo en el nombre del método:

* `findByHorario_Dia(...)`
* `findByHorario_InstalacionSnapshot_InstalacionId(...)`

Esto evita escribir consultas manuales y favorece la claridad del código.

#### Recomendación sobre índices (preparación para rendimiento)

Aunque no es imprescindible en un entorno docente inicial, en un proyecto real se crean índices para acelerar consultas frecuentes. Dado que se consultará con mucha frecuencia por:

* `usuarioId`
* `horario.dia`
* `horario.instalacionSnapshot.instalacionId`

En apartados posteriores se puede introducir `@Indexed` en el documento `Reserva` si se desea.

### Estado del proyecto tras este apartado

Al finalizar este apartado, el proyecto queda preparado con:

* Clases de dominio mapeadas a MongoDB con `@Document`.
* Objetos embebidos definidos conforme al diagrama.
* Repositorios con CRUD y consultas base.

En el siguiente apartado comenzaremos:

* **DTOs de entrada y salida**
* **endpoints REST**
* validación a nivel de API y códigos HTTP

De este modo, se evita que las entidades de persistencia se utilicen directamente como contrato de la API, manteniendo una separación clara entre capas.

\pagebreak