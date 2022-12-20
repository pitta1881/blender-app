## Proyecto

El presente proyecto es un Trabajo Práctico Final Integrador de Conocimientos para la materia Sistemas Distribuidos y Programación Paralela de la Universidad Nacional de Luján.

## Propuesta

La propuesta consiste en crear una red distribuida de renderizado de imágenes a partir de un archivo .blend creado en el programa Blender, otorgando así una forma de renderizar modelos y animaciones aún en aquellos computadores sin la potencia requerida para éste trabajo. Ésta red será tolerante a fallos, escalable, multithread y distribuida.

## Autores

- Ledesma Damián (Leg.: 155825)
- Pittavino Patricio Ariel (Leg.: 121476)

## Tecnologías

- [Java 17](https://www.oracle.com/java/technologies/downloads/#java17)
- [Blender](https://www.blender.org/).

### Dependencias

- [Gson](https://mvnrepository.com/artifact/com.google.code.gson/gson)
- [Logback Classic Module](https://mvnrepository.com/artifact/ch.qos.logback/logback-classic)
- [Lettuce](https://mvnrepository.com/artifact/io.lettuce/lettuce-core)
- [Apache Commons](https://mvnrepository.com/artifact/org.apache.commons/commons-lang3)
- [Zip4j](https://mvnrepository.com/artifact/net.lingala.zip4j/zip4j)
- [Dotenv](https://mvnrepository.com/artifact/io.github.cdimascio/java-dotenv)
- [Google Cloud Storage](https://mvnrepository.com/artifact/com.google.cloud/google-cloud-storage)
- [Maven Shade Plugin](https://mvnrepository.com/artifact/org.apache.maven.plugins/maven-shade-plugin)

### Desarrollo

- [Git](https://git-scm.com/)
- [IntelliJ IDEA](https://www.jetbrains.com/idea/)
- [Database Client Extension for VS Code](https://marketplace.visualstudio.com/items?itemName=cweijan.vscode-database-client2)
- [Putty](https://www.putty.org/)
- [WinSCP](https://winscp.net/eng/docs/lang:es)

### Servicios en la Nube

- [Google Cloud Storage](https://cloud.google.com/storage?hl=es-419)
- [Google Compute Engine](https://cloud.google.com/compute?hl=es)
- [RedisLab](https://app.redislabs.com/)

## Preparación del Workspace

1. Crear una llave de formato .JSON para usar como credencial para Google Cloud Storage.
2. Crear un archivo .env a partir del .example.env que se proporciona.
3. Ubicar ambos archivos en src/main/resources/

## Preparación de archivos .blend

1. Ingresar a https://free3d.com/es/modelos-3d/animated-blender.
2. Buscar un modelo (free).  
   Ej.: https://free3d.com/es/modelo-3d/black-dragon-rigged-and-game-ready-92023.html
   Recomendado con camara incluida: https://storage.googleapis.com/blend-example-bucket/Dragon_2.5_For_Animations.blend
3. Descargar siempre la opción '.blend' y descomprimir en cualquier ubicación.
4. Eliminar cualquier archivo que no sea .blend

## Ejecución de la aplicación

1. Run Gateway (Múltiples).
2. Run Servidor (Múltiples).
3. Run Worker (Múltiples).
4. Run Cliente (Múltiples).
5. Run Clean-Gateway (Solo uno).

## Notas

- Es posible que el archivo .blend descargado no contenga una 'Camara', por lo que no se podrá renderizar. Para agregar una camara seguir el siguiente tutorial: https://www.youtube.com/watch?t=46&v=aY04h4ujrlY&feature=youtu.be
- Para comprobar cuantos frames tiene una animación, se debe abrir el archivo .blend, ir al tab 'Animation' y en la parte inferior de la pantalla se encuentra dos variables Start y End.
- En el root del repositorio, se incluye un archivo .blend con camara incluida listo para probar (Dragon_2.5_For_Animations.blend).

<small><i>Última actualización: 14/12/2022</i></small>
