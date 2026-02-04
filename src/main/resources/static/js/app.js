/* global $ */

const API = {
  instalaciones: "/api/instalaciones",
  usuarios: "/api/usuarios",
  reservas: "/api/reservas",
  horarios: "api/horarios"
};

$(document).ready(function () {
  wireEvents();
  cargarTodo();
});

/* =========================
   Eventos de formularios
   ========================= */

function wireEvents() {
  $("#formInstalacion").on("submit", function (e) {
    e.preventDefault();
    crearInstalacion();
  });

  $("#formUsuario").on("submit", function (e) {
    e.preventDefault();
    crearUsuario();
  });

  $("#formHorario").on("submit", function (e) {
    e.preventDefault();
    crearHorario();
  });

  $("#formReserva").on("submit", function (e) {
    e.preventDefault();
    crearReserva();
  });

  $("#formFiltroReservas").on("submit", function (e) {
    e.preventDefault();
    cargarReservasConFiltros();
  });
}

/* =========================
   Alertas y utilidades
   ========================= */

function showAlert(type, msg) {
  $("#alerta")
    .removeClass("d-none alert-success alert-danger alert-warning alert-info")
    .addClass("alert-" + type)
    .text(msg);

  setTimeout(() => $("#alerta").addClass("d-none"), 3000);
}

function parseApiError(xhr, fallbackMsg) {
  const r = xhr.responseJSON;
  if (!r) return fallbackMsg;

  if (Array.isArray(r.details) && r.details.length > 0) {
    return `${r.message}: ${r.details.join(" | ")}`;
  }
  return r.message || fallbackMsg;
}

function escapeHtml(s) {
  return String(s)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;");
}

/* =========================
   Carga inicial
   ========================= */

function cargarTodo() {
  $.when(cargarInstalaciones(), cargarUsuarios(), cargarHorarios())
    .done(function () {
      cargarReservas();
    })
    .fail(function () {
      showAlert("danger", "Error cargando datos iniciales");
    });
}

/* =========================
   Instalaciones
   ========================= */

function cargarInstalaciones() {
  return $.getJSON(API.instalaciones)
    .done(function (data) {
      renderInstalaciones(data);
      rellenarSelectInstalaciones(data);
    })
    .fail(function (xhr) {
      showAlert("danger", parseApiError(xhr, "Error cargando instalaciones"));
    });
}

function renderInstalaciones(instalaciones) {
  const rows = (instalaciones || []).map(function (i) {
    return `
      <tr>
        <td>${escapeHtml(i.nombre)}</td>
        <td>${escapeHtml(i.direccion)}</td>
        <td>${escapeHtml(i.ciudad)}</td>
        <td class="text-end">
          <button class="btn btn-sm btn-outline-danger" data-action="del-inst" data-id="${i.id}">
            Eliminar
          </button>
        </td>
      </tr>
    `;
  }).join("");

  $("#tablaInstalaciones").html(rows || `<tr><td colspan="4" class="text-center text-muted">Sin datos</td></tr>`);

  // Delegación de eventos para botones generados dinámicamente
  $("#tablaInstalaciones button[data-action='del-inst']").off("click").on("click", function () {
    const id = $(this).data("id");
    eliminarInstalacion(id);
  });
}

function rellenarSelectInstalaciones(instalaciones) {
  const opts = (instalaciones || []).map(i =>
    `<option value="${i.id}">${escapeHtml(i.nombre)} (${escapeHtml(i.ciudad)})</option>`
  ).join("");

  // Selects: filtros y alta
  $("#filtroInstalacion").html(`<option value="">Todas</option>${opts}`);
  $("#resInstalacion").html(`<option value="" disabled selected>Seleccione...</option>${opts}`);
  $("#horInstalacion").html(`<option value="" disabled selected>Seleccione...</option>${opts}`);
}

function crearInstalacion() {
  const payload = {
    id: $("#instID").val().trim(),
    nombre: $("#instNombre").val().trim(),
    direccion: $("#instDireccion").val().trim(),
    ciudad: $("#instCiudad").val().trim()
  };

  $.ajax({
    url: API.instalaciones,
    method: "POST",
    contentType: "application/json",
    data: JSON.stringify(payload)
  })
    .done(function () {
      showAlert("success", "Instalación creada");
      $("#formInstalacion")[0].reset();
      cargarInstalaciones().done(cargarReservasConFiltros);
    })
    .fail(function (xhr) {
      showAlert("danger", parseApiError(xhr, "Error creando instalación"));
    });
}

/* =========================
   horarios
   ========================= */


function cargarHorarios() {
  return $.getJSON(API.horarios)
    .done(function (data) {
      $('#tablaHorarios').empty();
      $('#tablaHorarios').append(renderHorarios(data));
      rellenarSelectInstalaciones(data);
    })
    .fail(function (xhr) {
      showAlert("danger", parseApiError(xhr, "Error cargando horarios"));
    });
}

function crearHorario() {
  const payload = {
    //id: $("#instID").val().trim(),
    instalacion: $("#horInstalacion").val(),
    horaInicio: $("#horHoraInicio").val(),
    horaFin: $("#horHoraFin").val(),
  };

  $.ajax({
    url: API.horarios,
    method: "POST",
    contentType: "application/json",
    data: JSON.stringify(payload)
  })
    .done(function () {
      showAlert("success", "Horario creado");
      $("#formHorario")[0].reset();
      cargarHorarios();
    })
    .fail(function (xhr) {
      showAlert("danger", parseApiError(xhr, "Error creando horario"));
    });
}


function renderHorarios(horarios) {
  const rows = (horarios || []).map(function (h) {
    return `
      <tr>
        <td>${escapeHtml(h.instalacion.nombre)}</td>
        <td>${escapeHtml(h.horaInicio)}</td>
        <td>${escapeHtml(h.horaFin)}</td>
        <td class="text-end">
          <button class="btn btn-sm btn-outline-danger" data-action="del-inst" data-id="${h.id}">
            Eliminar
          </button>
        </td>
      </tr>
    `;
  }).join("");
}


function eliminarInstalacion(id) {
  if (!confirm("¿Eliminar la instalación?")) return;

  $.ajax({
    url: `${API.instalaciones}/${id}`,
    method: "DELETE"
  })
    .done(function () {
      showAlert("success", "Instalación eliminada");
      cargarInstalaciones().done(cargarReservasConFiltros);
    })
    .fail(function (xhr) {
      showAlert("danger", parseApiError(xhr, "Error eliminando instalación"));
    });
}

/* =========================
   Usuarios
   ========================= */

function cargarUsuarios() {
  return $.getJSON(API.usuarios)
    .done(function (data) {
      renderUsuarios(data);
      rellenarSelectUsuarios(data);
    })
    .fail(function (xhr) {
      showAlert("danger", parseApiError(xhr, "Error cargando usuarios"));
    });
}

function renderUsuarios(usuarios) {
  const rows = (usuarios || []).map(function (u) {
    return `
      <tr>
        <td>${escapeHtml(u.nombre)}</td>
        <td>${escapeHtml(u.email)}</td>
        <td class="text-end">
          <button class="btn btn-sm btn-outline-danger" data-action="del-user" data-id="${u.id}">
            Eliminar
          </button>
        </td>
      </tr>
    `;
  }).join("");

  $("#tablaUsuarios").html(rows || `<tr><td colspan="3" class="text-center text-muted">Sin datos</td></tr>`);

  $("#tablaUsuarios button[data-action='del-user']").off("click").on("click", function () {
    const id = $(this).data("id");
    eliminarUsuario(id);
  });
}

function rellenarSelectUsuarios(usuarios) {
  const opts = (usuarios || []).map(u =>
    `<option value="${u.id}">${escapeHtml(u.nombre)} (${escapeHtml(u.email)})</option>`
  ).join("");

  $("#filtroUsuario").html(`<option value="">Todos</option>${opts}`);
  $("#resUsuario").html(`<option value="" disabled selected>Seleccione...</option>${opts}`);
}

function crearUsuario() {
  const payload = {
    nombre: $("#userNombre").val().trim(),
    email: $("#userEmail").val().trim()
  };

  $.ajax({
    url: API.usuarios,
    method: "POST",
    contentType: "application/json",
    data: JSON.stringify(payload)
  })
    .done(function () {
      showAlert("success", "Usuario creado");
      $("#formUsuario")[0].reset();
      cargarUsuarios().done(cargarReservasConFiltros);
    })
    .fail(function (xhr) {
      showAlert("danger", parseApiError(xhr, "Error creando usuario"));
    });
}

function eliminarUsuario(id) {
  if (!confirm("¿Eliminar el usuario?")) return;

  $.ajax({
    url: `${API.usuarios}/${id}`,
    method: "DELETE"
  })
    .done(function () {
      showAlert("success", "Usuario eliminado");
      cargarUsuarios().done(cargarReservasConFiltros);
    })
    .fail(function (xhr) {
      showAlert("danger", parseApiError(xhr, "Error eliminando usuario"));
    });
}

/* =========================
   Reservas
   ========================= */

function cargarReservas() {
  return $.getJSON(API.reservas)
    .done(function (data) {
      renderReservas(data);
    })
    .fail(function (xhr) {
      showAlert("danger", parseApiError(xhr, "Error cargando reservas"));
    });
}

function cargarReservasConFiltros() {
  const usuarioId = $("#filtroUsuario").val();
  const instalacionId = $("#filtroInstalacion").val();
  const dia = $("#filtroDia").val();

  const params = {};
  if (usuarioId) params.usuarioId = usuarioId;
  if (instalacionId) params.instalacionId = instalacionId;
  if (dia) params.dia = dia;

  const query = $.param(params);
  const url = query ? `${API.reservas}?${query}` : API.reservas;

  return $.getJSON(url)
    .done(function (data) {
      renderReservas(data);
    })
    .fail(function (xhr) {
      showAlert("danger", parseApiError(xhr, "Error filtrando reservas"));
    });
}

function crearReserva() {
  const payload = {
    usuarioId: $("#resUsuario").val(),
    instalacionId: $("#resInstalacion").val(),
    dia: $("#resDia").val(),
    horaInicio: $("#resHoraInicio").val(),
    horaFin: $("#resHoraFin").val()
  };

  $.ajax({
    url: API.reservas,
    method: "POST",
    contentType: "application/json",
    data: JSON.stringify(payload)
  })
    .done(function () {
      showAlert("success", "Reserva creada");
      $("#formReserva")[0].reset();
      cargarReservasConFiltros();
    })
    .fail(function (xhr) {
      // 409 típico por solape, 400 por validación, 404 por usuario/instalación inexistente
      showAlert("danger", parseApiError(xhr, "Error creando reserva"));
    });
}

function eliminarReserva(id) {
  if (!confirm("¿Eliminar la reserva?")) return;

  $.ajax({
    url: `${API.reservas}/${id}`,
    method: "DELETE"
  })
    .done(function () {
      showAlert("success", "Reserva eliminada");
      cargarReservasConFiltros();
    })
    .fail(function (xhr) {
      showAlert("danger", parseApiError(xhr, "Error eliminando reserva"));
    });
}

function renderReservas(reservas) {
  const usuariosMap = construirUsuariosMap();
  const rows = (reservas || []).map(function (r) {
    const h = r.horario || {};
    const snap = h.instalacionSnapshot || {};
    const usuarioNombre = usuariosMap.get(r.usuarioId) || r.usuarioId;

    const dia = h.dia || "";
    const tramo = `${h.horaInicio || ""} - ${h.horaFin || ""}`;
    const instalacion = snap.nombre ? `${snap.nombre} (${snap.ciudad || ""})` : (snap.instalacionId || "");

    return `
      <tr>
        <td>${escapeHtml(dia)}</td>
        <td>${escapeHtml(tramo)}</td>
        <td>${escapeHtml(instalacion)}</td>
        <td>${escapeHtml(usuarioNombre)}</td>
        <td class="text-end">
          <button class="btn btn-sm btn-outline-danger" data-action="del-res" data-id="${r.id}">
            Eliminar
          </button>
        </td>
      </tr>
    `;
  }).join("");

  $("#tablaReservas").html(rows || `<tr><td colspan="5" class="text-center text-muted">Sin datos</td></tr>`);

  $("#tablaReservas button[data-action='del-res']").off("click").on("click", function () {
    const id = $(this).data("id");
    eliminarReserva(id);
  });
}

/* =========================
   Mapas auxiliares
   ========================= */

function construirUsuariosMap() {
  // Construye un mapa id -> nombre leyendo la tabla ya cargada.
  // Alternativa: guardar el último listado en una variable global.
  const map = new Map();

  // Se intenta construir desde el select de reservas (que contiene nombre + email).
  $("#resUsuario option").each(function () {
    const val = $(this).attr("value");
    const txt = $(this).text();
    if (val) map.set(val, txt);
  });

  return map;
}