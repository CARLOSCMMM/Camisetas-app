## CORS y configuración por entornos

En una arquitectura cliente–servidor donde el frontend (HTML/JS) consume una API REST, es habitual que el navegador aplique la política de seguridad **Same-Origin Policy**. Esta política bloquea, por defecto, peticiones AJAX a un origen distinto (dominio, puerto o protocolo diferentes). Para permitir el acceso controlado a la API se utiliza **CORS (Cross-Origin Resource Sharing)**.

Además, en proyectos reales se requiere distinguir configuraciones según el entorno:

* **Desarrollo** (Dev Container, puertos locales, logs más verbosos).
* **Producción** (orígenes permitidos restringidos, credenciales seguras, logs moderados).

En este apartado se configura:

1. CORS para permitir el consumo desde el frontend.
2. Configuración por entornos con perfiles (`dev`, `prod`) manteniendo el uso de `.env`.


### 1) Conceptos básicos de CORS

CORS es un mecanismo basado en cabeceras HTTP que permite a un servidor indicar qué orígenes están autorizados a realizar solicitudes.

Elementos habituales:

* `Access-Control-Allow-Origin`: origen permitido.
* `Access-Control-Allow-Methods`: métodos permitidos (GET, POST, etc.).
* `Access-Control-Allow-Headers`: cabeceras permitidas.
* Petición **preflight** (OPTIONS): el navegador la envía antes de algunas solicitudes para comprobar permisos.

En desarrollo, si el frontend se abre, por ejemplo, desde:

* `http://localhost:5500` (Live Server)
  y la API está en:
* `http://localhost:8080`

son orígenes distintos por el puerto y el navegador bloqueará la llamada si no hay CORS configurado.


### 2) Estrategias posibles en este proyecto

En este capítulo se trabajará con dos escenarios comunes:

* **Escenario A (más simple)**: el frontend se sirve desde Spring Boot (`src/main/resources/static`) y el origen es el mismo. En este caso, CORS no es estrictamente necesario, pero conviene conocerlo porque no siempre será así.

* **Escenario B (habitual en prácticas)**: el frontend se sirve desde un servidor distinto (Live Server, Nginx, etc.). Aquí sí es necesario habilitar CORS.

Se implementará una configuración que permita **activar/desactivar** el origen permitido mediante variables de entorno.


### 3) Configuración de CORS en Spring Boot

Se define una clase de configuración implementando `WebMvcConfigurer` para aplicar CORS de forma global a `/api/**`.

#### `config/CorsConfig.java`

```java
package com.dam.reservas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

  @Value("${app.cors.allowed-origins:*}")
  private String allowedOrigins;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOrigins(allowedOrigins.split(","))
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(false)
        .maxAge(3600);
  }
}
```

Notas:

* `allowedOrigins` se puede definir como una lista separada por comas.
* En desarrollo puede ser `*` (no recomendado en producción).
* `allowCredentials(false)` evita combinaciones inseguras con `*`.

### 4) Configuración por entornos con perfiles

Spring Boot permite definir distintos ficheros YAML por perfil:

* `application.yml` (común)
* `application-dev.yml`
* `application-prod.yml`

La idea es mantener en `application.yml` lo esencial y delegar ajustes por perfil (CORS, logs, etc.) a los YAML específicos.

#### 4.1 `application.yml` (base)

Ubicación: `src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

  data:
    mongodb:
      uri: mongodb://${MONGO_INITDB_ROOT_USERNAME}:${MONGO_INITDB_ROOT_PASSWORD}@mongo:27017/${MONGO_DB}?authSource=admin

app:
  cors:
    allowed-origins: ${APP_CORS_ALLOWED_ORIGINS:*}
```

* Se activa por defecto el perfil `dev` si no se define `SPRING_PROFILES_ACTIVE`.
* La URI de MongoDB se construye con variables del `.env` y el hostname `mongo` (servicio Compose).



#### 4.2 `application-dev.yml` (desarrollo)

```yaml
logging:
  level:
    org.springframework.web: INFO
    org.springframework.data.mongodb: INFO

app:
  cors:
    allowed-origins: ${APP_CORS_ALLOWED_ORIGINS:*}
```

En desarrollo se suelen permitir orígenes más amplios y logs más verbosos.



#### 4.3 `application-prod.yml` (producción)

```yaml
logging:
  level:
    org.springframework.web: WARN
    org.springframework.data.mongodb: WARN

app:
  cors:
    allowed-origins: ${APP_CORS_ALLOWED_ORIGINS:https://mi-dominio-ejemplo}
```

En producción:

* Se restringen orígenes permitidos a dominios concretos.
* Se moderan los logs.



### 5) Variables de entorno necesarias

En el `.env` se pueden definir variables adicionales para controlar CORS y perfil:

```env
SPRING_PROFILES_ACTIVE=dev
APP_CORS_ALLOWED_ORIGINS=http://localhost:5500,http://localhost:8080
```

En desarrollo, si se usa Live Server, se añade su origen:

* `http://localhost:5500` (puede variar según configuración)

Si el frontend se sirve desde Spring (`/static`), bastaría con `http://localhost:8080`.



### 6) Verificación de CORS

Para comprobar CORS, se puede:

* Abrir la consola del navegador y verificar que no hay errores del tipo:

  * “blocked by CORS policy”
* Probar con `curl` simulando un origen:

```bash
curl -i http://localhost:8080/api/instalaciones \
  -H "Origin: http://localhost:5500"
```

Debe observarse en la respuesta una cabecera `Access-Control-Allow-Origin` adecuada.


### Resultado de este apartado

Tras este apartado el proyecto queda con:

* CORS configurado globalmente para `/api/**`.
* Control del origen permitido mediante variables de entorno (`APP_CORS_ALLOWED_ORIGINS`).
* Separación por entornos con perfiles (`dev` / `prod`) y YAML específico.
* Preparación para servir frontend desde un origen distinto sin problemas de navegador.

En el siguiente apartado se abordará: **“Front con Bootstrap: estructura y componentes”**, donde se definirá la estructura del cliente web y la interfaz para gestionar instalaciones, usuarios y reservas.

\pagebreak
