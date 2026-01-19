## MongoDB: documentos, colecciones y ObjectId

MongoDB es una base de datos **orientada a documentos**, lo que implica un modelo de almacenamiento y acceso a los datos diferente al de las bases de datos relacionales tradicionales. En este apartado se introducen los conceptos fundamentales necesarios para comprender cómo se almacenan y gestionan los datos en MongoDB y cómo estos conceptos se aplican al sistema de **reservas de pistas deportivas** que se desarrollará a lo largo del capítulo.

### Documentos

El **documento** es la unidad básica de almacenamiento en MongoDB. Un documento es una estructura de datos en formato **BSON** (Binary JSON), similar a un objeto JSON, que permite:

* Pares clave–valor.
* Tipos de datos simples (string, number, boolean, date).
* Estructuras anidadas (objetos dentro de objetos).
* Arrays de valores u objetos.

A diferencia de una fila en una tabla relacional, un documento puede contener información jerárquica y no requiere un esquema rígido previamente definido.

Ejemplo de documento sencillo:

```json
{
  "_id": ObjectId("65a1c9a1f1c2e0a9a0b12345"),
  "nombre": "Pista Central",
  "direccion": "C/ Mayor 1",
  "ciudad": "Jaén"
}
```

En este ejemplo, todos los datos relacionados con una instalación deportiva se encuentran agrupados en un único documento.


### Colecciones

Una **colección** es un conjunto de documentos del mismo tipo lógico. Conceptualmente, puede compararse con una tabla en una base de datos relacional, aunque con diferencias importantes:

* Los documentos de una colección **no están obligados a tener la misma estructura exacta**.
* No es necesario crear la colección previamente; MongoDB la crea automáticamente al insertar el primer documento.
* No existen claves foráneas ni restricciones de integridad a nivel de base de datos.

Ejemplos de colecciones en el sistema de reservas:

* `instalaciones`
* `usuarios`
* `reservas`

Cada una de estas colecciones almacena documentos relacionados con una entidad del dominio de la aplicación.


### ObjectId

Cada documento en MongoDB tiene un identificador único denominado `_id`. Si no se especifica, MongoDB genera automáticamente un **ObjectId**.

Un `ObjectId` es un valor de 12 bytes que incluye:

* Marca temporal de creación.
* Identificador del proceso.
* Contador incremental.

Esto garantiza unicidad sin necesidad de coordinación central.

Ejemplo de `_id`:

```json
"_id": ObjectId("65a1c9a1f1c2e0a9a0b12345")
```

En aplicaciones Java con Spring Data MongoDB, el `ObjectId` suele mapearse a un campo de tipo `String` o `ObjectId`, siendo habitual el uso de `String` para simplificar la serialización y el trabajo con JSON.


### Documentos embebidos y referencias

Uno de los aspectos más importantes del diseño en MongoDB es decidir **qué datos se embeben** dentro de un documento y **qué datos se referencian** mediante identificadores.

#### Documentos embebidos

Un documento embebido es un objeto que se almacena dentro de otro documento. Este enfoque es adecuado cuando:

* Los datos se consultan siempre junto al documento principal.
* No se necesita reutilizar el subdocumento de forma independiente.
* Se desea preservar un “snapshot” del estado de los datos en un momento concreto.

En el sistema de reservas, el **horario** se embebe dentro de la reserva, y a su vez contiene una copia de la instalación.

Ejemplo simplificado de documento `reserva` con documentos embebidos:

```json
{
  "_id": ObjectId("65a2aa0f9d3e9c1b8c111111"),
  "fechaReserva": "2026-01-18T10:15:00Z",
  "usuarioId": "65a199f3e0b1c2d3e4f56789",
  "horario": {
    "dia": "2026-01-20",
    "horaInicio": "18:00",
    "horaFin": "19:00",
    "instalacionSnapshot": {
      "instalacionId": "65a18888aa11223344556677",
      "nombre": "Pista Central",
      "direccion": "C/ Mayor 1",
      "ciudad": "Jaén"
    }
  }
}
```

Este diseño permite que una reserva conserve la información de la instalación tal y como era en el momento de la reserva, incluso si la instalación cambia posteriormente.


#### Referencias entre documentos

Las **referencias** consisten en almacenar el identificador (`_id`) de otro documento en lugar de embebido completo. Este enfoque se utiliza cuando:

* El documento referenciado se reutiliza en muchos contextos.
* La información debe mantenerse siempre actualizada.
* No es necesario cargarla automáticamente en cada consulta.

En este proyecto, la reserva **referencia al usuario** mediante su identificador (`usuarioId`), evitando duplicar datos personales.

Ejemplo de referencia:

```json
{
  "usuarioId": "65a199f3e0b1c2d3e4f56789"
}
```

La recuperación de los datos completos del usuario se realiza, si es necesario, mediante una consulta adicional desde la aplicación.


### Comparación con el modelo relacional

| Aspecto       | Relacional     | MongoDB (Documental)  |
| ------------- | -------------- | --------------------- |
| Unidad básica | Fila           | Documento             |
| Agrupación    | Tabla          | Colección             |
| Identificador | Clave primaria | `_id` (ObjectId)      |
| Relaciones    | JOIN           | Embebido o referencia |
| Esquema       | Rígido         | Flexible              |

Esta comparación ayuda a comprender por qué MongoDB resulta especialmente adecuado para modelos de dominio complejos y jerárquicos, como el sistema de reservas planteado.

\pagebreak