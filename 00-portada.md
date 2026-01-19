---
title: "Acceso a Datos: Spring Rest y Mongo en acción"
author: [Juan Gualberto Gutiérrez Marín]
date: "Enero 2026"
keywords: [computación, ciberseguridad, robótica, programación, algoritmo, hacking]
lang: "es"
titlepage: true,
titlepage-text-color: "FFFFFF"
titlepage-rule-color: "360049"
titlepage-rule-height: 0
titlepage-background: "docs/background.pdf"
toc: false
---


\pagebreak

> AVISO: El texto contiene muchos enlaces embebidos que sólo funcionan si se abre el PDF desde un lector digital. Si se imprime, se perderá mucha información interesante. No obstante, en el apartado "Bibliografía" se han añadido los enlaces para aquellos lectores que deseen una copia impresa y puedan ver la información. 

---

Este documento se encuentra bajo una licencia Creative Commons de Atribución-CompartirIgual (CC BY-SA). 

![Atribución-CompartirIgual (CC BY-SA)](docs/licencia.png)

Esto significa que puedes:

- Compartir: copiar y redistribuir el material en cualquier medio o formato.
- Adaptar: remezclar, transformar y construir sobre el material para cualquier propósito, incluso comercialmente.

Bajo las siguientes condiciones:

- Atribución: debes dar crédito de manera adecuada, proporcionar un enlace a la licencia e indicar si se han realizado cambios. Puedes hacerlo de cualquier manera razonable, pero no de una manera que sugiera que el licenciante te respalda a ti o al uso que hagas del trabajo.
- Compartir igual: si remezclas, transformas o creas a partir del material, debes distribuir tus contribuciones bajo la misma licencia que el original.

Para más detalles, consulta la [licencia completa](https://creativecommons.org/licenses/by-sa/4.0/legalcode.es).

---

> Para una versión actualizada de este libro visita esta Web: <https://gitlab.iesvirgendelcarmen.com/juangu>.




\pagebreak
\tableofcontents
\pagebreak

# Introducción a las bases de datos NoSQL

## Introducción a las bases de datos NoSQL

Las bases de datos **NoSQL (Not Only SQL)** surgen como respuesta a las limitaciones de las bases de datos relacionales tradicionales en determinados escenarios, especialmente aquellos relacionados con **gran volumen de datos**, **alta concurrencia**, **escalabilidad horizontal** y **modelos de datos cambiantes**.

A diferencia de los sistemas relacionales, NoSQL no impone un modelo único basado en tablas, filas y relaciones estrictas, sino que ofrece distintos enfoques de almacenamiento adaptados a diferentes necesidades.

### Características generales de las bases de datos NoSQL

Las bases de datos NoSQL comparten una serie de características comunes:

* **Esquema flexible**: los datos no necesitan ajustarse a una estructura rígida predefinida.
* **Escalabilidad horizontal**: están diseñadas para escalar añadiendo nodos, no aumentando la potencia de uno solo.
* **Alto rendimiento** en operaciones de lectura y escritura.
* **Modelo de consistencia flexible**, habitualmente orientado a consistencia eventual en lugar de transacciones ACID estrictas.
* **Orientación al dominio**: el modelo de datos suele adaptarse a cómo se consume la información.

Estas características las hacen especialmente adecuadas para aplicaciones web modernas, sistemas distribuidos y arquitecturas basadas en microservicios.

### Tipos principales de bases de datos NoSQL

Las bases de datos NoSQL se clasifican habitualmente en cuatro grandes categorías:

#### Bases de datos clave–valor

Almacenan datos como pares `clave $\rightarrow$ valor`.

Ejemplo conceptual:

```
"session_1234" $\rightarrow$ { "usuario": "ana", "rol": "admin" }
```

Uso típico:

* Cachés
* Sesiones de usuario
* Configuración temporal

Ventajas:

* Simplicidad
* Velocidad extrema

Inconvenientes:

* Consultas muy limitadas
* No aptas para datos complejos

#### Bases de datos orientadas a columnas

Organizan los datos por columnas en lugar de por filas.

Ejemplo conceptual:

```
usuario:1:nombre $\rightarrow$ "Luis"
usuario:1:email  $\rightarrow$ "luis@email.com"
```

Uso típico:

* Analítica
* Big Data
* Series temporales

Ventajas:

* Excelente rendimiento en grandes volúmenes
* Compresión eficiente

Inconvenientes:

* Curva de aprendizaje
* No pensadas para CRUD clásico

#### Bases de datos orientadas a grafos

Modelan los datos como **nodos** y **relaciones**.

Ejemplo conceptual:

```
(Usuario)-[RESERVA]->(Pista)
```

Uso típico:

* Redes sociales
* Sistemas de recomendación
* Análisis de relaciones complejas

Ventajas:

* Consultas relacionales muy potentes
* Modelo muy expresivo

Inconvenientes:

* No adecuadas para consultas simples masivas
* Modelo menos intuitivo para CRUD básicos

#### Bases de datos orientadas a documentos

Almacenan la información en forma de **documentos estructurados**, normalmente en formato JSON o BSON.

Ejemplo conceptual:

```json
{
  "nombre": "Pista Central",
  "ciudad": "Jaén",
  "tipo": "Tenis"
}
```

Este tipo de base de datos es el que se utilizará en este capítulo.

### Ventajas e inconvenientes de NoSQL frente a bases de datos relacionales

**Ventajas**

* Mayor flexibilidad del modelo de datos.
* Menor acoplamiento entre aplicación y estructura de almacenamiento.
* Escalabilidad sencilla.
* Mejor adaptación a dominios complejos.

**Inconvenientes**

* Menor estandarización.
* Ausencia de JOINs clásicos.
* Transacciones limitadas o no disponibles según el motor.
* Mayor responsabilidad en la lógica de aplicación.

Estas diferencias hacen que la elección entre SQL y NoSQL dependa del **caso de uso**, no de una superioridad técnica general.

\pagebreak 

## MongoDB

MongoDB es un ejemplo de base de datos documental. Las **bases de datos documentales** almacenan la información en forma de documentos completos y autocontenidos. Cada documento representa una entidad del dominio y puede incluir estructuras anidadas, listas y subdocumentos.

Este enfoque se alinea de forma natural con los objetos utilizados en los lenguajes de programación orientados a objetos y facilita el desarrollo de aplicaciones backend modernas.

### Conceptos fundamentales

#### Documento

Unidad básica de almacenamiento. Equivale conceptualmente a una fila en una base de datos relacional, pero con estructura jerárquica.

Ejemplo:

```json
{
  "_id": "64f8a2...",
  "nombre": "Instalación Norte",
  "ciudad": "Jaén"
}
```

#### Colección

Conjunto de documentos del mismo tipo lógico. Equivale a una tabla.

Ejemplo:

* `usuarios`
* `instalaciones`
* `reservas`

#### ObjectId

Identificador único generado automáticamente por MongoDB. Incluye información temporal y garantiza unicidad sin coordinación central.

### Instalación y configuración de MongoDB

Para crear la infraestructura de servicios necesaria, en lugar de instalar MongoDB directamente en el sistema operativo, y siguiendo la metodología habitual del módulo, se utilizarán **contenedores Docker**. En concreto, se desplegarán dos servicios:

* Un contenedor para el **servidor MongoDB**, que actuará como base de datos documental.
* Un contenedor para **Mongo Express**, una interfaz web que permite inspeccionar y administrar MongoDB de forma visual.

Este enfoque presenta varias ventajas: homogeneiza el entorno de trabajo, reduce el tiempo de configuración inicial y evita dependencias con el sistema anfitrión.

Para ello, se crea una carpeta llamada `stack` y, dentro de ella, el archivo `docker-compose.yml` con el siguiente contenido:

```yaml
version: '3.1'

services:

  mongo:
    image: mongo
    restart: 'no'
    environment:
      MONGO_INITDB_ROOT_USERNAME: root
      MONGO_INITDB_ROOT_PASSWORD: 83uddjfp0cmMD
    ports:
      - 27017:27017

  mongo-express:
    image: mongo-express
    restart: 'no'
    ports:
      - 8081:8081
    environment:
      ME_CONFIG_BASICAUTH_USERNAME: mongo
      ME_CONFIG_BASICAUTH_PASSWORD: 83uddjfp0cmMD
      ME_CONFIG_MONGODB_ADMINUSERNAME: root
      ME_CONFIG_MONGODB_ADMINPASSWORD: 83uddjfp0cmMD
      ME_CONFIG_MONGODB_URL: mongodb://root:83uddjfp0cmMD@mongo:27017/
```

#### `docker-compose.yml`

El archivo `docker-compose.yml` permite definir y ejecutar múltiples servicios Docker de forma coordinada. A continuación se analizan sus principales secciones.

```yaml
version: '3.1'
```

Esta línea indica la versión del esquema de Docker Compose utilizada. La versión 3.1 es compatible con la mayoría de instalaciones actuales de Docker.

##### Servicio `mongo`

```yaml
mongo:
  image: mongo
  restart: 'no'
  environment:
    MONGO_INITDB_ROOT_USERNAME: root
    MONGO_INITDB_ROOT_PASSWORD: 83uddjfp0cmMD
  ports:
    - 27017:27017
```

Este servicio define una instancia de MongoDB basada en la imagen oficial:

* **image**: se utiliza la imagen oficial `mongo` desde Docker Hub.
* **restart: 'no'**: el contenedor no se reiniciará automáticamente si se detiene.
* **environment**:

  * Se define un usuario administrador (`root`) y su contraseña.
  * Estas credenciales se utilizan para acceder a la base de datos.
* **ports**:

  * Se expone el puerto estándar de MongoDB (27017), permitiendo la conexión desde el host y desde otros contenedores.


#### Servicio `mongo-express`

```yaml
mongo-express:
  image: mongo-express
  restart: 'no'
  ports:
    - 8081:8081
  environment:
    ME_CONFIG_BASICAUTH_USERNAME: mongo
    ME_CONFIG_BASICAUTH_PASSWORD: 83uddjfp0cmMD
    ME_CONFIG_MONGODB_ADMINUSERNAME: root
    ME_CONFIG_MONGODB_ADMINPASSWORD: 83uddjfp0cmMD
    ME_CONFIG_MONGODB_URL: mongodb://root:83uddjfp0cmMD@mongo:27017/
```

Este servicio despliega Mongo Express, una interfaz web de administración:

* **ports**:

  * Se expone el puerto 8081, que permite acceder a la interfaz desde el navegador.
* **ME_CONFIG_BASICAUTH_USERNAME / PASSWORD**:

  * Definen las credenciales para acceder a la interfaz web.
* **ME_CONFIG_MONGODB_ADMINUSERNAME / PASSWORD**:

  * Son las credenciales con las que Mongo Express se autentica contra MongoDB.
* **ME_CONFIG_MONGODB_URL**:

  * Cadena de conexión a MongoDB, donde se especifica el usuario, contraseña y nombre del servicio (`mongo`).

Es importante diferenciar claramente entre:

* **MongoDB**: motor de base de datos documental.
* **Mongo Express**: herramienta web de administración.

Aunque este entorno es de desarrollo, **siempre se deben usar contraseñas robustas**, ya que una exposición accidental del servicio podría comprometer los datos almacenados.

#### Puesta en marcha de los servicios

Una vez creado el archivo `docker-compose.yml`, y teniendo Docker y Docker Compose instalados, se ejecuta el siguiente comando desde la carpeta `stack`:

```bash
docker-compose up -d
```

Este comando descarga las imágenes necesarias y lanza los contenedores en segundo plano. Tras unos segundos:

* MongoDB estará disponible en el puerto **27017**.
* Mongo Express será accesible desde el navegador en `http://localhost:8081`.

### Conexión interactiva a MongoDB

Aunque en las aplicaciones reales el acceso a MongoDB se realiza desde el backend, resulta muy útil conocer el uso del **shell interactivo** para pruebas, aprendizaje y depuración.

MongoDB utiliza actualmente **mongosh**, que sustituye al antiguo `mongo`.

![Ejemplo de conexión en modo interactivo.](docs/mongodbinteractivo.png)

#### Acceso al shell de MongoDB

1. **Abrir una terminal dentro del contenedor MongoDB**:

   ```bash
   docker exec -ti stack-mongo_mongo_1 /bin/bash
   ```

2. **Iniciar el shell interactivo**:

   ```bash
   mongosh -u root -p
   ```

   Se solicitará la contraseña configurada en el `docker-compose.yml`.

3. **Seleccionar una base de datos**:

   ```javascript
   use('GestionAcademica')
   ```

Si la base de datos no existe, MongoDB la creará automáticamente cuando se inserte el primer documento.

### Operaciones CRUD básicas en MongoDB

Vamos a suponer que tenemos el siguente diagrama UML:

![Diagrama UML Gestión Académica](docs/uml.png)

#### Crear un documento (Create)

Para insertar un documento en una colección llamada `alumnos`:

```javascript
db.alumnos.insertOne({
  nombre: "Juan Perez",
  edad: 20,
  carrera: "Ingeniería Informática"
})
```

MongoDB crea automáticamente la colección si no existe y asigna un identificador `_id`.

#### Leer documentos (Read)

Para recuperar todos los documentos de la colección:

```javascript
db.alumnos.find()
```

#### Actualizar un documento (Update)

Para modificar un campo concreto se utiliza el operador `$set`:

```javascript
db.alumnos.updateOne(
  { nombre: "Juan Perez" },
  { $set: { carrera: "Ingeniería Eléctrica" } }
)
```

#### Eliminar un documento (Delete)

Para eliminar un documento:

```javascript
db.alumnos.deleteOne({ nombre: "Juan Perez" })
```

#### Consultas con condiciones y arrays

Buscar profesores por nombre:

```javascript
db.profesor.find({ nombre: "Juan" }).pretty()
```

Buscar profesores con asignaturas de más de 4 horas:

```javascript
db.profesor.find({
  asignaturas: {
    $elemMatch: {
      horas: { $gt: 4 }
    }
  }
})
```

### Colección de ejemplo: `estudiantes`

Se utilizará una colección ficticia para practicar consultas:

```javascript
db.estudiantes.insertMany([
  { nombre: "Ana García", edad: 22, carrera: "Biología", semestre: 5, promedio: 8.5 },
  { nombre: "Carlos López", edad: 21, carrera: "Ingeniería Civil", semestre: 4, promedio: 7.2 },
  { nombre: "María Torres", edad: 20, carrera: "Psicología", semestre: 3, promedio: 9.0 },
  { nombre: "Juan Rodríguez", edad: 23, carrera: "Historia", semestre: 6, promedio: 7.8 },
  { nombre: "Elena Pérez", edad: 19, carrera: "Matemáticas", semestre: 2, promedio: 9.5 }
])
```

#### Ejemplos de consultas

* Obtener todos los estudiantes:

  ```javascript
  db.estudiantes.find({})
  ```

* Filtrar por carrera:

  ```javascript
  db.estudiantes.find({ carrera: "Biología" })
  ```

* Estudiantes mayores de 21 años:

  ```javascript
  db.estudiantes.find({ edad: { $gt: 21 } })
  ```

* Promedio mayor o igual a 8:

  ```javascript
  db.estudiantes.find({ promedio: { $gte: 8.0 } })
  ```

* Ordenar por promedio descendente:

  ```javascript
  db.estudiantes.find().sort({ promedio: -1 })
  ```

* Limitar resultados:

  ```javascript
  db.estudiantes.find().limit(3)
  ```

MongoDB ofrece una amplia variedad de operadores y opciones para realizar consultas avanzadas. [Se recomienda consultar la documentación oficial para profundizar en estos aspectos.](https://www.mongodb.com/docs/)


\pagebreak
