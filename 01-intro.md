## Introducción: REST + MongoDB + SPA ligera

En este capítulo se aborda el desarrollo de una aplicación web completa basada en una **arquitectura cliente–servidor**, utilizando tecnologías ampliamente empleadas en el desarrollo de aplicaciones modernas. El objetivo es integrar un **backend REST** desarrollado con Spring Boot y MongoDB con un **frontend ligero** construido con HTML5, Bootstrap y JavaScript, siguiendo el modelo de **Single Page Application (SPA) simple**, sin necesidad de herramientas de construcción complejas.

### Objetivo general del capítulo

El objetivo principal es que el alumnado sea capaz de **diseñar, implementar, probar y documentar** una aplicación que:

* Exponga una **API REST** para la gestión de datos.
* Utilice una **base de datos orientada a documentos (MongoDB)** para almacenar la información.
* Consuma dicha API desde un cliente web mediante **peticiones HTTP asíncronas**.
* Aplique buenas prácticas básicas de diseño, validación y manejo de errores.

Este enfoque permite evaluar de forma directa y práctica los criterios de evaluación relacionados con el acceso a datos y el desarrollo de aplicaciones multicapa.

### Arquitectura de la aplicación

La aplicación se estructura en tres capas claramente diferenciadas:

* **Cliente (frontend)**
  Implementado con HTML5, Bootstrap y JavaScript (jQuery). Se encarga de la interacción con el usuario y del consumo de la API REST mediante peticiones AJAX.

* **Servidor (backend)**
  Desarrollado con Spring Boot, expone una API REST que gestiona la lógica de negocio y el acceso a la base de datos. Se apoya en Spring Data MongoDB para la persistencia de la información.

* **Base de datos**
  MongoDB almacena la información en forma de documentos BSON, permitiendo un modelo flexible que se adapta bien a los requisitos del dominio de reservas.

La comunicación entre cliente y servidor se realiza exclusivamente mediante **JSON sobre HTTP**, lo que desacopla ambas capas y facilita su mantenimiento y evolución.

### Por qué REST, MongoDB y una SPA ligera

La combinación de estas tecnologías no es casual y responde a criterios técnicos y pedagógicos:

* **REST** permite definir una interfaz clara, estándar y fácilmente comprobable para el acceso a los datos, favoreciendo la separación de responsabilidades.
* **MongoDB**, como base de datos orientada a documentos, facilita el trabajo con estructuras de datos complejas y jerárquicas, alineándose bien con el modelo de reservas planteado.
* Una **SPA ligera**, sin frameworks pesados ni procesos de compilación, permite centrarse en los conceptos fundamentales del consumo de APIs y la gestión del estado en el cliente, reduciendo la carga cognitiva inicial.

Este enfoque resulta especialmente adecuado para comprender las diferencias entre bases de datos relacionales y documentales, así como para introducir patrones de desarrollo habituales en aplicaciones web actuales.

### Caso práctico: gestión de reservas de pistas deportivas

A lo largo del capítulo se desarrollará una aplicación que permite:

* Gestionar **instalaciones deportivas**.
* Definir **horarios** asociados a dichas instalaciones, almacenando un “snapshot” de la información relevante.
* Registrar **usuarios**.
* Crear, consultar, modificar y cancelar **reservas**, combinando información embebida (horario e instalación) con referencias a otros documentos (usuario).

Para entender mejor la aplicación veamos el siguiente caso de uso:

![Diagrama de caso de uso de gestión de pistas deportivas](docs/diagrama%20de%20casos%20de%20uso.png)

El diagrama UML que seguiremos es este:

![Diagrama UML de Gestión de Pistas Deportivas](docs/diagrama%20de%20clases.png)

Este modelo ha sido elegido porque:

* Presenta **objetos simples y estructurados**, ideales para trabajar la persistencia en MongoDB.
* Requiere **consultas filtradas** (por fecha, usuario o instalación).
* Obliga a gestionar la **consistencia de los datos**, especialmente al evitar solapamientos de reservas, lo que introduce el concepto de transacción o control lógico de integridad.


\pagebreak
