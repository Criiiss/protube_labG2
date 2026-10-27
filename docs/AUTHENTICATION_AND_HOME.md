# Autenticación y home

Se conserva React/TypeScript/Vite y el backend Spring Boot existente. No se cambia el contrato antiguo `GET /api/videos` ni los DTO de perfiles.

## Ejecutar

1. Arranca el backend desde la configuración habitual de IntelliJ con Java 21 o superior. Mantén `ENV_PROTUBE_STORE_DIR` apuntando a tu carpeta de medios.
2. En otra terminal: `cd frontend` y `npm run dev`.
3. Abre `http://localhost:5173`. Las pantallas usan rutas hash (`/#/login`, `/#/register`, `/#/forgot`, `/#/profile`), compatibles con el servidor existente.

El perfil `dev` utiliza H2 en memoria: los usuarios se pierden al reiniciar. En PostgreSQL, Flyway aplica V2 automáticamente; no se modifica V1. Las sesiones son de servidor y se pierden al reiniciar la aplicación. La opción Recordarme conserva la cookie hasta 30 días mientras esa sesión exista; sin ella la sesión caduca a los 30 minutos o al cerrar el navegador.

## Correo

Para probar verificación y recuperación necesitas un servidor SMTP. Por defecto se usa `localhost:1025`, adecuado para Mailpit/MailHog en desarrollo. Abre la bandeja de ese servicio y sigue el enlace recibido. No se devuelven tokens al navegador ni se escriben contraseñas o enlaces en logs.

Variables opcionales:

| Variable | Valor por defecto / propósito |
| --- | --- |
| `ENV_PROTUBE_SMTP_HOST` | `localhost` |
| `ENV_PROTUBE_SMTP_PORT` | `1025` |
| `ENV_PROTUBE_SMTP_USER` / `ENV_PROTUBE_SMTP_PASSWORD` | Vacíos |
| `ENV_PROTUBE_SMTP_AUTH` / `ENV_PROTUBE_SMTP_TLS` | `false` |
| `ENV_PROTUBE_MAIL_FROM` | `no-reply@localhost` |
| `ENV_PROTUBE_FRONTEND_URL` | `http://localhost:5173`; origen CORS y destino de enlaces |
| `ENV_PROTUBE_MINIMUM_AGE` | `13`; decisión configurable del proyecto |
| `ENV_PROTUBE_SECURE_COOKIES` | `false` en desarrollo; `true` con HTTPS |

Sin SMTP el registro muestra un error y se revierte, en vez de crear una cuenta que no puede verificarse. La verificación caduca en 24 horas y la recuperación en 30 minutos. Ambos enlaces se consumen una vez; reenviarlos invalida el anterior del mismo tipo. Cambiar la contraseña invalida las sesiones anteriores.

## Google

Configura `ENV_PROTUBE_GOOGLE_CLIENT_ID`, `ENV_PROTUBE_GOOGLE_CLIENT_SECRET` y, si cambia la URL, `ENV_PROTUBE_GOOGLE_CALLBACK` (por defecto `http://localhost:8080/api/auth/google/callback`). Autoriza exactamente ese callback en la consola de Google. El botón se deshabilita mientras no haya configuración.

La primera entrada exige elegir usuario, aportar fecha de nacimiento y aceptar términos. Una cuenta local no se enlaza automáticamente a Google por coincidencia de email; debe entrar con su contraseña. La llamada real a Google requiere esas credenciales y no forma parte de las pruebas locales automatizadas.

## API y seguridad

- `/api/auth/config`, `/captcha`, `/register`, `/login`, `/session`, `/logout`, `/forgot`, `/resend`, `/verify`, `/reset`, `/google`, `/google/callback`, `/google/complete`.
- Contraseñas bcrypt, coste 12; máximo 72 bytes para evitar truncamiento. Email y usuario local se normalizan a minúsculas.
- Validación en cliente y servidor, errores por campo, captcha aritmético de un uso ligado a sesión y con caducidad de 5 minutos. Es una comprobación básica para el proyecto, no un sustituto de un proveedor antibots para producción.
- Límite de 5 intentos de acceso por identificador en 15 minutos, además de límites por IP. Contadores en memoria por instancia.
- Cookie HttpOnly / SameSite=Lax; rotación de sesión al acceder. Las mutaciones requieren `X-Protube-Request: 1`; CORS admite exclusivamente el origen configurado con credenciales.
- Los endpoints privados de perfiles conservan `userId` por compatibilidad y comprueban que corresponde a la sesión, sin confiar en el identificador enviado por el cliente.
- Los términos incluidos describen el prototipo académico. El responsable debe revisar y completar condiciones y privacidad antes de publicar el servicio.

## Home

`GET /api/catalog` lee los metadatos de las tablas existentes y devuelve una lista vacía si aún no existe la tabla de vídeos en H2. No usa las cadenas ficticias de `/api/videos`. Las portadas siguen usando `dog.png`.

La home distingue carga, error, catálogo vacío y modo de muestra voluntario. Conserva el diseño de 3 vídeos + carrusel + bloque final; permite buscar, filtrar, ordenar y cargar más. La selección «Recomendados» conserva el orden del catálogo: no hay un motor personalizado de recomendación.

Biblioteca, Me gusta, Ver más tarde, No me interesa, historial y suscripciones se guardan por usuario en `localStorage` de este navegador. No se sincronizan con la BD ni otros dispositivos. El historial registra la selección de una tarjeta; la reproducción real, la subida y las notificaciones de servidor pertenecen a otros apartados y siguen pendientes. El menú muestra avisos claros para esas funciones.

## Validación

- Frontend: `npm run build`, `npx eslint src/App.tsx src/auth src/home src/profile/ProfilePage.tsx src/profile/profileApi.ts src/components/home`, `npx jest src/auth/validation.test.ts --runInBand --coverage=false`.
- Backend: `mvn test -Dpro_tube.store.dir=target/test-media` desde `backend`. Las pruebas de autenticación usan H2 y un correo simulado; cubren cifrado, verificación, duplicados, caducidad, recuperación, revocación de sesiones, acceso a perfiles, logout, captcha, CSRF y limitación de intentos. Se mantienen los umbrales JaCoCo existentes.
