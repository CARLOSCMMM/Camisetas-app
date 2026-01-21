## Spring Security, login y ACLs contra MongoDB

En este apartado vamos a añadir Spring Security, login con usuarios en MongoDB y autorización por roles a nuestro proyecto.

En esta mejora se incorpora **Spring Security** para que **todas las operaciones** del sistema requieran identificación y se aplica **control de acceso** según el tipo de usuario:

* **ADMIN**
  * CRUD de **usuarios**, **instalaciones** y **horarios disponibles**.
* **CONSERJE**
  * Solo puede ver **las pistas reservadas del día actual**, sin conocer el usuario que reserva.
* **CLIENTE**
  * Solo puede ver **sus reservas**.
  * Puede **modificar o borrar** una reserva **si aún no ha pasado** (fecha/hora futura).
  * Puede consultar **pistas disponibles** para una fecha y franja horaria para poder reservar.

La autenticación se realizará con **usuarios almacenados en MongoDB** (colección `usuarios`) usando como “username” el **email**, y contraseñas guardadas como **hash BCrypt**.

A continuación detallamos **las modificaciones del backend**: `pom.xml`, modelo, repositorios, seguridad, DTOs mínimos de seguridad y controladores/servicios necesarios para cumplir las reglas.

### 1) `pom.xml`: dependencias nuevas

Añadir Spring Security:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

(¡Ojo! se mantiene Web, Data MongoDB y Validation.)

### 2) Modelo: añadir rol y contraseña al usuario

#### 2.1 Enum de rol

Para implementar los tipos de usuarios podemos hacerlo mediante una tabla y dejarlo abierto (más complicado para el backend) o bien mediante un enum de Java y dejarlo más atado. En nuestro caso usaremos **enum**, así creamos el archivo `model/RolUsuario.java`:

```java
package com.dam.reservas.model;

public enum RolUsuario {
  ADMIN, CONSERJE, CLIENTE
}
```

#### 2.2 Modificar `Usuario`

**Cambios**:

* añadir `passwordHash` (BCrypt)
* añadir `rol`

`model/Usuario.java`

```java
package com.dam.reservas.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

  // Guardar SIEMPRE hash BCrypt, nunca texto plano
  @NotBlank(message = "passwordHash es obligatorio")
  private String passwordHash;

  @NotNull(message = "rol es obligatorio")
  private RolUsuario rol;

  public Usuario() {}

  public Usuario(String nombre, String email, String passwordHash, RolUsuario rol) {
    this.nombre = nombre;
    this.email = email;
    this.passwordHash = passwordHash;
    this.rol = rol;
  }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public String getNombre() { return nombre; }
  public void setNombre(String nombre) { this.nombre = nombre; }

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }

  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

  public RolUsuario getRol() { return rol; }
  public void setRol(RolUsuario rol) { this.rol = rol; }
}
```


### 3) Repositorio de usuarios: login por email

`repo/UsuarioRepository.java`

```java
package com.dam.reservas.repo;

import com.dam.reservas.model.Usuario;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {
  Optional<Usuario> findByEmailIgnoreCase(String email);
  boolean existsByEmailIgnoreCase(String email);
}
```


### 4) Seguridad: UserDetailsService + PasswordEncoder + configuración HTTP

#### 4.1 Principal propio (incluye id y rol)

`security/AuthUser.java`

```java
package com.dam.reservas.security;

import com.dam.reservas.model.RolUsuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AuthUser implements UserDetails {

  private final String id;
  private final String email;
  private final String passwordHash;
  private final RolUsuario rol;

  public AuthUser(String id, String email, String passwordHash, RolUsuario rol) {
    this.id = id;
    this.email = email;
    this.passwordHash = passwordHash;
    this.rol = rol;
  }

  public String getId() { return id; }
  public RolUsuario getRol() { return rol; }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    // Spring Security usa convención ROLE_*
    return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
  }

  @Override
  public String getPassword() { return passwordHash; }

  @Override
  public String getUsername() { return email; }

  @Override public boolean isAccountNonExpired() { return true; }
  @Override public boolean isAccountNonLocked() { return true; }
  @Override public boolean isCredentialsNonExpired() { return true; }
  @Override public boolean isEnabled() { return true; }
}
```

#### 4.2 UserDetailsService cargando desde MongoDB

`security/MongoUserDetailsService.java`

```java
package com.dam.reservas.security;

import com.dam.reservas.model.Usuario;
import com.dam.reservas.repo.UsuarioRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class MongoUserDetailsService implements UserDetailsService {

  private final UsuarioRepository usuarioRepo;

  public MongoUserDetailsService(UsuarioRepository usuarioRepo) {
    this.usuarioRepo = usuarioRepo;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Usuario u = usuarioRepo.findByEmailIgnoreCase(username)
        .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    return new AuthUser(u.getId(), u.getEmail(), u.getPasswordHash(), u.getRol());
  }
}
```

#### 4.3 Configuración de Spring Security

Se propone un enfoque didáctico y práctico:

* Permitir recursos estáticos (`/`, `/index.html`, `/js/**`, `/css/**`) sin login.
* Proteger toda la API (`/api/**`) exigiendo autenticación.
* Usar `formLogin()` (login por sesión/cookie).
* Desactivar CSRF para simplificar POST/PUT/DELETE desde jQuery (en producción se haría de otra forma).

`security/SecurityConfig.java`

```java
package com.dam.reservas.security;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity // habilita @PreAuthorize
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(
          "/", "/index.html",
          "/css/**", "/js/**",
          "/favicon.ico"
        ).permitAll()
        .requestMatchers("/api/**").authenticated()
        .anyRequest().permitAll()
      )
      .formLogin(form -> form.permitAll())
      .logout(logout -> logout.permitAll());

    return http.build();
  }
}
```

### 5) Nuevo documento: Horarios disponibles (para que ADMIN gestione horarios)

Como en el modelo original el `Horario` estaba embebido en `Reserva`, para poder cumplir “CRUD de horarios” se introduce un documento explícito de disponibilidad.

#### 5.1 Modelo `HorarioDisponible`

`model/HorarioDisponible.java`

```java
package com.dam.reservas.model;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalTime;

@Document(collection = "horarios")
public class HorarioDisponible {

  @Id
  private String id;

  @NotNull
  private String instalacionId;

  @NotNull
  private LocalDate dia;

  @NotNull
  private LocalTime horaInicio;

  @NotNull
  private LocalTime horaFin;

  public HorarioDisponible() {}

  public HorarioDisponible(String instalacionId, LocalDate dia, LocalTime horaInicio, LocalTime horaFin) {
    this.instalacionId = instalacionId;
    this.dia = dia;
    this.horaInicio = horaInicio;
    this.horaFin = horaFin;
  }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public String getInstalacionId() { return instalacionId; }
  public void setInstalacionId(String instalacionId) { this.instalacionId = instalacionId; }

  public LocalDate getDia() { return dia; }
  public void setDia(LocalDate dia) { this.dia = dia; }

  public LocalTime getHoraInicio() { return horaInicio; }
  public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

  public LocalTime getHoraFin() { return horaFin; }
  public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
}
```

#### 5.2 Repositorio

`repo/HorarioDisponibleRepository.java`

```java
package com.dam.reservas.repo;

import com.dam.reservas.model.HorarioDisponible;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface HorarioDisponibleRepository extends MongoRepository<HorarioDisponible, String> {
  List<HorarioDisponible> findByInstalacionIdAndDia(String instalacionId, LocalDate dia);
}
```

### 6) Reglas de negocio y endpoints por rol

Para mantener claro el control de acceso se definen rutas por “área”:

* `/api/admin/**` → ADMIN
* `/api/conserje/**` → CONSERJE
* `/api/cliente/**` → CLIENTE

Y se aplican permisos con `@PreAuthorize`.

#### 6.1 DTOs mínimos necesarios

##### Crear usuario (ADMIN) con contraseña en claro (solo en request)

El backend la convierte a `passwordHash`.

`dto/AdminUsuarioCreateRequest.java`

```java
package com.dam.reservas.dto;

import com.dam.reservas.model.RolUsuario;
import jakarta.validation.constraints.*;

public record AdminUsuarioCreateRequest(
  @NotBlank String nombre,
  @NotBlank @Email String email,
  @NotBlank @Size(min = 6, message = "password mínimo 6 caracteres") String password,
  @NotNull RolUsuario rol
) {}
```

`dto/AdminUsuarioResponse.java`

```java
package com.dam.reservas.dto;

import com.dam.reservas.model.RolUsuario;

public record AdminUsuarioResponse(
  String id,
  String nombre,
  String email,
  RolUsuario rol
) {}
```

##### Reserva (CLIENTE): el cliente NO envía usuarioId

`dto/ClienteReservaCreateRequest.java`

```java
package com.dam.reservas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record ClienteReservaCreateRequest(
  @NotBlank String instalacionId,
  @NotNull LocalDate dia,
  @NotNull LocalTime horaInicio,
  @NotNull LocalTime horaFin
) {}
```

##### Disponibilidad (CLIENTE): instalaciones libres para una franja

`dto/DisponibilidadRequest.java`

```java
package com.dam.reservas.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record DisponibilidadRequest(
  @NotNull LocalDate dia,
  @NotNull LocalTime horaInicio,
  @NotNull LocalTime horaFin
) {}
```

### 7) Servicios: garantizar reglas “solo sus reservas” y “no pasada”

#### 7.1 Utilidad para “reserva pasada”

Regla: una reserva se considera “pasada” si el día es anterior a hoy, o si es hoy y la horaFin ya ha pasado.

Se aplica en update/delete del cliente.

#### 7.2 ReservaRepository: consultas necesarias

Asegúrate de tener estas consultas (o equivalentes):

`repo/ReservaRepository.java` (añadir)

```java
List<Reserva> findByUsuarioIdAndHorario_Dia(String usuarioId, java.time.LocalDate dia);
List<Reserva> findByHorario_Dia(java.time.LocalDate dia);
```

Ya tenías otras por instalación y día, útiles para solapes.

### 8) Controladores por rol (backend)

#### 8.1 Admin: CRUD de usuarios, instalaciones y horarios

##### AdminUsuariosController

`web/AdminUsuariosController.java`

```java
package com.dam.reservas.web;

import com.dam.reservas.dto.*;
import com.dam.reservas.model.Usuario;
import com.dam.reservas.repo.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsuariosController {

  private final UsuarioRepository repo;
  private final PasswordEncoder encoder;

  public AdminUsuariosController(UsuarioRepository repo, PasswordEncoder encoder) {
    this.repo = repo;
    this.encoder = encoder;
  }

  @GetMapping
  public List<AdminUsuarioResponse> listar() {
    return repo.findAll().stream()
      .map(u -> new AdminUsuarioResponse(u.getId(), u.getNombre(), u.getEmail(), u.getRol()))
      .toList();
  }

  @PostMapping
  public AdminUsuarioResponse crear(@Valid @RequestBody AdminUsuarioCreateRequest req) {
    if (repo.existsByEmailIgnoreCase(req.email())) {
      throw new ConflictException("Ya existe un usuario con ese email");
    }
    Usuario u = new Usuario(req.nombre(), req.email(), encoder.encode(req.password()), req.rol());
    u = repo.save(u);
    return new AdminUsuarioResponse(u.getId(), u.getNombre(), u.getEmail(), u.getRol());
  }

  @DeleteMapping("/{id}")
  public void borrar(@PathVariable String id) {
    if (!repo.existsById(id)) throw new NotFoundException("Usuario no encontrado: " + id);
    repo.deleteById(id);
  }
}
```

Para instalaciones y horarios disponibles, el patrón es el mismo (`@PreAuthorize("hasRole('ADMIN')")`) reutilizando repositorios.

#### 8.2 Conserje: ver reservas del día (sin usuario)

Endpoint:

* `GET /api/conserje/reservas-hoy`

Devuelve: día, tramo, instalación (snapshot) — sin `usuarioId`.

`dto/ConserjeReservaHoyResponse.java`

```java
package com.dam.reservas.dto;

public record ConserjeReservaHoyResponse(
  String instalacionNombre,
  String instalacionCiudad,
  String horaInicio,
  String horaFin
) {}
```

`web/ConserjeController.java`

```java
package com.dam.reservas.web;

import com.dam.reservas.dto.ConserjeReservaHoyResponse;
import com.dam.reservas.repo.ReservaRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/conserje")
@PreAuthorize("hasRole('CONSERJE')")
public class ConserjeController {

  private final ReservaRepository reservaRepo;

  public ConserjeController(ReservaRepository reservaRepo) {
    this.reservaRepo = reservaRepo;
  }

  @GetMapping("/reservas-hoy")
  public List<ConserjeReservaHoyResponse> reservasHoy() {
    LocalDate hoy = LocalDate.now();
    return reservaRepo.findByHorario_Dia(hoy).stream()
      .map(r -> new ConserjeReservaHoyResponse(
        r.getHorario().getInstalacionSnapshot().getNombre(),
        r.getHorario().getInstalacionSnapshot().getCiudad(),
        r.getHorario().getHoraInicio().toString(),
        r.getHorario().getHoraFin().toString()
      ))
      .toList();
  }
}
```

#### 8.3 Cliente: solo sus reservas + disponibilidad + modificar/borrar si no ha pasado

Para obtener el usuario autenticado con su `id`, se usa `@AuthenticationPrincipal AuthUser`.

`web/ClienteReservasController.java`

```java
package com.dam.reservas.web;

import com.dam.reservas.dto.ClienteReservaCreateRequest;
import com.dam.reservas.dto.DisponibilidadRequest;
import com.dam.reservas.model.*;
import com.dam.reservas.repo.*;
import com.dam.reservas.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.List;

@RestController
@RequestMapping("/api/cliente")
@PreAuthorize("hasRole('CLIENTE')")
public class ClienteReservasController {

  private final ReservaRepository reservaRepo;
  private final InstalacionRepository instalacionRepo;

  public ClienteReservasController(ReservaRepository reservaRepo, InstalacionRepository instalacionRepo) {
    this.reservaRepo = reservaRepo;
    this.instalacionRepo = instalacionRepo;
  }

  @GetMapping("/reservas")
  public List<Reserva> misReservas(@AuthenticationPrincipal AuthUser auth) {
    return reservaRepo.findByUsuarioId(auth.getId());
  }

  @PostMapping("/reservas")
  public Reserva crearReserva(@AuthenticationPrincipal AuthUser auth,
                              @Valid @RequestBody ClienteReservaCreateRequest req) {

    if (!req.horaInicio().isBefore(req.horaFin())) {
      throw new BadRequestException("El rango horario no es válido (horaInicio debe ser anterior a horaFin)");
    }

    Instalacion inst = instalacionRepo.findById(req.instalacionId())
      .orElseThrow(() -> new NotFoundException("Instalación no encontrada: " + req.instalacionId()));

    // Comprobar solape en esa instalación/día
    List<Reserva> delDia = reservaRepo
      .findByHorario_InstalacionSnapshot_InstalacionIdAndHorario_Dia(inst.getId(), req.dia());

    boolean solapa = delDia.stream().anyMatch(r ->
      r.getHorario().getHoraInicio().isBefore(req.horaFin())
        && req.horaInicio().isBefore(r.getHorario().getHoraFin())
    );
    if (solapa) throw new ConflictException("Existe una reserva que solapa en esa instalación y franja horaria");

    InstalacionSnapshot snap = new InstalacionSnapshot(inst.getId(), inst.getNombre(), inst.getDireccion(), inst.getCiudad());
    Horario horario = new Horario(req.dia(), req.horaInicio(), req.horaFin(), snap);

    Reserva reserva = new Reserva(Instant.now(), horario, auth.getId());
    return reservaRepo.save(reserva);
  }

  @DeleteMapping("/reservas/{id}")
  public void borrarReserva(@AuthenticationPrincipal AuthUser auth, @PathVariable String id) {
    Reserva r = reservaRepo.findById(id).orElseThrow(() -> new NotFoundException("Reserva no encontrada: " + id));
    if (!r.getUsuarioId().equals(auth.getId())) throw new NotFoundException("Reserva no encontrada: " + id);
    if (reservaYaPasada(r)) throw new ConflictException("No se puede borrar una reserva pasada");
    reservaRepo.deleteById(id);
  }

  @PutMapping("/reservas/{id}")
  public Reserva modificarReserva(@AuthenticationPrincipal AuthUser auth,
                                  @PathVariable String id,
                                  @Valid @RequestBody ClienteReservaCreateRequest req) {
    Reserva r = reservaRepo.findById(id).orElseThrow(() -> new NotFoundException("Reserva no encontrada: " + id));
    if (!r.getUsuarioId().equals(auth.getId())) throw new NotFoundException("Reserva no encontrada: " + id);
    if (reservaYaPasada(r)) throw new ConflictException("No se puede modificar una reserva pasada");
    if (!req.horaInicio().isBefore(req.horaFin())) {
      throw new BadRequestException("El rango horario no es válido (horaInicio debe ser anterior a horaFin)");
    }

    Instalacion inst = instalacionRepo.findById(req.instalacionId())
      .orElseThrow(() -> new NotFoundException("Instalación no encontrada: " + req.instalacionId()));

    // Solape excluyendo la reserva actual
    List<Reserva> delDia = reservaRepo
      .findByHorario_InstalacionSnapshot_InstalacionIdAndHorario_Dia(inst.getId(), req.dia());

    boolean solapa = delDia.stream()
      .filter(x -> !x.getId().equals(r.getId()))
      .anyMatch(x -> x.getHorario().getHoraInicio().isBefore(req.horaFin())
        && req.horaInicio().isBefore(x.getHorario().getHoraFin()));

    if (solapa) throw new ConflictException("Existe una reserva que solapa en esa instalación y franja horaria");

    InstalacionSnapshot snap = new InstalacionSnapshot(inst.getId(), inst.getNombre(), inst.getDireccion(), inst.getCiudad());
    r.setHorario(new Horario(req.dia(), req.horaInicio(), req.horaFin(), snap));
    return reservaRepo.save(r);
  }

  // Instalaciones disponibles para una fecha y franja dada
  @PostMapping("/disponibilidad")
  public List<Instalacion> instalacionesDisponibles(@Valid @RequestBody DisponibilidadRequest req) {
    if (!req.horaInicio().isBefore(req.horaFin())) {
      throw new BadRequestException("El rango horario no es válido (horaInicio debe ser anterior a horaFin)");
    }

    List<Instalacion> instalaciones = instalacionRepo.findAll();
    List<Reserva> reservasDelDia = reservaRepo.findByHorario_Dia(req.dia());

    return instalaciones.stream().filter(inst -> {
      List<Reserva> deInst = reservasDelDia.stream()
        .filter(r -> inst.getId().equals(r.getHorario().getInstalacionSnapshot().getInstalacionId()))
        .toList();

      boolean solapa = deInst.stream().anyMatch(r ->
        r.getHorario().getHoraInicio().isBefore(req.horaFin())
          && req.horaInicio().isBefore(r.getHorario().getHoraFin())
      );

      return !solapa;
    }).toList();
  }

  private boolean reservaYaPasada(Reserva r) {
    LocalDate dia = r.getHorario().getDia();
    LocalTime fin = r.getHorario().getHoraFin();

    LocalDate hoy = LocalDate.now();
    LocalTime ahora = LocalTime.now();

    if (dia.isBefore(hoy)) return true;
    if (dia.isAfter(hoy)) return false;
    return !fin.isAfter(ahora); // hoy: pasada si fin <= ahora
  }
}
```

### 9) Endpoint útil: “quién soy” (para frontend)

Para que el frontend pueda saber el rol y adaptar menús/pantallas:

`web/AuthController.java`

```java
package com.dam.reservas.web;

import com.dam.reservas.security.AuthUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  @GetMapping("/me")
  public Object me(@AuthenticationPrincipal AuthUser auth) {
    return new Object() {
      public final String id = auth.getId();
      public final String email = auth.getUsername();
      public final String rol = auth.getRol().name();
    };
  }
}
```

### Resumen de lo que queda implementado en backend

* Autenticación con **usuarios de MongoDB** (email + BCrypt).
* Autorización por roles con `@PreAuthorize`:

  * ADMIN: `/api/admin/**`
  * CONSERJE: `/api/conserje/**`
  * CLIENTE: `/api/cliente/**`
* Cliente:

  * solo ve sus reservas
  * modifica/borrra solo si no está pasada
  * consulta instalaciones disponibles para un día/franja
* Conserje:

  * ve reservas del día sin datos de usuario
* Admin:

  * CRUD de usuarios (y preparado para extender a instalaciones y horarios)



### 1) DTOs ADMIN

A continuación completamos los endpoints y lógica para **el rol ADMIN** con CRUD completo de:

* **Usuarios** (con rol y contraseña, guardando BCrypt)
* **Instalaciones**
* **Horarios disponibles** (documento `horarios`)

Incluiremos **DTOs**, **controladores** y la **lógica mínima** de validación/reglas (email único, rango horario válido). Usamos el patrón `/api/admin/**` protegido con `@PreAuthorize("hasRole('ADMIN')")`.

#### 1.1 Usuarios (ADMIN)

Ya tenías `AdminUsuarioCreateRequest` y `AdminUsuarioResponse`. Añadimos DTO de actualización (permite cambiar nombre, email, rol y, opcionalmente, password).

`dto/AdminUsuarioUpdateRequest.java`

```java
package com.dam.reservas.dto;

import com.dam.reservas.model.RolUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUsuarioUpdateRequest(
  @NotBlank String nombre,
  @NotBlank @Email String email,
  @NotNull RolUsuario rol,
  @Size(min = 6, message = "password mínimo 6 caracteres") String password // opcional (puede ser null o "")
) {}
```


#### 1.2 Instalaciones (ADMIN)

`dto/AdminInstalacionRequest.java`

```java
package com.dam.reservas.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminInstalacionRequest(
  @NotBlank String nombre,
  @NotBlank String direccion,
  @NotBlank String ciudad
) {}
```

`dto/AdminInstalacionResponse.java`

```java
package com.dam.reservas.dto;

public record AdminInstalacionResponse(
  String id,
  String nombre,
  String direccion,
  String ciudad
) {}
```


#### 1.3 Horarios disponibles (ADMIN)

`dto/AdminHorarioRequest.java`

```java
package com.dam.reservas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record AdminHorarioRequest(
  @NotBlank String instalacionId,
  @NotNull LocalDate dia,
  @NotNull LocalTime horaInicio,
  @NotNull LocalTime horaFin
) {}
```

`dto/AdminHorarioResponse.java`

```java
package com.dam.reservas.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record AdminHorarioResponse(
  String id,
  String instalacionId,
  LocalDate dia,
  LocalTime horaInicio,
  LocalTime horaFin
) {}
```



### 2) Repositorio de horarios (si no lo añadiste ya)

`repo/HorarioDisponibleRepository.java`

```java
package com.dam.reservas.repo;

import com.dam.reservas.model.HorarioDisponible;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface HorarioDisponibleRepository extends MongoRepository<HorarioDisponible, String> {
  List<HorarioDisponible> findByInstalacionIdAndDia(String instalacionId, LocalDate dia);
  List<HorarioDisponible> findByDia(LocalDate dia);
}
```


### 3) Controlador ADMIN: Usuarios (CRUD completo)

`web/AdminUsuariosController.java`

```java
package com.dam.reservas.web;

import com.dam.reservas.dto.*;
import com.dam.reservas.model.Usuario;
import com.dam.reservas.repo.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsuariosController {

  private final UsuarioRepository repo;
  private final PasswordEncoder encoder;

  public AdminUsuariosController(UsuarioRepository repo, PasswordEncoder encoder) {
    this.repo = repo;
    this.encoder = encoder;
  }

  @GetMapping
  public List<AdminUsuarioResponse> listar() {
    return repo.findAll().stream()
      .map(u -> new AdminUsuarioResponse(u.getId(), u.getNombre(), u.getEmail(), u.getRol()))
      .toList();
  }

  @GetMapping("/{id}")
  public AdminUsuarioResponse obtener(@PathVariable String id) {
    Usuario u = repo.findById(id).orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + id));
    return new AdminUsuarioResponse(u.getId(), u.getNombre(), u.getEmail(), u.getRol());
  }

  @PostMapping
  public AdminUsuarioResponse crear(@Valid @RequestBody AdminUsuarioCreateRequest req) {
    if (repo.existsByEmailIgnoreCase(req.email())) {
      throw new ConflictException("Ya existe un usuario con ese email");
    }
    Usuario u = new Usuario(req.nombre(), req.email(), encoder.encode(req.password()), req.rol());
    u = repo.save(u);
    return new AdminUsuarioResponse(u.getId(), u.getNombre(), u.getEmail(), u.getRol());
  }

  @PutMapping("/{id}")
  public AdminUsuarioResponse actualizar(@PathVariable String id, @Valid @RequestBody AdminUsuarioUpdateRequest req) {
    Usuario u = repo.findById(id).orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + id));

    // email único (si cambia)
    String emailNuevo = req.email();
    if (!u.getEmail().equalsIgnoreCase(emailNuevo) && repo.existsByEmailIgnoreCase(emailNuevo)) {
      throw new ConflictException("Ya existe un usuario con ese email");
    }

    u.setNombre(req.nombre());
    u.setEmail(emailNuevo);
    u.setRol(req.rol());

    if (req.password() != null && !req.password().isBlank()) {
      u.setPasswordHash(encoder.encode(req.password()));
    }

    u = repo.save(u);
    return new AdminUsuarioResponse(u.getId(), u.getNombre(), u.getEmail(), u.getRol());
  }

  @DeleteMapping("/{id}")
  public void borrar(@PathVariable String id) {
    if (!repo.existsById(id)) throw new NotFoundException("Usuario no encontrado: " + id);
    repo.deleteById(id);
  }
}
```



### 4) Controlador ADMIN: Instalaciones (CRUD completo)

`web/AdminInstalacionesController.java`

```java
package com.dam.reservas.web;

import com.dam.reservas.dto.AdminInstalacionRequest;
import com.dam.reservas.dto.AdminInstalacionResponse;
import com.dam.reservas.model.Instalacion;
import com.dam.reservas.repo.InstalacionRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/instalaciones")
@PreAuthorize("hasRole('ADMIN')")
public class AdminInstalacionesController {

  private final InstalacionRepository repo;

  public AdminInstalacionesController(InstalacionRepository repo) {
    this.repo = repo;
  }

  @GetMapping
  public List<AdminInstalacionResponse> listar() {
    return repo.findAll().stream()
      .map(i -> new AdminInstalacionResponse(i.getId(), i.getNombre(), i.getDireccion(), i.getCiudad()))
      .toList();
  }

  @GetMapping("/{id}")
  public AdminInstalacionResponse obtener(@PathVariable String id) {
    Instalacion i = repo.findById(id).orElseThrow(() -> new NotFoundException("Instalación no encontrada: " + id));
    return new AdminInstalacionResponse(i.getId(), i.getNombre(), i.getDireccion(), i.getCiudad());
  }

  @PostMapping
  public AdminInstalacionResponse crear(@Valid @RequestBody AdminInstalacionRequest req) {
    Instalacion i = new Instalacion(req.nombre(), req.direccion(), req.ciudad());
    i = repo.save(i);
    return new AdminInstalacionResponse(i.getId(), i.getNombre(), i.getDireccion(), i.getCiudad());
  }

  @PutMapping("/{id}")
  public AdminInstalacionResponse actualizar(@PathVariable String id, @Valid @RequestBody AdminInstalacionRequest req) {
    Instalacion i = repo.findById(id).orElseThrow(() -> new NotFoundException("Instalación no encontrada: " + id));
    i.setNombre(req.nombre());
    i.setDireccion(req.direccion());
    i.setCiudad(req.ciudad());
    i = repo.save(i);
    return new AdminInstalacionResponse(i.getId(), i.getNombre(), i.getDireccion(), i.getCiudad());
  }

  @DeleteMapping("/{id}")
  public void borrar(@PathVariable String id) {
    if (!repo.existsById(id)) throw new NotFoundException("Instalación no encontrada: " + id);
    repo.deleteById(id);
  }
}
```


### 5) Controlador ADMIN: Horarios disponibles (CRUD completo)

Reglas mínimas:

* `horaInicio < horaFin` → 400
* `instalacionId` debe existir → 404
* (Opcional) evitar duplicados/solapes de horarios disponibles. Aquí lo dejamos como extensión; se puede añadir igual que con reservas.

`web/AdminHorariosController.java`

```java
package com.dam.reservas.web;

import com.dam.reservas.dto.AdminHorarioRequest;
import com.dam.reservas.dto.AdminHorarioResponse;
import com.dam.reservas.model.HorarioDisponible;
import com.dam.reservas.repo.HorarioDisponibleRepository;
import com.dam.reservas.repo.InstalacionRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/horarios")
@PreAuthorize("hasRole('ADMIN')")
public class AdminHorariosController {

  private final HorarioDisponibleRepository repo;
  private final InstalacionRepository instalacionRepo;

  public AdminHorariosController(HorarioDisponibleRepository repo, InstalacionRepository instalacionRepo) {
    this.repo = repo;
    this.instalacionRepo = instalacionRepo;
  }

  @GetMapping
  public List<AdminHorarioResponse> listar(@RequestParam(required = false) String instalacionId,
                                          @RequestParam(required = false) java.time.LocalDate dia) {
    if (instalacionId != null && !instalacionId.isBlank() && dia != null) {
      return repo.findByInstalacionIdAndDia(instalacionId, dia).stream()
        .map(h -> new AdminHorarioResponse(h.getId(), h.getInstalacionId(), h.getDia(), h.getHoraInicio(), h.getHoraFin()))
        .toList();
    }
    if (dia != null) {
      return repo.findByDia(dia).stream()
        .map(h -> new AdminHorarioResponse(h.getId(), h.getInstalacionId(), h.getDia(), h.getHoraInicio(), h.getHoraFin()))
        .toList();
    }
    return repo.findAll().stream()
      .map(h -> new AdminHorarioResponse(h.getId(), h.getInstalacionId(), h.getDia(), h.getHoraInicio(), h.getHoraFin()))
      .toList();
  }

  @GetMapping("/{id}")
  public AdminHorarioResponse obtener(@PathVariable String id) {
    HorarioDisponible h = repo.findById(id).orElseThrow(() -> new NotFoundException("Horario no encontrado: " + id));
    return new AdminHorarioResponse(h.getId(), h.getInstalacionId(), h.getDia(), h.getHoraInicio(), h.getHoraFin());
  }

  @PostMapping
  public AdminHorarioResponse crear(@Valid @RequestBody AdminHorarioRequest req) {
    validarRango(req);
    if (!instalacionRepo.existsById(req.instalacionId())) {
      throw new NotFoundException("Instalación no encontrada: " + req.instalacionId());
    }
    HorarioDisponible h = new HorarioDisponible(req.instalacionId(), req.dia(), req.horaInicio(), req.horaFin());
    h = repo.save(h);
    return new AdminHorarioResponse(h.getId(), h.getInstalacionId(), h.getDia(), h.getHoraInicio(), h.getHoraFin());
  }

  @PutMapping("/{id}")
  public AdminHorarioResponse actualizar(@PathVariable String id, @Valid @RequestBody AdminHorarioRequest req) {
    validarRango(req);
    HorarioDisponible h = repo.findById(id).orElseThrow(() -> new NotFoundException("Horario no encontrado: " + id));

    if (!instalacionRepo.existsById(req.instalacionId())) {
      throw new NotFoundException("Instalación no encontrada: " + req.instalacionId());
    }

    h.setInstalacionId(req.instalacionId());
    h.setDia(req.dia());
    h.setHoraInicio(req.horaInicio());
    h.setHoraFin(req.horaFin());

    h = repo.save(h);
    return new AdminHorarioResponse(h.getId(), h.getInstalacionId(), h.getDia(), h.getHoraInicio(), h.getHoraFin());
  }

  @DeleteMapping("/{id}")
  public void borrar(@PathVariable String id) {
    if (!repo.existsById(id)) throw new NotFoundException("Horario no encontrado: " + id);
    repo.deleteById(id);
  }

  private void validarRango(AdminHorarioRequest req) {
    if (!req.horaInicio().isBefore(req.horaFin())) {
      throw new BadRequestException("El rango horario no es válido (horaInicio debe ser anterior a horaFin)");
    }
  }
}
```



### 6) Recomendación: ocultar passwordHash en respuestas públicas

Con ADMIN ya no devolvemos entidad `Usuario` directamente, sino `AdminUsuarioResponse` sin `passwordHash`. Aun así, conviene **no usar jamás `Usuario` como response** en endpoints públicos o de cliente.



### 7) Endpoints ADMIN finales (resumen)

Todos requieren sesión iniciada y rol ADMIN:

* Usuarios:

  * `GET /api/admin/usuarios`
  * `GET /api/admin/usuarios/{id}`
  * `POST /api/admin/usuarios`
  * `PUT /api/admin/usuarios/{id}`
  * `DELETE /api/admin/usuarios/{id}`

* Instalaciones:

  * `GET /api/admin/instalaciones`
  * `GET /api/admin/instalaciones/{id}`
  * `POST /api/admin/instalaciones`
  * `PUT /api/admin/instalaciones/{id}`
  * `DELETE /api/admin/instalaciones/{id}`

* Horarios disponibles:

  * `GET /api/admin/horarios?instalacionId=...&dia=YYYY-MM-DD`
  * `GET /api/admin/horarios/{id}`
  * `POST /api/admin/horarios`
  * `PUT /api/admin/horarios/{id}`
  * `DELETE /api/admin/horarios/{id}`


### 8) Nota importante para el siguiente paso (frontend)

Como la autenticación es `formLogin()`, cuando el usuario hace login:

* el navegador guarda una **cookie de sesión**
* y jQuery podrá llamar a `/api/**` sin añadir tokens manualmente (mientras sea mismo origen)

### Frontend con login + UI por rol (ADMIN / CONSERJE / CLIENTE)

En este paso dejamos el **frontend funcional con autenticación por sesión (Spring Security formLogin)** y una interfaz que:

* Muestra **pantalla de login**
* Tras login, consulta `/api/auth/me` y **renderiza menús/zonas según rol**
* Implementa:

  * **ADMIN**: CRUD de usuarios, instalaciones y horarios disponibles
  * **CONSERJE**: ver reservas de hoy (sin usuario)
  * **CLIENTE**: ver sus reservas, crear/modificar/borrar si no pasadas, consultar disponibilidad

Incluyo los **cambios mínimos de backend** necesarios para usar una página de login propia (solo configuración), y luego los **archivos estáticos**.

#### 1) Backend: ajustar Spring Security para login.html (mínimo)

Modifica tu `SecurityConfig` para:

* Permitir `/login.html`
* Usar login page propia
* Mantener sesión/cookies

`security/SecurityConfig.java` (cambia solo el bloque `formLogin` y matchers)

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
  http
    .csrf(csrf -> csrf.disable())
    .authorizeHttpRequests(auth -> auth
      .requestMatchers(
        "/", "/index.html", "/login.html",
        "/css/**", "/js/**",
        "/favicon.ico"
      ).permitAll()
      .requestMatchers("/api/**").authenticated()
      .anyRequest().permitAll()
    )
    .formLogin(form -> form
      .loginPage("/login.html")
      .loginProcessingUrl("/login")
      .defaultSuccessUrl("/", true)
      .permitAll()
    )
    .logout(logout -> logout
      .logoutUrl("/logout")
      .logoutSuccessUrl("/login.html")
      .permitAll()
    );

  return http.build();
}
```

Importante:

* Spring Security espera por defecto parámetros `username` y `password` en el POST `/login`.

---

#### 2) Frontend: archivos

Ubicación: `src/main/resources/static/`

**2.1 `login.html`**

Página simple con Bootstrap. Envía POST a `/login` con `username` (email) y `password`.

```html
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Login - Reservas</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>

<body class="bg-light">
  <main class="container py-5" style="max-width: 520px;">
    <div class="card shadow-sm">
      <div class="card-header fw-semibold">Acceso al sistema</div>
      <div class="card-body">

        <div id="loginError" class="alert alert-danger d-none" role="alert"></div>

        <form method="post" action="/login" id="loginForm">
          <div class="mb-3">
            <label class="form-label">Email</label>
            <input class="form-control" name="username" type="email" required autocomplete="username">
          </div>
          <div class="mb-3">
            <label class="form-label">Contraseña</label>
            <input class="form-control" name="password" type="password" required autocomplete="current-password">
          </div>
          <button class="btn btn-primary w-100" type="submit">Entrar</button>
        </form>

        <hr class="my-4">

        <p class="text-muted mb-0">
          El acceso está restringido a usuarios registrados (ADMIN, CONSERJE, CLIENTE).
        </p>
      </div>
    </div>
  </main>

  <script>
    // Si Spring Security redirige a /login.html?error, mostramos mensaje.
    const params = new URLSearchParams(window.location.search);
    if (params.has("error")) {
      const el = document.getElementById("loginError");
      el.textContent = "Credenciales no válidas.";
      el.classList.remove("d-none");
    }
    if (params.has("logout")) {
      const el = document.getElementById("loginError");
      el.textContent = "Sesión cerrada.";
      el.classList.remove("d-none");
      el.classList.remove("alert-danger");
      el.classList.add("alert-success");
    }
  </script>
</body>
</html>
```
**2.2 `index.html` (dashboard por rol)**

Este `index.html` carga jQuery, consulta `/api/auth/me` y muestra secciones según rol.

```html
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Reservas de Pistas</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>

<body class="bg-light">

<nav class="navbar navbar-expand-lg navbar-dark bg-dark">
  <div class="container">
    <a class="navbar-brand" href="/">Reservas</a>

    <div class="ms-auto d-flex gap-2 align-items-center">
      <span id="who" class="navbar-text text-white-50 small"></span>
      <form method="post" action="/logout" class="mb-0">
        <button class="btn btn-sm btn-outline-light" type="submit">Salir</button>
      </form>
    </div>
  </div>
</nav>

<main class="container py-4">

  <div id="alerta" class="alert d-none" role="alert"></div>

  <!-- ADMIN -->
  <section id="secAdmin" class="d-none">
    <div class="row g-3">

      <!-- Usuarios -->
      <div class="col-12">
        <div class="card shadow-sm">
          <div class="card-header fw-semibold">ADMIN · Usuarios</div>
          <div class="card-body">
            <form id="formAdminUsuario" class="row g-2 align-items-end">
              <div class="col-md-3">
                <label class="form-label">Nombre</label>
                <input id="auNombre" class="form-control" required>
              </div>
              <div class="col-md-3">
                <label class="form-label">Email</label>
                <input id="auEmail" type="email" class="form-control" required>
              </div>
              <div class="col-md-2">
                <label class="form-label">Rol</label>
                <select id="auRol" class="form-select" required>
                  <option value="ADMIN">ADMIN</option>
                  <option value="CONSERJE">CONSERJE</option>
                  <option value="CLIENTE" selected>CLIENTE</option>
                </select>
              </div>
              <div class="col-md-2">
                <label class="form-label">Password</label>
                <input id="auPass" type="password" class="form-control" required>
              </div>
              <div class="col-md-2 d-grid">
                <button class="btn btn-primary" type="submit">Crear</button>
              </div>
            </form>

            <hr>

            <div class="table-responsive">
              <table class="table table-hover align-middle">
                <thead><tr><th>Nombre</th><th>Email</th><th>Rol</th><th class="text-end">Acciones</th></tr></thead>
                <tbody id="tabAdminUsuarios"></tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <!-- Instalaciones -->
      <div class="col-12">
        <div class="card shadow-sm">
          <div class="card-header fw-semibold">ADMIN · Instalaciones</div>
          <div class="card-body">
            <form id="formAdminInst" class="row g-2 align-items-end">
              <div class="col-md-4">
                <label class="form-label">Nombre</label>
                <input id="aiNombre" class="form-control" required>
              </div>
              <div class="col-md-4">
                <label class="form-label">Dirección</label>
                <input id="aiDir" class="form-control" required>
              </div>
              <div class="col-md-2">
                <label class="form-label">Ciudad</label>
                <input id="aiCiudad" class="form-control" required>
              </div>
              <div class="col-md-2 d-grid">
                <button class="btn btn-primary" type="submit">Crear</button>
              </div>
            </form>

            <hr>

            <div class="table-responsive">
              <table class="table table-hover align-middle">
                <thead><tr><th>Nombre</th><th>Dirección</th><th>Ciudad</th><th class="text-end">Acciones</th></tr></thead>
                <tbody id="tabAdminInst"></tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <!-- Horarios -->
      <div class="col-12">
        <div class="card shadow-sm">
          <div class="card-header fw-semibold">ADMIN · Horarios disponibles</div>
          <div class="card-body">
            <form id="formAdminHorario" class="row g-2 align-items-end">
              <div class="col-md-4">
                <label class="form-label">Instalación</label>
                <select id="ahInst" class="form-select" required></select>
              </div>
              <div class="col-md-3">
                <label class="form-label">Día</label>
                <input id="ahDia" type="date" class="form-control" required>
              </div>
              <div class="col-md-2">
                <label class="form-label">Inicio</label>
                <input id="ahIni" type="time" class="form-control" required>
              </div>
              <div class="col-md-2">
                <label class="form-label">Fin</label>
                <input id="ahFin" type="time" class="form-control" required>
              </div>
              <div class="col-md-1 d-grid">
                <button class="btn btn-primary" type="submit">Crear</button>
              </div>
            </form>

            <hr>

            <div class="table-responsive">
              <table class="table table-hover align-middle">
                <thead><tr><th>Instalación</th><th>Día</th><th>Inicio</th><th>Fin</th><th class="text-end">Acciones</th></tr></thead>
                <tbody id="tabAdminHorarios"></tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

    </div>
  </section>

  <!-- CONSERJE -->
  <section id="secConserje" class="d-none">
    <div class="card shadow-sm">
      <div class="card-header fw-semibold">CONSERJE · Reservas de hoy</div>
      <div class="card-body">
        <button class="btn btn-outline-secondary mb-3" id="btnRecargarHoy">Recargar</button>
        <div class="table-responsive">
          <table class="table table-hover align-middle">
            <thead><tr><th>Instalación</th><th>Ciudad</th><th>Inicio</th><th>Fin</th></tr></thead>
            <tbody id="tabConserjeHoy"></tbody>
          </table>
        </div>
      </div>
    </div>
  </section>

  <!-- CLIENTE -->
  <section id="secCliente" class="d-none">
    <div class="row g-3">

      <div class="col-12">
        <div class="card shadow-sm">
          <div class="card-header fw-semibold">CLIENTE · Disponibilidad</div>
          <div class="card-body">
            <form id="formDisp" class="row g-2 align-items-end">
              <div class="col-md-3">
                <label class="form-label">Día</label>
                <input id="cdDia" type="date" class="form-control" required>
              </div>
              <div class="col-md-2">
                <label class="form-label">Inicio</label>
                <input id="cdIni" type="time" class="form-control" required>
              </div>
              <div class="col-md-2">
                <label class="form-label">Fin</label>
                <input id="cdFin" type="time" class="form-control" required>
              </div>
              <div class="col-md-3 d-grid">
                <button class="btn btn-outline-primary" type="submit">Ver pistas libres</button>
              </div>
            </form>

            <hr>

            <div class="table-responsive">
              <table class="table table-hover align-middle">
                <thead><tr><th>Instalación</th><th>Dirección</th><th>Ciudad</th></tr></thead>
                <tbody id="tabDisp"></tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <div class="col-12">
        <div class="card shadow-sm">
          <div class="card-header fw-semibold">CLIENTE · Mis reservas</div>
          <div class="card-body">
            <form id="formClienteReserva" class="row g-2 align-items-end">
              <div class="col-md-4">
                <label class="form-label">Instalación</label>
                <select id="crInst" class="form-select" required></select>
              </div>
              <div class="col-md-3">
                <label class="form-label">Día</label>
                <input id="crDia" type="date" class="form-control" required>
              </div>
              <div class="col-md-2">
                <label class="form-label">Inicio</label>
                <input id="crIni" type="time" class="form-control" required>
              </div>
              <div class="col-md-2">
                <label class="form-label">Fin</label>
                <input id="crFin" type="time" class="form-control" required>
              </div>
              <div class="col-md-1 d-grid">
                <button class="btn btn-success" type="submit">Crear</button>
              </div>
            </form>

            <hr>

            <div class="table-responsive">
              <table class="table table-hover align-middle">
                <thead><tr><th>Día</th><th>Horario</th><th>Instalación</th><th class="text-end">Acciones</th></tr></thead>
                <tbody id="tabMisReservas"></tbody>
              </table>
            </div>
            <p class="text-muted mb-0">Solo podrás modificar o borrar reservas futuras.</p>
          </div>
        </div>
      </div>

    </div>
  </section>

</main>

<script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
<script src="js/app-secure.js"></script>
</body>
</html>
```

**2.3 `js/app-secure.js` (jQuery + endpoints por rol)**

Este fichero:

* comprueba sesión con `/api/auth/me`
* si 401, redirige a `/login.html`
* carga y opera según rol

```javascript
/* global $ */

const API = {
  me: "/api/auth/me",

  admin: {
    usuarios: "/api/admin/usuarios",
    instalaciones: "/api/admin/instalaciones",
    horarios: "/api/admin/horarios"
  },

  conserje: {
    hoy: "/api/conserje/reservas-hoy"
  },

  cliente: {
    reservas: "/api/cliente/reservas",
    disponibilidad: "/api/cliente/disponibilidad"
  }
};

let session = null;
let cacheInstalaciones = [];

$(document).ready(async function () {
  try {
    session = await getMe();
  } catch (e) {
    // no autenticado
    window.location.href = "/login.html";
    return;
  }

  $("#who").text(`${session.email} · ${session.rol}`);

  if (session.rol === "ADMIN") initAdmin();
  if (session.rol === "CONSERJE") initConserje();
  if (session.rol === "CLIENTE") initCliente();
});

/* ---------------------------
   Helpers
---------------------------- */

function showAlert(type, msg) {
  $("#alerta")
    .removeClass("d-none alert-success alert-danger alert-warning alert-info")
    .addClass("alert-" + type)
    .text(msg);
  setTimeout(() => $("#alerta").addClass("d-none"), 3500);
}

function parseApiError(xhr, fallback) {
  const r = xhr.responseJSON;
  if (!r) return fallback;
  if (Array.isArray(r.details) && r.details.length) return `${r.message}: ${r.details.join(" | ")}`;
  return r.message || fallback;
}

function escapeHtml(s) {
  return String(s)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;");
}

function ajaxJson(method, url, body) {
  return $.ajax({
    method,
    url,
    contentType: "application/json",
    data: body ? JSON.stringify(body) : undefined
  });
}

async function getMe() {
  return $.getJSON(API.me);
}

function fillSelectInst($sel, instalaciones, placeholder) {
  const opts = (instalaciones || []).map(i =>
    `<option value="${i.id}">${escapeHtml(i.nombre)} (${escapeHtml(i.ciudad)})</option>`
  ).join("");
  $sel.html(`<option value="" disabled selected>${placeholder}</option>${opts}`);
}

/* ---------------------------
   ADMIN
---------------------------- */

function initAdmin() {
  $("#secAdmin").removeClass("d-none");

  $("#formAdminUsuario").on("submit", crearAdminUsuario);
  $("#formAdminInst").on("submit", crearAdminInstalacion);
  $("#formAdminHorario").on("submit", crearAdminHorario);

  recargarAdmin();
}

function recargarAdmin() {
  $.when(
    cargarAdminUsuarios(),
    cargarAdminInstalaciones(),
    cargarAdminHorarios()
  ).fail(() => showAlert("danger", "Error cargando datos de administración"));
}

function cargarAdminUsuarios() {
  return $.getJSON(API.admin.usuarios)
    .done(renderAdminUsuarios)
    .fail(xhr => showAlert("danger", parseApiError(xhr, "Error cargando usuarios")));
}

function renderAdminUsuarios(items) {
  const rows = (items || []).map(u => `
    <tr>
      <td>${escapeHtml(u.nombre)}</td>
      <td>${escapeHtml(u.email)}</td>
      <td>${escapeHtml(u.rol)}</td>
      <td class="text-end">
        <button class="btn btn-sm btn-outline-danger" data-del-user="${u.id}">Eliminar</button>
      </td>
    </tr>
  `).join("");

  $("#tabAdminUsuarios").html(rows || `<tr><td colspan="4" class="text-center text-muted">Sin datos</td></tr>`);

  $("#tabAdminUsuarios button[data-del-user]").off("click").on("click", function () {
    const id = $(this).data("del-user");
    if (!confirm("¿Eliminar usuario?")) return;
    $.ajax({ method: "DELETE", url: `${API.admin.usuarios}/${id}` })
      .done(() => { showAlert("success", "Usuario eliminado"); cargarAdminUsuarios(); })
      .fail(xhr => showAlert("danger", parseApiError(xhr, "Error eliminando usuario")));
  });
}

function crearAdminUsuario(e) {
  e.preventDefault();
  const payload = {
    nombre: $("#auNombre").val().trim(),
    email: $("#auEmail").val().trim(),
    rol: $("#auRol").val(),
    password: $("#auPass").val()
  };

  ajaxJson("POST", API.admin.usuarios, payload)
    .done(() => { showAlert("success", "Usuario creado"); $("#formAdminUsuario")[0].reset(); cargarAdminUsuarios(); })
    .fail(xhr => showAlert("danger", parseApiError(xhr, "Error creando usuario")));
}

function cargarAdminInstalaciones() {
  return $.getJSON(API.admin.instalaciones)
    .done(data => {
      cacheInstalaciones = data || [];
      renderAdminInstalaciones(cacheInstalaciones);
      fillSelectInst($("#ahInst"), cacheInstalaciones, "Seleccione instalación...");
      fillSelectInst($("#crInst"), cacheInstalaciones, "Seleccione instalación...");
    })
    .fail(xhr => showAlert("danger", parseApiError(xhr, "Error cargando instalaciones")));
}

function renderAdminInstalaciones(items) {
  const rows = (items || []).map(i => `
    <tr>
      <td>${escapeHtml(i.nombre)}</td>
      <td>${escapeHtml(i.direccion)}</td>
      <td>${escapeHtml(i.ciudad)}</td>
      <td class="text-end">
        <button class="btn btn-sm btn-outline-danger" data-del-inst="${i.id}">Eliminar</button>
      </td>
    </tr>
  `).join("");

  $("#tabAdminInst").html(rows || `<tr><td colspan="4" class="text-center text-muted">Sin datos</td></tr>`);

  $("#tabAdminInst button[data-del-inst]").off("click").on("click", function () {
    const id = $(this).data("del-inst");
    if (!confirm("¿Eliminar instalación?")) return;
    $.ajax({ method: "DELETE", url: `${API.admin.instalaciones}/${id}` })
      .done(() => { showAlert("success", "Instalación eliminada"); cargarAdminInstalaciones(); })
      .fail(xhr => showAlert("danger", parseApiError(xhr, "Error eliminando instalación")));
  });
}

function crearAdminInstalacion(e) {
  e.preventDefault();
  const payload = {
    nombre: $("#aiNombre").val().trim(),
    direccion: $("#aiDir").val().trim(),
    ciudad: $("#aiCiudad").val().trim()
  };

  ajaxJson("POST", API.admin.instalaciones, payload)
    .done(() => { showAlert("success", "Instalación creada"); $("#formAdminInst")[0].reset(); cargarAdminInstalaciones(); })
    .fail(xhr => showAlert("danger", parseApiError(xhr, "Error creando instalación")));
}

function cargarAdminHorarios() {
  return $.getJSON(API.admin.horarios)
    .done(renderAdminHorarios)
    .fail(xhr => showAlert("danger", parseApiError(xhr, "Error cargando horarios")));
}

function renderAdminHorarios(items) {
  const instName = new Map((cacheInstalaciones || []).map(i => [i.id, `${i.nombre} (${i.ciudad})`]));

  const rows = (items || []).map(h => `
    <tr>
      <td>${escapeHtml(instName.get(h.instalacionId) || h.instalacionId)}</td>
      <td>${escapeHtml(h.dia)}</td>
      <td>${escapeHtml(h.horaInicio)}</td>
      <td>${escapeHtml(h.horaFin)}</td>
      <td class="text-end">
        <button class="btn btn-sm btn-outline-danger" data-del-h="${h.id}">Eliminar</button>
      </td>
    </tr>
  `).join("");

  $("#tabAdminHorarios").html(rows || `<tr><td colspan="5" class="text-center text-muted">Sin datos</td></tr>`);

  $("#tabAdminHorarios button[data-del-h]").off("click").on("click", function () {
    const id = $(this).data("del-h");
    if (!confirm("¿Eliminar horario?")) return;
    $.ajax({ method: "DELETE", url: `${API.admin.horarios}/${id}` })
      .done(() => { showAlert("success", "Horario eliminado"); cargarAdminHorarios(); })
      .fail(xhr => showAlert("danger", parseApiError(xhr, "Error eliminando horario")));
  });
}

function crearAdminHorario(e) {
  e.preventDefault();
  const payload = {
    instalacionId: $("#ahInst").val(),
    dia: $("#ahDia").val(),
    horaInicio: $("#ahIni").val(),
    horaFin: $("#ahFin").val()
  };

  ajaxJson("POST", API.admin.horarios, payload)
    .done(() => { showAlert("success", "Horario creado"); $("#formAdminHorario")[0].reset(); cargarAdminHorarios(); })
    .fail(xhr => showAlert("danger", parseApiError(xhr, "Error creando horario")));
}

/* ---------------------------
   CONSERJE
---------------------------- */

function initConserje() {
  $("#secConserje").removeClass("d-none");
  $("#btnRecargarHoy").on("click", cargarConserjeHoy);
  cargarConserjeHoy();
}

function cargarConserjeHoy() {
  $.getJSON(API.conserje.hoy)
    .done(items => {
      const rows = (items || []).map(x => `
        <tr>
          <td>${escapeHtml(x.instalacionNombre)}</td>
          <td>${escapeHtml(x.instalacionCiudad)}</td>
          <td>${escapeHtml(x.horaInicio)}</td>
          <td>${escapeHtml(x.horaFin)}</td>
        </tr>
      `).join("");
      $("#tabConserjeHoy").html(rows || `<tr><td colspan="4" class="text-center text-muted">Sin reservas</td></tr>`);
    })
    .fail(xhr => showAlert("danger", parseApiError(xhr, "Error cargando reservas de hoy")));
}

/* ---------------------------
   CLIENTE
---------------------------- */

function initCliente() {
  $("#secCliente").removeClass("d-none");

  // disponibilidad
  $("#formDisp").on("submit", consultarDisponibilidad);

  // reservas
  $("#formClienteReserva").on("submit", crearReservaCliente);

  // Necesitamos instalaciones para el select
  // Si no es ADMIN, no tenemos cacheInstalaciones: las pedimos por admin endpoint? No.
  // Para CLIENTE, lo correcto es exponer un endpoint público autenticado de instalaciones.
  // Como aún no lo has creado, reutilizamos temporalmente /api/admin/instalaciones NO (está protegido).
  // Solución: crear /api/instalaciones (autenticado) o /api/cliente/instalaciones.
  // Aquí asumimos que ya mantienes /api/instalaciones autenticado (tu API antigua). Si no, dímelo y te doy el endpoint.
  $.getJSON("/api/instalaciones")
    .done(data => {
      cacheInstalaciones = data || [];
      fillSelectInst($("#crInst"), cacheInstalaciones, "Seleccione instalación...");
    })
    .fail(xhr => showAlert("danger", parseApiError(xhr, "Error cargando instalaciones")));

  cargarMisReservas();
}

function consultarDisponibilidad(e) {
  e.preventDefault();
  const payload = {
    dia: $("#cdDia").val(),
    horaInicio: $("#cdIni").val(),
    horaFin: $("#cdFin").val()
  };

  ajaxJson("POST", API.cliente.disponibilidad, payload)
    .done(items => {
      const rows = (items || []).map(i => `
        <tr>
          <td>${escapeHtml(i.nombre)}</td>
          <td>${escapeHtml(i.direccion)}</td>
          <td>${escapeHtml(i.ciudad)}</td>
        </tr>
      `).join("");
      $("#tabDisp").html(rows || `<tr><td colspan="3" class="text-center text-muted">Sin disponibilidad</td></tr>`);
    })
    .fail(xhr => showAlert("danger", parseApiError(xhr, "Error consultando disponibilidad")));
}

function cargarMisReservas() {
  $.getJSON(API.cliente.reservas)
    .done(renderMisReservas)
    .fail(xhr => showAlert("danger", parseApiError(xhr, "Error cargando mis reservas")));
}

function crearReservaCliente(e) {
  e.preventDefault();
  const payload = {
    instalacionId: $("#crInst").val(),
    dia: $("#crDia").val(),
    horaInicio: $("#crIni").val(),
    horaFin: $("#crFin").val()
  };

  ajaxJson("POST", API.cliente.reservas, payload)
    .done(() => { showAlert("success", "Reserva creada"); $("#formClienteReserva")[0].reset(); cargarMisReservas(); })
    .fail(xhr => showAlert("danger", parseApiError(xhr, "Error creando reserva")));
}

function renderMisReservas(items) {
  const instName = new Map((cacheInstalaciones || []).map(i => [i.id, `${i.nombre} (${i.ciudad})`]));

  const rows = (items || []).map(r => {
    const h = r.horario || {};
    const snap = h.instalacionSnapshot || {};
    const inst = instName.get(snap.instalacionId) || snap.nombre || snap.instalacionId || "";

    return `
      <tr>
        <td>${escapeHtml(h.dia || "")}</td>
        <td>${escapeHtml((h.horaInicio || "") + " - " + (h.horaFin || ""))}</td>
        <td>${escapeHtml(inst)}</td>
        <td class="text-end">
          <button class="btn btn-sm btn-outline-danger" data-del-res="${r.id}">Borrar</button>
        </td>
      </tr>
    `;
  }).join("");

  $("#tabMisReservas").html(rows || `<tr><td colspan="4" class="text-center text-muted">Sin reservas</td></tr>`);

  $("#tabMisReservas button[data-del-res]").off("click").on("click", function () {
    const id = $(this).data("del-res");
    if (!confirm("¿Borrar reserva? (solo se permitirá si es futura)")) return;
    $.ajax({ method: "DELETE", url: `${API.cliente.reservas}/${id}` })
      .done(() => { showAlert("success", "Reserva borrada"); cargarMisReservas(); })
      .fail(xhr => showAlert("danger", parseApiError(xhr, "Error borrando reserva")));
  });
}
```


#### 3) Nota importante (para que CLIENTE cargue instalaciones sin ser ADMIN)

En el JS del cliente he supuesto que existe `GET /api/instalaciones` accesible a usuarios autenticados (tu API “antigua”).
Si actualmente has movido todo a `/api/admin/**`, crea un endpoint simple para instalaciones autenticadas, por ejemplo:

* `GET /api/instalaciones` (autenticado, cualquier rol)
* o `GET /api/cliente/instalaciones` (solo CLIENTE)

De esta manera el controlador quedaría así:

```java
package com.dam.reservas.web;

import com.dam.reservas.model.Instalacion;
import com.dam.reservas.repo.InstalacionRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instalaciones")
@PreAuthorize("isAuthenticated()") // cualquier usuario autenticado (ADMIN/CONSERJE/CLIENTE)
public class InstalacionesController {

  private final InstalacionRepository repo;

  public InstalacionesController(InstalacionRepository repo) {
    this.repo = repo;
  }

  /**
   * Listado de instalaciones accesible para cualquier usuario autenticado.
   * Filtros opcionales:
   * - ?ciudad=Jaen
   * - ?q=pista
   */
  @GetMapping
  public List<Instalacion> listar(@RequestParam(required = false) String ciudad,
                                  @RequestParam(required = false) String q) {

    if (ciudad != null && !ciudad.isBlank()) {
      return repo.findByCiudadIgnoreCase(ciudad.trim());
    }
    if (q != null && !q.isBlank()) {
      return repo.findByNombreContainingIgnoreCase(q.trim());
    }
    return repo.findAll();
  }

  @GetMapping("/{id}")
  public Instalacion obtener(@PathVariable String id) {
    return repo.findById(id).orElseThrow(() -> new NotFoundException("Instalación no encontrada: " + id));
  }
}

```

**4) Cómo probar este paso*

1. Arrancar backend con `./mvnw spring-boot:run`
2. Crear un usuario ADMIN desde Mongo Express.
3. Ir a `http://localhost:8080/login.html`
4. Login con email/password
5. Ver:
   * ADMIN: CRUD en el dashboard
   * CONSERJE: listado reservas de hoy
   * CLIENTE: disponibilidad + reservas

A continuación dejo el **siguiente paso lógico**: ampliar la UI de **CLIENTE** para **editar reservas (PUT)** con un **modal Bootstrap** y mantener el borrado (DELETE). Incluye:

* Cambios en `index.html` (sección CLIENTE): añadir botón **Editar** y modal.
* Cambios en `js/app-secure.js`: abrir modal, precargar datos, enviar PUT a `/api/cliente/reservas/{id}`, refrescar tabla y gestionar errores (409/400/404).

> Requisito backend: ya tienes `PUT /api/cliente/reservas/{id}` implementado en `ClienteReservasController` con la regla “no pasada”. Si la reserva es pasada o solapa, devolverá 409.

#### Modificaciones en el index.html y app.js

**1) `index.html`: añadir botón Editar y el modal Bootstrap**

**1.1 Modificar la tabla “CLIENTE · Mis reservas”**

Sustituye el `<thead>` y el `<tbody>` de `tabMisReservas` para que tenga dos acciones: **Editar** y **Borrar**.

En tu sección CLIENTE (tabla de mis reservas), usa esto:

```html
<table class="table table-hover align-middle">
  <thead>
    <tr>
      <th>Día</th>
      <th>Horario</th>
      <th>Instalación</th>
      <th class="text-end">Acciones</th>
    </tr>
  </thead>
  <tbody id="tabMisReservas"></tbody>
</table>
```

(La cabecera es igual, pero el render JS pondrá 2 botones.)

**1.2 Añadir el modal de edición (al final del `<main>` o justo antes de `</main>`)**

```html
<!-- Modal: Editar reserva -->
<div class="modal fade" id="modalEditarReserva" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog">
    <div class="modal-content">

      <div class="modal-header">
        <h5 class="modal-title">Editar reserva</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button>
      </div>

      <div class="modal-body">
        <div class="alert alert-danger d-none" id="editError" role="alert"></div>

        <form id="formEditarReserva" class="row g-2">
          <input type="hidden" id="editReservaId">

          <div class="col-12">
            <label class="form-label">Instalación</label>
            <select id="editInst" class="form-select" required></select>
          </div>

          <div class="col-md-6">
            <label class="form-label">Día</label>
            <input id="editDia" type="date" class="form-control" required>
          </div>

          <div class="col-md-3">
            <label class="form-label">Inicio</label>
            <input id="editIni" type="time" class="form-control" required>
          </div>

          <div class="col-md-3">
            <label class="form-label">Fin</label>
            <input id="editFin" type="time" class="form-control" required>
          </div>
        </form>
      </div>

      <div class="modal-footer">
        <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancelar</button>
        <button type="button" class="btn btn-primary" id="btnGuardarEdicion">Guardar cambios</button>
      </div>

    </div>
  </div>
</div>
```

**1.3 Asegurar que Bootstrap JS está incluido**

En tu `index.html` actual con jQuery, añade también Bootstrap bundle (si no lo tienes en esta versión). Debe ir antes de `app-secure.js`:

```html
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
<script src="js/app-secure.js"></script>
```

> Importante: `bootstrap.bundle.min.js` incluye `Modal`.


**2) `js/app-secure.js`: añadir edición (PUT) con modal**

A continuación te dejo **solo las partes a añadir/modificar** dentro del fichero.

**2.1 Estado para reservas y modal**

Añade arriba, junto a `session` y `cacheInstalaciones`:

```javascript
let cacheMisReservas = [];
let modalEditar = null;
```

**2.2 Inicializar modal y eventos en `initCliente()`**

Dentro de `initCliente()` añade:

```javascript
function initCliente() {
  $("#secCliente").removeClass("d-none");

  // Bootstrap Modal instance
  modalEditar = new bootstrap.Modal(document.getElementById("modalEditarReserva"));

  // disponibilidad
  $("#formDisp").on("submit", consultarDisponibilidad);

  // reservas
  $("#formClienteReserva").on("submit", crearReservaCliente);

  // Edición
  $("#btnGuardarEdicion").on("click", guardarEdicionReserva);

  // Cargar instalaciones (endpoint accesible a autenticados)
  $.getJSON("/api/instalaciones")
    .done(data => {
      cacheInstalaciones = data || [];
      fillSelectInst($("#crInst"), cacheInstalaciones, "Seleccione instalación...");
      fillSelectInst($("#editInst"), cacheInstalaciones, "Seleccione instalación...");
    })
    .fail(xhr => showAlert("danger", parseApiError(xhr, "Error cargando instalaciones")));

  cargarMisReservas();
}
```

**2.3 Modificar `cargarMisReservas()` para guardar cache**

```javascript
function cargarMisReservas() {
  $.getJSON(API.cliente.reservas)
    .done(items => {
      cacheMisReservas = items || [];
      renderMisReservas(cacheMisReservas);
    })
    .fail(xhr => showAlert("danger", parseApiError(xhr, "Error cargando mis reservas")));
}
```

**2.4 Modificar `renderMisReservas()` para incluir “Editar”**

Sustituimos `renderMisReservas` por esta versión:

```javascript
function renderMisReservas(items) {
  const instName = new Map((cacheInstalaciones || []).map(i => [i.id, `${i.nombre} (${i.ciudad})`]));

  const rows = (items || []).map(r => {
    const h = r.horario || {};
    const snap = h.instalacionSnapshot || {};
    const instId = snap.instalacionId;
    const inst = instName.get(instId) || snap.nombre || instId || "";

    return `
      <tr>
        <td>${escapeHtml(h.dia || "")}</td>
        <td>${escapeHtml((h.horaInicio || "") + " - " + (h.horaFin || ""))}</td>
        <td>${escapeHtml(inst)}</td>
        <td class="text-end">
          <button class="btn btn-sm btn-outline-primary me-2" data-edit-res="${r.id}">Editar</button>
          <button class="btn btn-sm btn-outline-danger" data-del-res="${r.id}">Borrar</button>
        </td>
      </tr>
    `;
  }).join("");

  $("#tabMisReservas").html(rows || `<tr><td colspan="4" class="text-center text-muted">Sin reservas</td></tr>`);

  // Editar
  $("#tabMisReservas button[data-edit-res]").off("click").on("click", function () {
    const id = $(this).data("edit-res");
    abrirModalEdicion(id);
  });

  // Borrar
  $("#tabMisReservas button[data-del-res]").off("click").on("click", function () {
    const id = $(this).data("del-res");
    if (!confirm("¿Borrar reserva? (solo se permitirá si es futura)")) return;
    $.ajax({ method: "DELETE", url: `${API.cliente.reservas}/${id}` })
      .done(() => { showAlert("success", "Reserva borrada"); cargarMisReservas(); })
      .fail(xhr => showAlert("danger", parseApiError(xhr, "Error borrando reserva")));
  });
}
```

**2.5 Función: abrir modal con datos precargados**

Añadimos estas funciones nuevas:

```javascript
function abrirModalEdicion(reservaId) {
  const r = (cacheMisReservas || []).find(x => x.id === reservaId);
  if (!r) {
    showAlert("danger", "No se encontró la reserva en memoria. Recargando...");
    return cargarMisReservas();
  }

  // reset de error
  $("#editError").addClass("d-none").text("");

  const h = r.horario || {};
  const snap = h.instalacionSnapshot || {};

  $("#editReservaId").val(r.id);
  $("#editDia").val(h.dia || "");
  $("#editIni").val(h.horaInicio || "");
  $("#editFin").val(h.horaFin || "");

  // Asegurar que el select está relleno
  if (!cacheInstalaciones || cacheInstalaciones.length === 0) {
    // si por lo que sea aún no están, intenta cargarlas
    $.getJSON("/api/instalaciones").done(data => {
      cacheInstalaciones = data || [];
      fillSelectInst($("#editInst"), cacheInstalaciones, "Seleccione instalación...");
      $("#editInst").val(snap.instalacionId || "");
      modalEditar.show();
    }).fail(xhr => showAlert("danger", parseApiError(xhr, "Error cargando instalaciones")));
    return;
  }

  // seleccionar instalación
  $("#editInst").val(snap.instalacionId || "");

  modalEditar.show();
}
```

**2.6 Función: guardar cambios (PUT) y cerrar modal**

```javascript
function guardarEdicionReserva() {
  const id = $("#editReservaId").val();
  const payload = {
    instalacionId: $("#editInst").val(),
    dia: $("#editDia").val(),
    horaInicio: $("#editIni").val(),
    horaFin: $("#editFin").val()
  };

  // Validación mínima en cliente (la API valida igualmente)
  if (!payload.instalacionId || !payload.dia || !payload.horaInicio || !payload.horaFin) {
    return mostrarErrorEdicion("Todos los campos son obligatorios.");
  }

  ajaxJson("PUT", `${API.cliente.reservas}/${id}`, payload)
    .done(() => {
      modalEditar.hide();
      showAlert("success", "Reserva modificada");
      cargarMisReservas();
    })
    .fail(xhr => {
      const msg = parseApiError(xhr, "Error modificando reserva");
      // Si es “reserva pasada” o solape, normalmente será 409; en ambos casos se muestra aquí
      mostrarErrorEdicion(msg);
    });
}

function mostrarErrorEdicion(msg) {
  $("#editError").removeClass("d-none").text(msg);
}
```


**3) Comportamiento esperado**

* El cliente pulsa **Editar** en una reserva → se abre modal con:

  * instalación actual seleccionada
  * día e inicio/fin precargados
* Al guardar:

  * si la reserva es futura y no hay solape → 200 OK, se cierra modal y refresca tabla
  * si es pasada → 409 Conflict con mensaje “No se puede modificar una reserva pasada”
  * si solapa → 409 Conflict
  * si el rango es inválido → 400 Bad Request


**4) Mejora para UX**

* En la tabla de reservas, **deshabilitar** el botón Editar/Borrar si la reserva ya es pasada (usando la misma lógica en JS comparando con la fecha/hora actual).
  Esto no sustituye la seguridad del backend, pero evita frustración en el usuario.

```javascript
function renderMisReservas(items) {
  const instName = new Map((cacheInstalaciones || []).map(i => [i.id, `${i.nombre} (${i.ciudad})`]));

  const rows = (items || []).map(r => {
    const h = r.horario || {};
    const snap = h.instalacionSnapshot || {};
    const instId = snap.instalacionId;
    const inst = instName.get(instId) || snap.nombre || instId || "";

    const pasada = reservaYaPasadaUI(r);
    const disAttr = pasada ? "disabled" : "";
    const titleAttr = pasada ? `title="Reserva pasada: no editable"` : "";

    return `
      <tr>
        <td>${escapeHtml(h.dia || "")}</td>
        <td>${escapeHtml((h.horaInicio || "") + " - " + (h.horaFin || ""))}</td>
        <td>${escapeHtml(inst)}</td>
        <td class="text-end">
          <button class="btn btn-sm btn-outline-primary me-2"
                  data-edit-res="${r.id}" ${disAttr} ${titleAttr}>
            Editar
          </button>
          <button class="btn btn-sm btn-outline-danger"
                  data-del-res="${r.id}" ${disAttr} ${titleAttr}>
            Borrar
          </button>
        </td>
      </tr>
    `;
  }).join("");

  $("#tabMisReservas").html(rows || `<tr><td colspan="4" class="text-center text-muted">Sin reservas</td></tr>`);

  // Editar
  $("#tabMisReservas button[data-edit-res]").off("click").on("click", function () {
    if ($(this).is(":disabled")) return;
    const id = $(this).data("edit-res");
    abrirModalEdicion(id);
  });

  // Borrar
  $("#tabMisReservas button[data-del-res]").off("click").on("click", function () {
    if ($(this).is(":disabled")) return;
    const id = $(this).data("del-res");
    if (!confirm("¿Borrar reserva? (solo se permitirá si es futura)")) return;
    $.ajax({ method: "DELETE", url: `${API.cliente.reservas}/${id}` })
      .done(() => { showAlert("success", "Reserva borrada"); cargarMisReservas(); })
      .fail(xhr => showAlert("danger", parseApiError(xhr, "Error borrando reserva")));
  });
}

```

