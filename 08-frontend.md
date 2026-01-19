## Front con Bootstrap: estructura y componentes

En este apartado se diseña el frontend de la aplicación como una interfaz web ligera basada en **HTML5 + Bootstrap 5**. El objetivo no es construir una SPA compleja, sino una interfaz clara y funcional que permita:

* Consultar instalaciones, usuarios y reservas.
* Crear y eliminar registros.
* Crear reservas indicando usuario, instalación y franja horaria.
* Mostrar mensajes de estado (éxito, error, validación).
* Preparar la estructura para que, en el siguiente apartado, se conecte la interfaz a la API mediante jQuery (AJAX).

Para mantener el proyecto simple y alineado con el entorno, se servirá el frontend desde Spring Boot en:

`src/main/resources/static/`


### Estructura de archivos recomendada

Se propone separar HTML, CSS y JavaScript:

```
src/main/resources/static/
  index.html
  css/
    app.css
  js/
    app.js
```

* `index.html` contendrá el marcado y los contenedores de la interfaz.
* `app.css` incluirá pequeños ajustes de estilo (si son necesarios).
* `app.js` contendrá la lógica de interfaz y, posteriormente, las llamadas AJAX.


### Diseño de la interfaz

La aplicación tendrá tres áreas funcionales:

1. **Instalaciones**: listado y alta rápida.
2. **Usuarios**: listado y alta rápida.
3. **Reservas**: listado con filtros y formulario de alta.

Se implementará con componentes Bootstrap estándar:

* `navbar` para navegación.
* `card` para secciones.
* `table` para listados.
* `form` para altas y filtros.
* `modal` (opcional) para edición o confirmación.
* `alert` para mensajes de éxito/error.


### `index.html` (estructura base)

A continuación se define una versión inicial del `index.html` con Bootstrap y contenedores listos para enlazar con JavaScript. Esta versión aún no realiza peticiones a la API; únicamente prepara la estructura.

```html
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Reservas de Pistas</title>

  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  <link rel="stylesheet" href="css/app.css">
</head>

<body class="bg-light">

<nav class="navbar navbar-expand-lg navbar-dark bg-dark">
  <div class="container">
    <a class="navbar-brand" href="#">Reservas de Pistas</a>
    <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#nav">
      <span class="navbar-toggler-icon"></span>
    </button>

    <div class="collapse navbar-collapse" id="nav">
      <ul class="navbar-nav ms-auto">
        <li class="nav-item"><a class="nav-link" href="#instalaciones">Instalaciones</a></li>
        <li class="nav-item"><a class="nav-link" href="#usuarios">Usuarios</a></li>
        <li class="nav-item"><a class="nav-link" href="#reservas">Reservas</a></li>
      </ul>
    </div>
  </div>
</nav>

<main class="container py-4">

  <!-- Área de alertas -->
  <div id="alerta" class="alert d-none" role="alert"></div>

  <!-- Instalaciones -->
  <section id="instalaciones" class="mb-4">
    <div class="card shadow-sm">
      <div class="card-header fw-semibold">Instalaciones</div>
      <div class="card-body">

        <form id="formInstalacion" class="row g-2 align-items-end">
          <div class="col-md-4">
            <label class="form-label">Nombre</label>
            <input id="instNombre" class="form-control" required>
          </div>
          <div class="col-md-4">
            <label class="form-label">Dirección</label>
            <input id="instDireccion" class="form-control" required>
          </div>
          <div class="col-md-3">
            <label class="form-label">Ciudad</label>
            <input id="instCiudad" class="form-control" required>
          </div>
          <div class="col-md-1 d-grid">
            <button class="btn btn-primary" type="submit">Añadir</button>
          </div>
        </form>

        <hr>

        <div class="table-responsive">
          <table class="table table-hover align-middle">
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Dirección</th>
                <th>Ciudad</th>
                <th class="text-end">Acciones</th>
              </tr>
            </thead>
            <tbody id="tablaInstalaciones"></tbody>
          </table>
        </div>

      </div>
    </div>
  </section>

  <!-- Usuarios -->
  <section id="usuarios" class="mb-4">
    <div class="card shadow-sm">
      <div class="card-header fw-semibold">Usuarios</div>
      <div class="card-body">

        <form id="formUsuario" class="row g-2 align-items-end">
          <div class="col-md-5">
            <label class="form-label">Nombre</label>
            <input id="userNombre" class="form-control" required>
          </div>
          <div class="col-md-5">
            <label class="form-label">Email</label>
            <input id="userEmail" type="email" class="form-control" required>
          </div>
          <div class="col-md-2 d-grid">
            <button class="btn btn-primary" type="submit">Añadir</button>
          </div>
        </form>

        <hr>

        <div class="table-responsive">
          <table class="table table-hover align-middle">
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Email</th>
                <th class="text-end">Acciones</th>
              </tr>
            </thead>
            <tbody id="tablaUsuarios"></tbody>
          </table>
        </div>

      </div>
    </div>
  </section>

  <!-- Reservas -->
  <section id="reservas">
    <div class="card shadow-sm">
      <div class="card-header fw-semibold">Reservas</div>
      <div class="card-body">

        <!-- Filtros -->
        <form id="formFiltroReservas" class="row g-2 align-items-end">
          <div class="col-md-4">
            <label class="form-label">Usuario</label>
            <select id="filtroUsuario" class="form-select">
              <option value="">Todos</option>
            </select>
          </div>
          <div class="col-md-4">
            <label class="form-label">Instalación</label>
            <select id="filtroInstalacion" class="form-select">
              <option value="">Todas</option>
            </select>
          </div>
          <div class="col-md-3">
            <label class="form-label">Día</label>
            <input id="filtroDia" type="date" class="form-control">
          </div>
          <div class="col-md-1 d-grid">
            <button class="btn btn-outline-secondary" type="submit">Filtrar</button>
          </div>
        </form>

        <hr>

        <!-- Alta de reserva -->
        <form id="formReserva" class="row g-2 align-items-end">
          <div class="col-md-4">
            <label class="form-label">Usuario</label>
            <select id="resUsuario" class="form-select" required></select>
          </div>
          <div class="col-md-4">
            <label class="form-label">Instalación</label>
            <select id="resInstalacion" class="form-select" required></select>
          </div>
          <div class="col-md-2">
            <label class="form-label">Día</label>
            <input id="resDia" type="date" class="form-control" required>
          </div>
          <div class="col-md-1">
            <label class="form-label">Inicio</label>
            <input id="resHoraInicio" type="time" class="form-control" required>
          </div>
          <div class="col-md-1">
            <label class="form-label">Fin</label>
            <input id="resHoraFin" type="time" class="form-control" required>
          </div>
          <div class="col-12 d-grid mt-2">
            <button class="btn btn-success" type="submit">Crear reserva</button>
          </div>
        </form>

        <hr>

        <div class="table-responsive">
          <table class="table table-hover align-middle">
            <thead>
              <tr>
                <th>Día</th>
                <th>Horario</th>
                <th>Instalación</th>
                <th>Usuario</th>
                <th class="text-end">Acciones</th>
              </tr>
            </thead>
            <tbody id="tablaReservas"></tbody>
          </table>
        </div>

      </div>
    </div>
  </section>

</main>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="js/app.js"></script>
</body>
</html>
```

### `css/app.css` (opcional)

En general, Bootstrap es suficiente. Se recomienda evitar estilos excesivos y limitarse a pequeños ajustes:

```css
/* app.css */
body {
  min-height: 100vh;
}

#alerta {
  position: sticky;
  top: 1rem;
  z-index: 1020;
}
```

### Plantillas de tabla (contrato de renderizado)

Aunque el renderizado se implementará en el siguiente apartado con jQuery, conviene fijar desde ya qué se mostrará:

#### Tabla de instalaciones

* Nombre
* Dirección
* Ciudad
* Botón eliminar

#### Tabla de usuarios

* Nombre
* Email
* Botón eliminar

#### Tabla de reservas

* Día (`horario.dia`)
* Franja (`horaInicio - horaFin`)
* Instalación (desde snapshot embebido: `horario.instalacionSnapshot.nombre`)
* Usuario (inicialmente se mostrará `usuarioId`; se podrá resolver a nombre en el frontend si se carga la lista de usuarios)
* Botón eliminar


### Objetivo técnico del apartado

Al finalizar este apartado se dispone de:

* Una interfaz web estática con Bootstrap.
* Formularios y tablas con identificadores claros (`id`) para conectar con JavaScript.
* Separación correcta de recursos (instalaciones, usuarios, reservas).
* Preparación para implementar el consumo real de la API mediante AJAX en el siguiente apartado.

En el siguiente apartado se implementará **“Consumo de API desde JS (jQuery)”**, que añadirá:

* Carga inicial de datos (GET).
* Altas (POST) desde formularios.
* Eliminación (DELETE).
* Filtrado de reservas usando parámetros de query.
* Gestión de errores utilizando el formato `ApiError` definido en el backend.

\pagebreak
