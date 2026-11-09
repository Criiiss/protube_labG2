# Studio: subida y edición (US06 y US07)

Arranca el backend y el frontend como se describe en [Autenticación y home](AUTHENTICATION_AND_HOME.md). En desarrollo puedes entrar con `admin` / `admin`. Pulsa **Subir contenido** en la cabecera o abre `http://localhost:5173/#/studio`.

## Preparar el procesamiento

El servidor necesita **FFmpeg** y **FFprobe** con el codificador `libx264`. Puedes obtenerlos desde la [página oficial de descargas](https://ffmpeg.org/download.html). Añade la carpeta `bin` a `PATH` o indica las rutas completas con estas variables de entorno antes de arrancar el backend:

| Variable | Valor por defecto |
| --- | --- |
| `PROTUBE_STUDIO_FFMPEG` | `ffmpeg` |
| `PROTUBE_STUDIO_FFPROBE` | `ffprobe` |
| `PROTUBE_STUDIO_MAX_BYTES` | `104857600` (100 MiB) |
| `PROTUBE_STUDIO_MAX_SECONDS` | `1200` (20 minutos) |
| `PROTUBE_STUDIO_DIRECTORY` | Carpeta hermana de `ENV_PROTUBE_STORE_DIR` con sufijo `.studio` |

Ejemplo para la terminal PowerShell donde arrancas Spring Boot:

```powershell
$env:PROTUBE_STUDIO_FFMPEG = "C:\ffmpeg\bin\ffmpeg.exe"
$env:PROTUBE_STUDIO_FFPROBE = "C:\ffmpeg\bin\ffprobe.exe"
```

## Uso

1. Selecciona o arrastra un archivo MP4, MOV o WebM. Rellena el título y los demás datos.
2. Selecciona visibilidad o guarda como borrador. Para programar, elige **Público** e introduce una fecha y hora local.
3. Pulsa **Subir vídeo**. La barra indica los bloques confirmados por el servidor. Si falla la conexión, se reintenta; si se interrumpe, abre el vídeo en **Mis vídeos**, selecciona el mismo archivo y pulsa **Reanudar subida**. **Cancelar subida** descarta los datos mientras esté pendiente o subiendo.
4. El servidor comprueba el archivo, extrae la duración y genera 360p, 720p, 1080p y tres miniaturas. Studio muestra el estado y la campana recoge los resultados. Los avisos aparecen en la web mientras está abierta.
5. En **Mis vídeos** puedes editar los metadatos, cambiar la miniatura o abrir la página de reproducción. También puedes subir JPG/PNG en 16:9, de 640×360 a 3840×2160 y hasta 5 MiB.

## Comportamiento del servidor

- Solo el propietario puede administrar el vídeo. Un vídeo privado, programado para más adelante o en borrador solo se puede previsualizar desde la cuenta propietaria una vez listo. El no listado es accesible por enlace. El catálogo incluye únicamente vídeos públicos y listos cuya fecha de publicación ya ha llegado.
- La restricción 18+ comprueba la fecha de nacimiento de la sesión. El propietario puede previsualizar su propio contenido. La preferencia de comentarios se guarda y se muestra en la página del vídeo; no crea un sistema de comentarios.
- Los archivos se guardan fuera de la ruta pública `/media`. El servidor verifica tamaño, SHA-256 y duración antes de publicar. Un fallo conserva el registro con estado **Error** para que el propietario pueda descartarlo.
- Flyway aplica V3 en PostgreSQL. En desarrollo, H2 sigue en memoria: los registros y el progreso no sobreviven al reinicio del backend. Con PostgreSQL y almacenamiento persistente se conserva el progreso. El trabajador está pensado para una sola instancia del backend y retoma los trabajos que quedaron procesándose al reiniciar.
- Se generan las tres resoluciones incluso si el original es pequeño; ampliar un vídeo no aumenta el detalle del original.

## Validación

Las pruebas de Studio cubren reanudación, cancelación, permisos, borradores, visibilidad, horario y miniaturas. Para ejecutar la suite completa con su cobertura, define las rutas de FFmpeg y FFprobe; la conversión real comprueba archivos MP4, MOV y WebM y necesita también el codificador `libvpx-vp9` para generar un WebM de prueba. La integración continua prepara estas herramientas antes de ejecutar Maven.
