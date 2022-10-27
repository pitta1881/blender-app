## Proyecto
El presente proyecto es un Trabajo Práctico Final Integrador de Conocimientos para la materia Sistemas Distribuidos y Programación Paralela de la Universidad Nacional de Luján.

## Propuesta
La propuesta consiste en crear una red distribuida de renderizado de imágenes a partir de un archivo .blend creado en el programa Blender, otorgando así una forma de renderizar modelos y animaciones aún en aquellos computadores sin la potencia requerida para éste trabajo. Ésta red será tolerante a fallos, escalable, multithread y distribuida.

## Autores
- Ledesma Damián (Leg.: 155825)
- Pittavino Patricio Ariel (Leg.: 121476)
- Salinas Leonardo (Leg.: 104478)

## Tecnologías
- [Java 17](https://www.oracle.com/java/technologies/downloads/#java17)
- [Blender](https://www.blender.org/).

### Dependencias
- [Gson](https://mvnrepository.com/artifact/com.google.code.gson/gson)
- [Logback Classic Module](https://mvnrepository.com/artifact/ch.qos.logback/logback-classic)
- [Apache FtpServer](https://mvnrepository.com/artifact/org.apache.ftpserver/ftpserver)
- [Apache Commons Net](https://mvnrepository.com/artifact/commons-net/commons-net)
- [Zip4j](https://mvnrepository.com/artifact/net.lingala.zip4j/zip4j)

### Desarrollo
- [Git](https://git-scm.com/)
- [IntelliJ IDEA](https://www.jetbrains.com/idea/)

## Preparación del Workspace
1. Descargar **BLENDER PORTABLE** para windows desde https://www.blender.org/download/.
2. Renombrar carpeta root del .zip a 'blender-windows'. Ej. blender-3.3.1-windows-x64 -> blender-windows
3. Ubicar .zip en src/main/resources/Servidor/FTP/

## Preparación de archivos .blend
1. Ingresar a https://free3d.com/es/modelos-3d/animated-blender.
2. Buscar un modelo (free).  
   Ej.: https://free3d.com/es/modelo-3d/black-dragon-rigged-and-game-ready-92023.html
3. Descargar siempre la opción '.blend' y descomprimir en cualquier ubicación.
4. Eliminar cualquier archivo que no sea .blend

## Ejecución de la aplicación
1. Run Gateway (Solo 1).
2. Run Servidor (Múltiples).
3. Run Worker (Múltiples).
4. Run Cliente (Solo 1).

## Notas
- Es posible que el archivo .blend descargado no contenga una 'Camara', por lo que no se podrá renderizar. Para agregar una camara seguir el siguiente tutorial: https://www.youtube.com/watch?t=46&v=aY04h4ujrlY&feature=youtu.be
- Para comprobar cuantos frames tiene una animación, se debe abrir el archivo .blend, ir al tab 'Animation' y en la parte inferior de la pantalla se encuentra dos variables Start y End.
- En el root del repositorio, se incluye un archivo .blend con camara incluida listo para probar (Dragon_2.5_For_Animations.blend).

## Oportunidad de Mejoras
- Agregar la posibilidad de que el cliente pueda enviar múltiples archivos a ser renderizados (batch).

<small><i>Última actualización: 27/10/2022</i></small>
