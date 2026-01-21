## MongoDB

Para trabajar con MongoDB, no basta con instalar el servidor; es fundamental asegurar el acceso desde el primer momento y conocer las herramientas de conexión.

### Instalación del Servidor y el Shell

Aunque nosotros vamos a trabajar con contenedores Docker para evitar perder el tiempo calentando el plato (ver siguiente apartado), te recordamos que también es posible y muy usada la instalación manual o incluso el uso en línea.

#### Instalación en Windows 11 (vía winget)

Utilizaremos la terminal para automatizar la descarga de la versión Community y el cliente de comandos (`mongosh`).

1. Abre **PowerShell** como administrador.
2. Ejecuta:
```powershell
winget install MongoDB.Server
winget install MongoDB.Shell

```


3. **Prueba de funcionamiento**: Abre una nueva terminal y escribe `mongosh --version`. Debería devolver la versión del shell (ej. 2.x.x).

#### En Ubuntu (Linux)

1. Importa la clave pública y añade el repositorio oficial:
```bash
sudo apt-get install gnupg curl
curl -fsSL https://www.mongodb.org/static/pgp/server-7.0.asc | sudo gpg -o /usr/share/keyrings/mongodb-server-7.0.gpg --dearmor
echo "deb [ arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-7.0.gpg ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list
sudo apt-get update
sudo apt-get install -y mongodb-org

```


2. Inicia el servicio: `sudo systemctl start mongod`.

3. **Prueba de funcionamiento**: Ejecuta `sudo systemctl status mongod` para verificar que el estado es "active (running)".



### Configuración del usuario Root (Primer inicio)

Por defecto, MongoDB permite la conexión sin contraseña la primera vez. Es crítico crear un administrador antes de habilitar la seguridad.

1. Entra al shell: `mongosh`
2. Cambia a la base de datos de administración:
```javascript
use admin

```


3. **Crea el usuario root** (usaremos la clave común para las prácticas):
```javascript
db.createUser({
  user: "root",
  pwd: "Secreto_123",
  roles: [ { role: "root", db: "admin" } ]
})

```


4. **Activa la autenticación**:
* En **Windows**: Edita el archivo `C:\Program Files\MongoDB\Server\X.X\bin\mongod.cfg`.
* En **Ubuntu**: Edita `/etc/mongod.conf`.
* Busca la sección `security` y añade (ojo con la indentación de dos espacios):
```yaml
security:
  authorization: enabled

```


5. **Reinicia el servicio**:
* Windows: `Restart-Service mongodb` (en PowerShell admin).
* Ubuntu: `sudo systemctl restart mongod`.


### Cómo conectar (Pruebas de acceso)

Una vez activada la seguridad, si intentas entrar solo con `mongosh`, no tendrás permisos para ver datos. Debes usar la **Connection String**:

* **Por terminal**:
```bash
mongosh "mongodb://root:Secreto_123@localhost:27017/?authSource=admin"

```


* **Por GUI (MongoDB Compass)**:
Si prefieres una interfaz visual, descarga **MongoDB Compass**. En la barra de conexión, pega la cadena anterior. Verás que aparecen las bases de datos `admin`, `config` y `local`.


### Recuperación del Root (Si olvidas la clave)

Si se pierde el acceso, MongoDB tiene un "modo de emergencia" que permite entrar sin contraseña ignorando las tablas de usuarios:

1. **Detén el servidor**:
* `sudo systemctl stop mongod` (Linux) o detén el servicio en Windows.


2. **Inicia manualmente sin seguridad**:
Abre una terminal y lanza el ejecutable con el parámetro `--noauth`:
```bash
mongod --dbpath /var/lib/mongodb --noauth  ## (Ajusta la ruta según tu SO)

```


3. **Entra y cambia la clave**:
En otra terminal, entra con `mongosh`, ve a `admin` y actualiza la contraseña:
```javascript
use admin
db.changeUserPassword("root", "Secreto_123")

```


4. **Cierra todo y reinicia normalmente**: Mata el proceso manual y vuelve a iniciar el servicio de forma estándar. La seguridad volverá a estar activa con la nueva clave.


> Recordad que `_id` es el identificador único obligatorio. Aunque usemos referencias por `_id`, en MongoDB es habitual duplicar información (como el nombre del alumno en la colección de matrículas) para evitar tener que consultar dos colecciones constantemente.

\pagebreak


## Modelado y Creación de la Base de Datos: Gestión Académica

En MongoDB, la unidad básica es el **documento** (un registro en formato BSON). Estos se agrupan en **colecciones**. A diferencia de las bases de datos relacionales, no forzamos una estructura rígida, lo que nos permite evolucionar el sistema fácilmente.

### Estrategias de Relación: Embebido vs. Referencia

Esta es la decisión más importante en el diseño de MongoDB.

#### Documentos Embebidos (Denormalización)

Consiste en guardar un documento dentro de otro como un subobjeto o un array.

* **Cuándo usarlo:** Cuando los datos se consultan siempre juntos (ej. los datos de contacto de un alumno).
* **Ventaja:** Mayor velocidad de lectura (una sola consulta obtiene todo).
* **Concepto de Snapshot (Foto):** En una matrícula, embebemos el nombre de la asignatura. Si el nombre de la asignatura cambia en el futuro, la matrícula del alumno conserva el nombre que tenía en ese momento. Es una **redundancia intencionada**.

#### Referencias (Normalización)

Consiste en guardar el `_id` de un documento en otro campo.

* **Cuándo usarlo:** Cuando el documento referenciado cambia a menudo o es muy grande.
* **Analogía SQL:** Es lo más parecido a una **Foreign Key (Clave Foránea)**, pero MongoDB no impide que borres el destino (no hay integridad referencial automática).


### Implementación Práctica en la Terminal

Primero, asegúrate de estar conectado con tu usuario root y selecciona la base de datos (se creará al insertar):

```javascript
use gestion_academica

```

#### Paso 1: Creación de Profesores (`insertOne`)

Insertamos un profesor que luego será referenciado. El campo `_id` se genera automáticamente como un **ObjectId** si no lo indicamos.

```javascript
db.profesores.insertOne({
  nombre: "Dra. García",
  departamento: "LSI",
  especialidad: "Bases de Datos",
  fecha_incorporacion: new Date("2015-09-01")
})

```

#### Paso 2: Creación de Asignaturas (`insertMany`)

Usamos `insertMany` para cargar el catálogo rápidamente.

```javascript
db.asignaturas.insertMany([
  { _id: "PROG1", nombre: "Programación I", creditos: 6 },
  { _id: "BD1", nombre: "Bases de Datos I", creditos: 6 },
  { _id: "ISO", nombre: "Introducción a los SO", creditos: 6 }
])

```

#### Paso 3: El Alumno (Modelo Híbrido: Embebido + Referencia)

Este documento es el más complejo. Contiene:

1. **Datos simples**: nombre y expediente.
2. **Documento embebido**: `contacto`.
3. **Array de referencias con Snapshot**: `matriculas`.

```javascript
db.alumnos.insertOne({
  nombre: "Ana López",
  expediente: "AL2024",
  contacto: {
    email: "ana@univ.es",
    tlf: "600111222",
    direccion: "Avda. de la Universidad, 4" 
  },
  matriculas: [
    {
      asignatura_id: "BD1",       // REFERENCIA a la asignatura
      nombre_asig: "Bases de Datos I", // SNAPSHOT (Redundancia)
      nota: 8.5,
      fecha: new Date()
    },
    {
      asignatura_id: "PROG1",
      nombre_asig: "Programación I",
      nota: 9.0,
      fecha: new Date()
    }
  ]
})

```


### Resumen de diferencias 

| Concepto Relacional (SQL) | Concepto MongoDB | Notas de Ingeniería |
| --- | --- | --- |
| **Fila** | **Documento** | El documento puede ser jerárquico (objetos anidados). |
| **Tabla** | **Colección** | No tienen por qué tener todos los mismos campos. |
| **Clave Primaria** | **`_id` (ObjectId)** | 12 bytes: timestamp + id proceso + contador. |
| **JOIN / Foreign Key** | **Referencia o Embebido** | En MongoDB preferimos duplicar datos (embeber) para evitar JOINs costosos. |

> **Importante:** La contraseña de root es **Secreto_123**. No la olvidéis para poder entrar en el shell (`mongosh -u root -p Secreto_123 --authSource admin`).

\pagebreak



## Manipulación de Datos (Operaciones CRUD)

En MongoDB, el CRUD no se realiza mediante sentencias de texto como en SQL, sino mediante métodos de JavaScript ejecutados sobre el objeto `db`.

#### Lectura (Read) con Proyecciones y Operadores

La lectura no solo es `find()`. Es fundamental aprender a filtrar y a elegir qué campos queremos ver (proyección).

* **Uso de Operadores Lógicos y de Comparación:**
No usamos `>`, `<` o `!=`. Usamos operadores BSON: `$gt` (greater than), `$lt` (less than), `$in` (contenido en), `$ne` (not equal).
```javascript
// Buscar alumnos con una nota mayor o igual a 9 en alguna asignatura
db.alumnos.find({ "matriculas.nota": { $gte: 9 } })

// Buscar profesores de "LSI" o "Arquitectura" usando $in
db.profesores.find({ departamento: { $in: ["LSI", "Arquitectura"] } })

```


* **Proyecciones (Elegir columnas):**
El segundo parámetro de `find` indica qué campos queremos (`1`) y cuáles no (`0`).
```javascript
// Solo queremos el nombre y el expediente (el _id sale por defecto)
db.alumnos.find({ "contacto.tlf": { $exists: true } }, { nombre: 1, expediente: 1 })

```


#### Actualizaciones Complejas (Update)

A diferencia de SQL, donde haces un `SET` de toda la fila, en MongoDB usamos **operadores de actualización** para no machacar el documento entero.

* **`$set`**: Modifica un campo o lo crea si no existe.
* **`$inc`**: Incrementa un valor numérico (ideal para contadores).
* **`$push`**: Añade un elemento a un array (fundamental en nuestra colección de alumnos).

```javascript
// Añadir una nueva matrícula al array de un alumno sin borrar las anteriores
db.alumnos.updateOne(
  { expediente: "AL2024" },
  { 
    $push: { 
      matriculas: { asignatura_id: "ISO", nombre_asig: "Sistemas Operativos", nota: 7 } 
    } 
  }
)

// Cambiar el nombre del departamento de un profesor
db.profesores.updateMany(
  { departamento: "LSI" },
  { $set: { departamento: "Lenguajes y Sistemas" } }
)

```

#### Borrado (Delete)

El borrado es definitivo. Se recomienda siempre hacer un `find` previo con el mismo filtro para estar seguros de qué vamos a borrar.

```javascript
// Borrar alumnos que no tengan ninguna matrícula (array vacío)
db.alumnos.deleteMany({ matriculas: { $size: 0 } })

```


## Consultas Complejas: El Aggregation Framework

Cuando necesitamos realizar cálculos (medias, sumas) o unir colecciones, el `find()` se queda corto. Usamos el **Pipeline de Agregación**, que funciona como una "cadena de montaje": el resultado de una etapa entra en la siguiente.

#### El operador `$lookup` (El "JOIN" de MongoDB)

Como vimos en el tema anterior, MongoDB usa referencias. Si queremos obtener una lista de alumnos que incluya los datos detallados del profesor que tienen asignado como tutor, haríamos lo siguiente:

```javascript
db.alumnos.aggregate([
  {
    $lookup: {
      from: "profesores",           // Colección con la que unimos
      localField: "tutor_id",       // Campo en 'alumnos' (la referencia)
      foreignField: "_id",          // Campo en 'profesores'
      as: "info_tutor"              // Nombre del array donde se guardará el resultado
    }
  }
])

```

#### Agrupación y Cálculos (`$group` y `$unwind`)

Si queremos calcular la **nota media de cada alumno**, tenemos un problema: las notas están dentro de un array. Primero debemos "deshacer" el array.

1. **`$unwind`**: Separa el array. Si un alumno tiene 3 matrículas, genera 3 documentos temporales (uno por matrícula).
2. **`$group`**: Agrupa y calcula.

```javascript
db.alumnos.aggregate([
  { $unwind: "$matriculas" }, // "Explotamos" el array de matrículas
  {
    $group: {
      _id: "$nombre",                // Agrupamos por el nombre del alumno
      notaMedia: { $avg: "$matriculas.nota" }, // Calculamos la media
      totalAsignaturas: { $sum: 1 }  // Contamos cuántas hay
    }
  },
  { $match: { notaMedia: { $gte: 5 } } } // Filtramos solo los aprobados
])

```

#### Resumen de Etapas Clave

| Etapa | Función | Analogía SQL |
| --- | --- | --- |
| `$match` | Filtra documentos | `WHERE` / `HAVING` |
| `$project` | Modifica o selecciona campos | `SELECT` |
| `$lookup` | Une colecciones | `LEFT OUTER JOIN` |
| `$group` | Agrupa y realiza cálculos | `GROUP BY` |
| `$sort` | Ordena los resultados | `ORDER BY` |
| `$unwind` | Descompone arrays para tratarlos individualmente | N/A (Específico de NoSQL) |

Aquí tienes una **Cheat Sheet** (Hoja de trucos) diseñada específicamente para que tus alumnos la tengan abierta durante las prácticas de Gestión Académica. Es un resumen compacto de todo lo que hemos visto.


### MongoDB Cheat Sheet: Gestión Académica

#### Conexión y Seguridad

* **Conexión básica**: `mongosh "mongodb://root:Secreto_123@localhost:27017/?authSource=admin"`
* **Clave común**: `Secreto_123`
* **Crear usuario (en DB admin)**: `db.createUser({ user: "root", pwd: "Secreto_123", roles: ["root"] })`

#### Estructura y Navegación

* **Ver bases de datos**: `show dbs`
* **Usar/Crear base de datos**: `use gestion_academica`
* **Ver colecciones**: `show collections`

#### Inserción (Create)

* **Un documento**: `db.alumnos.insertOne({ nombre: "Ana", expediente: "A1" })`
* **Varios documentos**: `db.profesores.insertMany([ { nombre: "Dr. X" }, { nombre: "Dra. Y" } ])`

#### Consultas (Read)

* **Todo**: `db.alumnos.find()`
* **Filtrar por campo**: `db.alumnos.find({ expediente: "A1" })`
* **Operadores de comparación**:
* `$gt` / `$gte`: Mayor / Mayor o igual.
* `$lt` / `$lte`: Menor / Menor o igual.
* `$in`: Dentro de un array (ej: `{ dep: { $in: ["LSI", "MAT"] } }`).


* **Proyección (elegir campos)**: `db.alumnos.find({}, { nombre: 1, _id: 0 })` (1 muestra, 0 oculta).

#### Actualización (Update)

* **Modificar campo**: `db.alumnos.updateOne({ _id: 1 }, { $set: { nombre: "Nuevo" } })`
* **Incrementar valor**: `db.alumnos.updateOne({ _id: 1 }, { $inc: { edad: 1 } })`
* **Añadir a un Array**: `db.alumnos.updateOne({ _id: 1 }, { $push: { matriculas: { asig: "BD" } } })`

#### Agregación (Consultas Avanzadas)

* **`$match`**: Filtrar (como el WHERE).
* **`$group`**: Agrupar (como GROUP BY). Usar con acumuladores: `{ $avg: "$nota" }`, `{ $sum: 1 }`.
* **`$unwind`**: "Abrir" un array para procesar sus elementos uno a uno.
* **`$lookup`**: Unir colecciones (como el JOIN).

### Documentación Oficial

Para profundizar o resolver dudas técnicas específicas, la documentación de MongoDB es una de las mejores en el mundo del software:

**[Manual de MongoDB (Documentación Oficial)](https://www.mongodb.com/docs/manual/)**

Te recomiendo especialmente estas secciones para los alumnos:

* [Operaciones CRUD](https://www.mongodb.com/docs/manual/crud/): Para ejemplos de lectura y escritura.
* [Aggregation Framework](https://www.mongodb.com/docs/manual/aggregation/): Para entender el pipeline de datos.
* [Operadores de consulta](https://www.mongodb.com/docs/manual/reference/operator/query/): Un listado completo de todos los `$gt`, `$in`, etc.


\pagebreak


## Ejercicio Práctico: Sistema de Gestión de Escuela de Ingeniería

**Contexto**: Vamos a implementar el backend de datos para una escuela. Necesitamos gestionar **Profesores**, **Alumnos** y sus **Matrículas**.

### Fase 1: Seguridad y Conexión

1. Inicia el shell de MongoDB (`mongosh`) como administrador utilizando el usuario `root` y la clave `Secreto_123`.
2. Crea una base de datos llamada `escuela_ingenieria`.
3. Crea un usuario específico para esta base de datos llamado `gestor_escuela` con el rol `dbAdmin` y `readWrite`.


### Fase 2: Modelado e Inserción

Crea las siguientes colecciones siguiendo las instrucciones de diseño documental:

#### 1. Colección `profesores`

Inserta al menos dos profesores.

* **Profesor A**: `nombre: "Miguel Cañas"`, `departamento: "Arquitectura de Computadores"`, `despacho: 102`.
* **Profesor B**: `nombre: "Isabel Ruiz"`, `departamento: "LSI"`, `despacho: 205`.

#### 2. Colección `alumnos`

Inserta un alumno con el siguiente modelo híbrido:

* **Datos básicos**: Nombre y expediente.
* **Documento Embebido**: `contacto` (email y ciudad).
* **Array de Referencias con Snapshot**: El campo `inscripciones` debe contener el `id_profesor` (referencia) y el `nombre_departamento` (snapshot/foto en ese momento).

**Script de ayuda**:

```javascript
db.alumnos.insertOne({
  nombre: "Juan Pérez",
  expediente: "JP2026",
  contacto: { email: "juan@correo.es", ciudad: "Jaén" },
  inscripciones: [
    { 
      profesor_id: ObjectId("ID_DE_MIGUEL"), // Usa el ID real generado arriba
      departamento_snapshot: "Arquitectura de Computadores",
      fecha: new Date()
    }
  ]
})

```

### Fase 3: Manipulación (CRUD)

Realiza las siguientes operaciones:

1. **Actualización**: Cambia el despacho del profesor "Miguel Cañas" al `108` usando `$set`.
2. **Inserción en Array**: Añade una nueva inscripción al alumno "Juan Pérez" con la profesora "Isabel Ruiz" usando `$push`.
3. **Consulta de Filtro**: Busca todos los alumnos cuya ciudad en el documento embebido `contacto` sea "Jaén".


### Fase 4: Desafío de Agregación (Reporting)

Utiliza el **Aggregation Framework** para generar un informe:

1. **Tarea**: Muestra el nombre del alumno y, mediante un `$lookup`, une la información de sus profesores para mostrar el nombre del profesor y su departamento actual.
2. **Pregunta de reflexión**: Si el profesor Miguel cambia de departamento hoy, ¿qué verás en el campo `departamento_snapshot` de la inscripción del alumno y qué verás en el documento unido mediante `$lookup`?



\pagebreak