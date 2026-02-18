/* global $ */

const API = {
  camisetas: "/api/camisetas",
  usuarios: "/api/usuarios",
  pedidos: "/api/pedidos"
};

const state = {
  camisetas: [],
  usuarios: [],
  pedidos: []
};

/* =========================
   Eventos de formularios
   ========================= */

function wireEvents() {
  $("#formCamiseta").on("submit", function (e) {
    e.preventDefault();
    guardarCamiseta();
  });

  $("#btnCamisetaCancelar").on("click", function () {
    resetCamisetaForm();
  });

  $("#formUsuario").on("submit", function (e) {
    e.preventDefault();
    guardarUsuario();
  });

  $("#btnUsuarioCancelar").on("click", function () {
    resetUsuarioForm();
  });

  $("#formPedido").on("submit", function (e) {
    e.preventDefault();
    crearPedido();
  });

  $("#menu_camisetas").on("click", function () {
    cargarCamisetas();
  });

  $("#menu_usuarios").on("click", function () {
    cargarUsuarios();
  });

  $("#menu_pedidos").on("click", function () {
    cargarPedidos();
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
  return String(s ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;");
}

function formatMoney(v) {
  const n = Number(v);
  if (Number.isNaN(n)) return "0.00";
  return n.toFixed(2);
}

function formatFecha(s) {
  if (!s) return "";
  return String(s).replace("T", " ");
}

/* =========================
   Carga inicial
   ========================= */

function cargarTodo() {
  $.when(cargarCamisetas(), cargarUsuarios())
    .done(function () {
      cargarPedidos();
    })
    .fail(function () {
      showAlert("danger", "Error cargando datos iniciales");
    });
}

/* =========================
   Camisetas
   ========================= */

function cargarCamisetas() {
  return $.getJSON(API.camisetas)
    .done(function (data) {
      state.camisetas = data || [];
      renderCamisetas(state.camisetas);
      rellenarSelectCamisetas(state.camisetas);
    })
    .fail(function (xhr) {
      showAlert("danger", parseApiError(xhr, "Error cargando camisetas"));
    });
}

function renderCamisetas(camisetas) {
  const rows = (camisetas || []).map(function (c) {
    return `
      <tr>
        <td>${escapeHtml(c.nombre)}</td>
        <td>${escapeHtml(c.talla)}</td>
        <td>${escapeHtml(c.color)}</td>
        <td>${formatMoney(c.precio)}</td>
        <td>${escapeHtml(c.stock)}</td>
        <td class="text-end">
          <button class="btn btn-sm btn-outline-secondary me-1" data-action="edit-cam" data-id="${c.id}">
            Editar
          </button>
          <button class="btn btn-sm btn-outline-danger" data-action="del-cam" data-id="${c.id}">
            Eliminar
          </button>
        </td>
      </tr>
    `;
  }).join("");

  $("#tablaCamisetas").html(rows || `<tr><td colspan="6" class="text-center text-muted">Sin datos</td></tr>`);

  $("#tablaCamisetas button[data-action='edit-cam']").off("click").on("click", function () {
    const id = $(this).data("id");
    const c = state.camisetas.find(x => x.id === id);
    if (c) startEditCamiseta(c);
  });

  $("#tablaCamisetas button[data-action='del-cam']").off("click").on("click", function () {
    const id = $(this).data("id");
    eliminarCamiseta(id);
  });
}

function startEditCamiseta(c) {
  $("#camId").val(c.id);
  $("#camNombre").val(c.nombre);
  $("#camTalla").val(c.talla);
  $("#camColor").val(c.color);
  $("#camPrecio").val(c.precio);
  $("#camStock").val(c.stock);
  $("#btnCamisetaGuardar").text("Actualizar");
  $("#btnCamisetaCancelar").removeClass("d-none");
}

function resetCamisetaForm() {
  $("#formCamiseta")[0].reset();
  $("#camId").val("");
  $("#btnCamisetaGuardar").text("Anadir");
  $("#btnCamisetaCancelar").addClass("d-none");
}

function guardarCamiseta() {
  const id = $("#camId").val().trim();
  const payload = {
    nombre: $("#camNombre").val().trim(),
    talla: $("#camTalla").val(),
    color: $("#camColor").val().trim(),
    precio: Number($("#camPrecio").val()),
    stock: Number($("#camStock").val())
  };

  const isEdit = id !== "";
  const url = isEdit ? `${API.camisetas}/${id}` : API.camisetas;
  const method = isEdit ? "PUT" : "POST";

  $.ajax({
    url,
    method,
    contentType: "application/json",
    data: JSON.stringify(payload)
  })
    .done(function () {
      showAlert("success", isEdit ? "Camiseta actualizada" : "Camiseta creada");
      resetCamisetaForm();
      cargarCamisetas();
    })
    .fail(function (xhr) {
      showAlert("danger", parseApiError(xhr, "Error guardando camiseta"));
    });
}

function eliminarCamiseta(id) {
  if (!confirm("Eliminar la camiseta?")) return;

  $.ajax({
    url: `${API.camisetas}/${id}`,
    method: "DELETE"
  })
    .done(function () {
      showAlert("success", "Camiseta eliminada");
      cargarCamisetas();
    })
    .fail(function (xhr) {
      showAlert("danger", parseApiError(xhr, "Error eliminando camiseta"));
    });
}

function rellenarSelectCamisetas(camisetas) {
  const opts = (camisetas || []).map(c =>
    `<option value="${c.id}">${escapeHtml(c.nombre)} (${escapeHtml(c.talla)} - ${escapeHtml(c.color)})</option>`
  ).join("");
  $("#pedCamisetas").html(opts);
}

/* =========================
   Usuarios
   ========================= */

function cargarUsuarios() {
  return $.getJSON(API.usuarios)
    .done(function (data) {
      state.usuarios = data || [];
      renderUsuarios(state.usuarios);
      rellenarSelectUsuarios(state.usuarios);
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
        <td>${escapeHtml(u.rol)}</td>
        <td class="text-end">
          <button class="btn btn-sm btn-outline-secondary me-1" data-action="edit-user" data-id="${u.id}">
            Editar
          </button>
          <button class="btn btn-sm btn-outline-danger" data-action="del-user" data-id="${u.id}">
            Eliminar
          </button>
        </td>
      </tr>
    `;
  }).join("");

  $("#tablaUsuarios").html(rows || `<tr><td colspan="4" class="text-center text-muted">Sin datos</td></tr>`);

  $("#tablaUsuarios button[data-action='edit-user']").off("click").on("click", function () {
    const id = $(this).data("id");
    const u = state.usuarios.find(x => x.id === id);
    if (u) startEditUsuario(u);
  });

  $("#tablaUsuarios button[data-action='del-user']").off("click").on("click", function () {
    const id = $(this).data("id");
    eliminarUsuario(id);
  });
}

function startEditUsuario(u) {
  $("#userId").val(u.id);
  $("#userNombre").val(u.nombre);
  $("#userEmail").val(u.email);
  $("#userPassword").val("");
  $("#userRol").val(u.rol || "ADMIN");
  $("#btnUsuarioGuardar").text("Actualizar");
  $("#btnUsuarioCancelar").removeClass("d-none");
}

function resetUsuarioForm() {
  $("#formUsuario")[0].reset();
  $("#userId").val("");
  $("#btnUsuarioGuardar").text("Anadir");
  $("#btnUsuarioCancelar").addClass("d-none");
}

function guardarUsuario() {
  const id = $("#userId").val().trim();
  const payload = {
    nombre: $("#userNombre").val().trim(),
    email: $("#userEmail").val().trim(),
    password: $("#userPassword").val().trim(),
    rol: $("#userRol").val()
  };

  const isEdit = id !== "";
  const url = isEdit ? `${API.usuarios}/${id}` : API.usuarios;
  const method = isEdit ? "PUT" : "POST";

  $.ajax({
    url,
    method,
    contentType: "application/json",
    data: JSON.stringify(payload)
  })
    .done(function () {
      showAlert("success", isEdit ? "Usuario actualizado" : "Usuario creado");
      resetUsuarioForm();
      cargarUsuarios();
    })
    .fail(function (xhr) {
      showAlert("danger", parseApiError(xhr, "Error guardando usuario"));
    });
}

function eliminarUsuario(id) {
  if (!confirm("Eliminar el usuario?")) return;

  $.ajax({
    url: `${API.usuarios}/${id}`,
    method: "DELETE"
  })
    .done(function () {
      showAlert("success", "Usuario eliminado");
      cargarUsuarios();
    })
    .fail(function (xhr) {
      showAlert("danger", parseApiError(xhr, "Error eliminando usuario"));
    });
}

function rellenarSelectUsuarios(usuarios) {
  const opts = (usuarios || []).map(u =>
    `<option value="${u.id}">${escapeHtml(u.nombre)} (${escapeHtml(u.email)})</option>`
  ).join("");

  $("#pedUsuario").html(`<option value="" disabled selected>Seleccione...</option>${opts}`);
}

/* =========================
   Pedidos
   ========================= */

function cargarPedidos() {
  return $.getJSON(API.pedidos)
    .done(function (data) {
      state.pedidos = data || [];
      renderPedidos(state.pedidos);
    })
    .fail(function (xhr) {
      showAlert("danger", parseApiError(xhr, "Error cargando pedidos"));
    });
}

function crearPedido() {
  const usuarioId = $("#pedUsuario").val();
  const camisetasIds = $("#pedCamisetas").val() || [];

  if (!usuarioId) {
    showAlert("warning", "Seleccione un usuario");
    return;
  }
  if (camisetasIds.length === 0) {
    showAlert("warning", "Seleccione al menos una camiseta");
    return;
  }

  const payload = {
    usuarioId,
    camisetasIds
  };

  $.ajax({
    url: API.pedidos,
    method: "POST",
    contentType: "application/json",
    data: JSON.stringify(payload)
  })
    .done(function () {
      showAlert("success", "Pedido creado");
      $("#formPedido")[0].reset();
      cargarPedidos();
    })
    .fail(function (xhr) {
      showAlert("danger", parseApiError(xhr, "Error creando pedido"));
    });
}

function eliminarPedido(id) {
  if (!confirm("Eliminar el pedido?")) return;

  $.ajax({
    url: `${API.pedidos}/${id}`,
    method: "DELETE"
  })
    .done(function () {
      showAlert("success", "Pedido eliminado");
      cargarPedidos();
    })
    .fail(function (xhr) {
      showAlert("danger", parseApiError(xhr, "Error eliminando pedido"));
    });
}

function renderPedidos(pedidos) {
  const rows = (pedidos || []).map(function (p) {
    const usuario = p.usuario || {};
    const items = Array.isArray(p.camisetas) ? p.camisetas : [];
    const total = items.reduce((acc, c) => acc + Number(c.precio || 0), 0);

    const detalleRows = items.map(c => `
      <tr>
        <td>${escapeHtml(c.nombre)}</td>
        <td>${escapeHtml(c.talla)}</td>
        <td>${escapeHtml(c.color)}</td>
        <td>${formatMoney(c.precio)}</td>
      </tr>
    `).join("");

    const detalleTable = `
      <div class="table-responsive">
        <table class="table table-sm mb-0">
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Talla</th>
              <th>Color</th>
              <th>Precio</th>
            </tr>
          </thead>
          <tbody>
            ${detalleRows || `<tr><td colspan="4" class="text-center text-muted">Sin items</td></tr>`}
          </tbody>
        </table>
      </div>
    `;

    return `
      <tr>
        <td>${escapeHtml(formatFecha(p.fechaCreacion))}</td>
        <td>${escapeHtml(usuario.nombre || usuario.email || "")}</td>
        <td>${items.length}</td>
        <td>${formatMoney(total)}</td>
        <td class="text-end">
          <button class="btn btn-sm btn-outline-secondary me-1" data-action="toggle-detail" data-id="${p.id}">
            Detalle
          </button>
          <button class="btn btn-sm btn-outline-danger" data-action="del-ped" data-id="${p.id}">
            Eliminar
          </button>
        </td>
      </tr>
      <tr class="pedido-detalle d-none" data-detail-for="${p.id}">
        <td colspan="5">${detalleTable}</td>
      </tr>
    `;
  }).join("");

  $("#tablaPedidos").html(rows || `<tr><td colspan="5" class="text-center text-muted">Sin datos</td></tr>`);

  $("#tablaPedidos button[data-action='toggle-detail']").off("click").on("click", function () {
    const id = $(this).data("id");
    $(`#tablaPedidos tr[data-detail-for='${id}']`).toggleClass("d-none");
  });

  $("#tablaPedidos button[data-action='del-ped']").off("click").on("click", function () {
    const id = $(this).data("id");
    eliminarPedido(id);
  });
}
