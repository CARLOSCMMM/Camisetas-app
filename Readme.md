# Spring Boot REST + MongoDB + SPA ligera

Aplicacion con backend REST en Spring Boot, MongoDB y una SPA ligera con HTML, Bootstrap y jQuery.

## Requisitos
- Docker Desktop
- Visual Studio Code + extension Dev Containers

## Configuracion de entorno
1. Copiar `.env.example` a `.env` (no se versiona)
   - PowerShell: `Copy-Item .env.example .env`
   - Bash: `cp .env.example .env`

## Arranque del DevContainer
1. Abrir la carpeta en VS Code.
2. Command Palette: `Dev Containers: Reopen in Container`.
3. Esperar a que termine la construccion del contenedor.

## Como iniciar backend
En la terminal del contenedor:

```bash
./mvnw spring-boot:run
```

Backend: `http://localhost:8080`
Mongo Express: `http://localhost:8081`

## Comprobar endpoints (curl)
Listados:

```bash
curl http://localhost:8080/api/camisetas
curl http://localhost:8080/api/usuarios
curl http://localhost:8080/api/pedidos
curl http://localhost:8080/api/instalaciones
curl http://localhost:8080/api/horarios
curl http://localhost:8080/api/reservas
```

Alta de ejemplo (camiseta):

```bash
curl -X POST http://localhost:8080/api/camisetas \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Basica","talla":"M","color":"Negro","precio":9.99,"stock":10}'
```

Alta de ejemplo (usuario):

```bash
curl -X POST http://localhost:8080/api/usuarios \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Ana","email":"ana@example.com","password":"1234","rol":"ADMIN"}'
```

## Probar UI (pasos y capturas)
1. Abrir `http://localhost:8080/` en el navegador.
2. Camisetas: crear, editar y borrar.
   - Capturas: `docs/img/listado-camisetas.png`, `docs/img/editar-camisetas.png`, `docs/img/eliminar-camisetas.png`
3. Usuarios: alta, edicion y borrado.
   - Capturas: `docs/img/crud-usuarios.png`, `docs/img/eliminar-editar-usuarios.png`
4. Pedidos: crear pedido y ver maestro-detalle.
   - Capturas: `docs/img/vista-pedidos.png`, `docs/img/maestro-detalle-pedido.png`, `docs/img/eliminar-pedido.png`
