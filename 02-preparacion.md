## Preparación del entorno y dependencias

En este apartado se prepara un entorno reproducible basado en **Dev Containers** para minimizar el tiempo de arranque y evitar problemas de configuración entre equipos. El objetivo es disponer, desde el primer momento, de:

* Un contenedor de desarrollo con **Java + Maven** para el backend.
* Un servicio **MongoDB** como base de datos documental.
* Un servicio **Mongo Express** (interfaz web) para inspección y administración.
* Un archivo **`.env` en la raíz del proyecto** con variables compartidas tanto por Docker Compose como por Spring Boot.
* Un proyecto **Spring Boot (Maven)** con los *starters* necesarios y configuración en **YAML**.

### Requisitos previos

* Docker y Docker Compose instalados en el equipo.
* Visual Studio Code con la extensión **Dev Containers**.
* Git (recomendado) para versionar el proyecto.

### Estructura de carpetas recomendada

Se propone la siguiente estructura mínima:

```
reservas-pistas/
  .devcontainer/
    devcontainer.json
    docker-compose.yml
  src/
  pom.xml
  .env
  application.yml (en src/main/resources)
```

La carpeta `.devcontainer/` contiene toda la configuración necesaria para levantar el entorno completo (IDE + servicios) con un único comando desde VS Code.

### Creación del archivo `.env` (variables compartidas)

En la raíz del proyecto se crea un archivo `.env`. Este archivo centraliza configuración sensible y de entorno (usuario, password, nombre de base de datos, puertos).

Ejemplo recomendado:

```env
# MongoDB (servicio Docker)
MONGO_PORT=27017
MONGO_INITDB_ROOT_USERNAME=root
MONGO_INITDB_ROOT_PASSWORD=83uddjfp0cmMD
MONGO_DB=reservas_db

# Mongo Express
MONGO_EXPRESS_PORT=8081
ME_CONFIG_BASICAUTH_USERNAME=mongo
ME_CONFIG_BASICAUTH_PASSWORD=83uddjfp0cmMD
```

Buenas prácticas:

* En un entorno real, este archivo no debería subirse al repositorio (usar `.gitignore`).
* En contexto educativo, puede incluirse si se pretende homogeneidad, pero conviene explicitarlo.


### Preparación del Dev Container

La idea es que VS Code se conecte a un contenedor de desarrollo (“workspace”) y, a la vez, levante los servicios auxiliares (MongoDB y Mongo Express) mediante Docker Compose.

#### `.devcontainer/docker-compose.yml`

Se define el contenedor de desarrollo y los servicios. Este archivo usa variables del `.env`.

```yaml
version: "3.8"

services:
  app:
    image: mcr.microsoft.com/devcontainers/java:21
    volumes:
      - ..:/workspace:cached
    working_dir: /workspace
    command: sleep infinity
    depends_on:
      - mongo
      - mongo-express
    environment:
      # Variables disponibles también dentro del contenedor de desarrollo
      MONGO_INITDB_ROOT_USERNAME: ${MONGO_INITDB_ROOT_USERNAME}
      MONGO_INITDB_ROOT_PASSWORD: ${MONGO_INITDB_ROOT_PASSWORD}
      MONGO_DB: ${MONGO_DB}

  mongo:
    image: mongo:7
    restart: "no"
    environment:
      MONGO_INITDB_ROOT_USERNAME: ${MONGO_INITDB_ROOT_USERNAME}
      MONGO_INITDB_ROOT_PASSWORD: ${MONGO_INITDB_ROOT_PASSWORD}
    ports:
      - "${MONGO_PORT}:27017"
    volumes:
      - mongo_data:/data/db

  mongo-express:
    image: mongo-express:1
    restart: "no"
    depends_on:
      - mongo
    ports:
      - "${MONGO_EXPRESS_PORT}:8081"
    environment:
      ME_CONFIG_BASICAUTH_USERNAME: ${ME_CONFIG_BASICAUTH_USERNAME}
      ME_CONFIG_BASICAUTH_PASSWORD: ${ME_CONFIG_BASICAUTH_PASSWORD}
      ME_CONFIG_MONGODB_ADMINUSERNAME: ${MONGO_INITDB_ROOT_USERNAME}
      ME_CONFIG_MONGODB_ADMINPASSWORD: ${MONGO_INITDB_ROOT_PASSWORD}
      ME_CONFIG_MONGODB_URL: mongodb://${MONGO_INITDB_ROOT_USERNAME}:${MONGO_INITDB_ROOT_PASSWORD}@mongo:27017/

volumes:
  mongo_data:
```

Notas técnicas:

* Se fija MongoDB 7 por estabilidad y compatibilidad.
* Se añade volumen `mongo_data` para persistencia entre reinicios.
* El contenedor `app` no ejecuta la aplicación; simplemente proporciona el entorno de desarrollo.

#### `.devcontainer/devcontainer.json`

Configura cómo VS Code se conecta al contenedor.

```json
{
  "name": "reservas-pistas-devcontainer",
  "dockerComposeFile": "docker-compose.yml",
  "service": "app",
  "workspaceFolder": "/workspace",
  "shutdownAction": "stopCompose",
  "customizations": {
    "vscode": {
      "extensions": [
        "vscjava.vscode-java-pack",
        "vmware.vscode-spring-boot",
        "redhat.vscode-yaml"
      ]
    }
  }
}
```

Con esto, al abrir el proyecto en VS Code:

1. Se elige “Reopen in Container”.
2. Se construye el entorno y se levantan los servicios.
3. MongoDB queda disponible desde el contenedor `app` por hostname `mongo:27017`.


### Creación del proyecto Spring Boot (Maven)

El backend se implementará como un proyecto Maven estándar con Spring Boot.

#### Opción recomendada: Spring Initializr

1. Acceder a Spring Initializr (web) o usar integración en IDE.
2. Configurar:

   * Project: **Maven**
   * Language: **Java**
   * Spring Boot: versión estable
   * Group: `com.dam`
   * Artifact: `reservas`
   * Packaging: `jar`
   * Java: 21 (o 17 si el centro lo requiere)
3. Añadir dependencias (*starters*):

   * **Spring Web**
   * **Spring Data MongoDB**
   * **Validation**

Generar, descargar y descomprimir dentro del repositorio `reservas-pistas/`.

#### Alternativa: crear Maven y añadir starters manualmente

Si se parte de un Maven “vacío”:

1. Crear proyecto Maven:

   ```bash
   mvn archetype:generate \
     -DgroupId=com.dam \
     -DartifactId=reservas \
     -DarchetypeArtifactId=maven-archetype-quickstart \
     -DinteractiveMode=false
   ```

2. Sustituir el `pom.xml` por uno basado en Spring Boot, incluyendo:

   * Parent `spring-boot-starter-parent`
   * Dependencias de `web`, `data-mongodb`, `validation`

En entornos docentes suele ser mejor usar Spring Initializr para evitar ruido.

### Dependencias (starters) necesarias

En el `pom.xml` deben estar al menos:

* `spring-boot-starter-web`: API REST (controladores, JSON, HTTP).
* `spring-boot-starter-data-mongodb`: persistencia MongoDB.
* `spring-boot-starter-validation`: validación de DTOs con `@Valid`.

Ejemplo mínimo:

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>

  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
  </dependency>

  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
  </dependency>
</dependencies>
```


### Configuración en YAML (`application.yml`) usando variables de entorno

En Spring Boot se usará `application.yml` en `src/main/resources/`.

La idea es **leer las variables del `.env`**. En Docker Compose, el `.env` se carga automáticamente para los servicios. Para Spring, hay dos estrategias:

* Ejecutar Spring desde el contenedor `app` y pasarle variables de entorno (recomendado).
* Cargar `.env` con una librería adicional (opcional, no necesario si se usa Compose).

#### `src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  data:
    mongodb:
      uri: mongodb://${MONGO_INITDB_ROOT_USERNAME}:${MONGO_INITDB_ROOT_PASSWORD}@mongo:27017/${MONGO_DB}?authSource=admin

logging:
  level:
    org.springframework.web: INFO
```

Notas:

* `mongo` es el hostname del servicio dentro de Docker Compose.
* `authSource=admin` es necesario cuando se autentica con usuario root.
* La base de datos que usará la aplicación será `${MONGO_DB}`.

### Verificación del entorno

Una vez abierto el proyecto en Dev Container:

1. Comprobar que Mongo está levantado:

   ```bash
   docker ps
   ```

2. Acceder a Mongo Express en el host:

   * `http://localhost:8081`
   * Usuario/contraseña definidos en `.env`

3. Probar conexión desde el contenedor de desarrollo:

   ```bash
   mongosh "mongodb://root:83uddjfp0cmMD@mongo:27017/?authSource=admin"
   ```


### Nota sobre “mongoengine”

MongoEngine es un ODM típico de Python (Flask/Django), no forma parte del stack Java/Spring. En Spring Boot, el equivalente conceptual es:

* **Spring Data MongoDB** (repositorios, mapeo de documentos, consultas derivadas)
* Alternativamente `MongoTemplate` para consultas más personalizadas

En este capítulo se utilizará **Spring Data MongoDB** por su alineación con el enfoque REST y por simplificar el CRUD y las consultas.


### Archivo `.env` de ejemplo

El archivo `.env`, situado en la raíz del proyecto, centraliza las variables de entorno utilizadas tanto por Docker Compose como por Spring Boot. De este modo se evita la duplicación de configuración y se facilita la modificación del entorno sin tocar el código.

Ejemplo completo de `.env`:

```env
# ==========================
# MongoDB
# ==========================
MONGO_PORT=27017
MONGO_INITDB_ROOT_USERNAME=root
MONGO_INITDB_ROOT_PASSWORD=83uddjfp0cmMD
MONGO_DB=reservas_db

# ==========================
# Mongo Express
# ==========================
MONGO_EXPRESS_PORT=8081
ME_CONFIG_BASICAUTH_USERNAME=mongo
ME_CONFIG_BASICAUTH_PASSWORD=83uddjfp0cmMD
```

Notas importantes:

* En un entorno profesional, este archivo **no debe versionarse**.
* En el contexto educativo puede compartirse para facilitar el arranque, pero debe explicarse el motivo y las implicaciones.
* Todas las variables definidas aquí están disponibles para los contenedores definidos en Docker Compose.



### Archivo `.gitignore` recomendado

Para evitar subir al repositorio archivos generados automáticamente o que contengan información sensible, se recomienda incluir al menos las siguientes entradas en el archivo `.gitignore`:

```gitignore
# Variables de entorno
.env

# Directorios generados por Maven
target/

# Archivos de configuración del IDE
.idea/
*.iml
.vscode/

# Logs
*.log
```

Este `.gitignore` evita:

* La publicación accidental de credenciales.
* Conflictos entre distintos entornos de desarrollo.
* La inclusión de binarios o artefactos generados en tiempo de compilación.


### Arranque de la aplicación Spring Boot desde el contenedor

Una vez que el proyecto se ha abierto en el **Dev Container**, el arranque de la aplicación se realiza desde el propio contenedor de desarrollo.

Desde el terminal integrado de VS Code:

```bash
./mvnw spring-boot:run
```

Este comando:

* Utiliza el *Maven Wrapper* incluido en el proyecto.
* Arranca Spring Boot usando la configuración definida en `application.yml`.
* Conecta automáticamente con MongoDB usando las variables de entorno proporcionadas por Docker Compose.

Si todo es correcto, en la salida del log se podrá observar:

* Inicialización del contexto de Spring.
* Conexión correcta a MongoDB.
* Puerto HTTP del servidor web en estado “listening”.


### Cheatsheet para troubleshooting 

Ante problemas de arranque o conexión, conviene revisar de forma sistemática los siguientes puntos:

#### MongoDB no arranca o no responde

* Verificar que los contenedores están en ejecución:

  ```bash
  docker ps
  ```
* Comprobar que el puerto definido en `.env` (`MONGO_PORT`) no está siendo usado por otro servicio.
* Revisar los logs del contenedor:

  ```bash
  docker logs <id_contenedor_mongo>
  ```

#### Error de autenticación en MongoDB

* Confirmar que `MONGO_INITDB_ROOT_USERNAME` y `MONGO_INITDB_ROOT_PASSWORD` coinciden en:

  * `.env`
  * `docker-compose.yml`
  * `application.yml`
* Asegurarse de que la URI de conexión incluye:

  ```
  authSource=admin
  ```

#### Spring Boot no conecta con MongoDB

* Verificar que en `application.yml` se usa el hostname `mongo` y no `localhost`.
* Comprobar que Spring se está ejecutando **dentro del contenedor**, no en el host.
* Probar conexión manual desde el contenedor:

  ```bash
  mongosh "mongodb://root:password@mongo:27017/?authSource=admin"
  ```

#### Mongo Express no es accesible

* Confirmar que el puerto `MONGO_EXPRESS_PORT` está correctamente expuesto.
* Acceder desde el navegador a:

  ```
  http://localhost:8081
  ```
* Revisar credenciales de acceso web (basic auth).

#### Cambios en `.env` no surten efecto

* Recordar que Docker Compose **no recarga automáticamente** variables de entorno.
* Detener y volver a levantar los servicios:

  ```bash
  docker-compose down
  docker-compose up -d
  ```

\pagebreak
