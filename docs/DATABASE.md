# 🗄️ Base de datos de ProTube

Esta guía explica **cómo está montada la base de datos**, **por qué** está diseñada así y **qué tiene que
respetar el backend** al trabajar con ella. Está pensada tanto para quien se incorpora al proyecto como para
consultarla mientras se programa.

> **Resumen en 3 líneas**
> - Motor: **PostgreSQL 16**, la fuente de verdad de la aplicación (NFR-01: nada de BD en memoria en producción).
> - El esquema lo crea **Flyway** a partir de los scripts `db/migration/V*.sql`; Hibernate solo lo **valida**.
> - Los ficheros de vídeo **no** están en la BD: viven en disco (`ENV_PROTUBE_STORE_DIR`) y la BD solo guarda su nombre.

## Índice

1. [Visión general](#1-visión-general)
2. [Cómo se crea y evoluciona el esquema (Flyway)](#2-cómo-se-crea-y-evoluciona-el-esquema-flyway)
3. [Modelo de datos](#3-modelo-de-datos)
4. [Tablas en detalle](#4-tablas-en-detalle)
5. [Reglas que el backend debe cumplir](#5-reglas-que-el-backend-debe-cumplir)
6. [Qué pasa al borrar](#6-qué-pasa-al-borrar)
7. [Índices y para qué sirven](#7-índices-y-para-qué-sirven)
8. [Importación del videoGrabber](#8-importación-del-videograbber)
9. [Mapeo a entidades JPA](#9-mapeo-a-entidades-jpa)
10. [Operaciones habituales (cheatsheet)](#10-operaciones-habituales-cheatsheet)
11. [Futuro: búsqueda con MongoDB](#11-futuro-búsqueda-con-mongodb)

---

## 1. Visión general

```mermaid
flowchart LR
    FE["🖥️ Frontend<br/>React"] -->|REST /api| BE["☕ Backend<br/>Spring Boot"]
    BE -->|JPA / Hibernate| PG[("🐘 PostgreSQL<br/>metadatos, usuarios,<br/>comentarios, likes, vistas")]
    BE -->|lee / escribe ficheros| FS[("📁 ENV_PROTUBE_STORE_DIR<br/>12.mp4, 12.webp, 12.json")]
    FW["🛫 Flyway"] -.->|crea / actualiza<br/>el esquema al arrancar| PG
    BE -.-> FW
```

| Qué | Dónde se guarda | Por qué |
|---|---|---|
| Vídeos (`.mp4`) y miniaturas (`.webp`) | Disco, en `ENV_PROTUBE_STORE_DIR` | Son ficheros grandes; en la BD harían las copias y consultas lentísimas |
| Título, descripción, autor, tags, categorías… | PostgreSQL (`videos`, `tags`, …) | Se consultan, filtran y editan |
| Usuarios, comentarios, likes, visualizaciones | PostgreSQL | Datos relacionales que necesitan integridad |

La tabla `videos` solo guarda el **nombre** del fichero (`file_name = '12.mp4'`); el backend construye la ruta
completa con `ENV_PROTUBE_STORE_DIR`.

### Perfiles de Spring

| Perfil | Base de datos | ¿Quién crea las tablas? | Uso |
|---|---|---|---|
| `dev` | H2 en memoria | Hibernate (`ddl-auto=update`) | Desarrollo rápido sin Docker |
| `prod` | PostgreSQL | **Flyway** (`ddl-auto=validate`) | Run config "Production" y entrega con `docker compose` |

> ⚠️ Los scripts de Flyway están escritos para PostgreSQL, por eso Flyway está desactivado en `dev`.
> Antes de dar algo por bueno, pruébalo también con el perfil `prod`.

---

## 2. Cómo se crea y evoluciona el esquema (Flyway)

### ¿Qué es Flyway?

Una herramienta que ejecuta scripts SQL **versionados** y en orden. Los scripts están en:

```
backend/src/main/resources/db/migration/
└── V1__init_schema.sql      ← esquema inicial
    V2__...sql               ← futuros cambios (nunca editando V1)
```

El nombre sigue el formato `V<número>__<descripción>.sql` (dos guiones bajos).

### Qué hace al arrancar el backend

```mermaid
flowchart TD
    A([Arranca el backend con perfil prod]) --> B[Flyway lee db/migration/V*.sql]
    B --> C{¿Existe la tabla<br/>flyway_schema_history?}
    C -- No: BD vacía --> D[La crea]
    C -- Sí --> E[Compara scripts con el historial]
    D --> E
    E --> F{¿Algún script aplicado<br/>ha cambiado? checksum}
    F -- Sí --> X([❌ Error: checksum mismatch<br/>el backend no arranca])
    F -- No --> G[Ejecuta solo los scripts pendientes, en orden]
    G --> H[Hibernate valida que las entidades<br/>coinciden con las tablas]
    H --> I([✅ Backend listo])
```

### La tabla `flyway_schema_history`

Es el "registro" de Flyway: una fila por script aplicado. **No se toca a mano y no necesita entidad JPA.**

| version | description | checksum | installed_on | success |
|---|---|---|---|---|
| 1 | init schema | 123456789 | 2026-10-15 10:00 | t |

Gracias a ella:
- Cada base de datos (la tuya, la de un compañero, la de Docker) **se pone al día sola** al arrancar.
- Si alguien edita un script ya aplicado, el **checksum** no coincide y Flyway lo detecta.

### 📏 Reglas de oro

| ✅ Hacer | ❌ No hacer |
|---|---|
| Crear `V2__add_favorites.sql` para cualquier cambio | Editar `V1__init_schema.sql` una vez está en `develop`/`main` |
| Un script = un cambio coherente | Mezclar cambios no relacionados en un script |
| Actualizar **este documento** en el mismo PR | Cambiar las tablas desde DBeaver/pgAdmin a mano |
| Ajustar las entidades JPA a la vez que el SQL | Confiar en `ddl-auto=update` para producción |

> 💡 **Excepción:** mientras V1 no esté compartida (no está en `develop`/`main`) se puede editar. La BD local
> ya creada dará *checksum mismatch*; hay que borrarla y recrearla (ver [cheatsheet](#10-operaciones-habituales-cheatsheet)).

---

## 3. Modelo de datos

### Vista rápida por dominios

```mermaid
flowchart LR
    subgraph AUTH["👤 Usuarios"]
        users["users<br/>(identidad / login)"]
        user_profiles["user_profiles<br/>(perfil público)"]
    end
    subgraph CAT["🎬 Catálogo"]
        videos
        categories
        tags
        video_categories
        video_tags
    end
    subgraph SOC["💬 Social"]
        comments
        comment_likes
        video_likes
    end
    subgraph ANA["📊 Analytics"]
        video_views
    end
    users --- user_profiles
    users --> videos
    videos --- video_categories --- categories
    videos --- video_tags --- tags
    users --> comments
    videos --> comments
    comments --> comment_likes
    users --> comment_likes
    users --> video_likes
    videos --> video_likes
    videos --> video_views
    users -.-> video_views
```

### Diagrama entidad-relación completo

```mermaid
erDiagram
    users ||--|| user_profiles : "tiene"
    users ||--o{ videos : "sube"
    users ||--o{ comments : "escribe"
    videos ||--o{ comments : "tiene"
    comments |o--o{ comments : "respuestas"
    videos ||--o{ video_categories : ""
    categories ||--o{ video_categories : ""
    videos ||--o{ video_tags : ""
    tags ||--o{ video_tags : ""
    users ||--o{ comment_likes : "da"
    comments ||--o{ comment_likes : "recibe"
    users ||--o{ video_likes : "da"
    videos ||--o{ video_likes : "recibe"
    videos ||--o{ video_views : "registra"
    users |o--o{ video_views : "anónimo si NULL"

    users {
        bigint id PK
        varchar username UK
        varchar email UK "minúsculas"
        varchar password_hash "NULL si no es LOCAL"
        varchar auth_provider "LOCAL | GOOGLE | IMPORTED"
        varchar provider_id "sub de Google"
        timestamptz created_at
        timestamptz updated_at
    }
    user_profiles {
        bigint user_id PK,FK
        varchar display_name
        varchar avatar_url
        varchar banner_url
        text description
        varchar subscriptions_visibility "PUBLIC | PRIVATE"
        varchar liked_videos_visibility "PUBLIC | PRIVATE"
        timestamptz updated_at
    }
    videos {
        bigint id PK
        bigint owner_id FK
        varchar title
        text description
        varchar file_name UK "12.mp4"
        varchar thumbnail_name "12.webp"
        int width
        int height
        numeric duration_sec
        bigint view_count "contador cacheado"
        bigint like_count "contador cacheado"
        timestamptz created_at
        timestamptz updated_at
    }
    categories {
        bigint id PK
        varchar name UK "minúsculas"
    }
    tags {
        bigint id PK
        varchar name UK "minúsculas"
    }
    video_categories {
        bigint video_id PK,FK
        bigint category_id PK,FK
    }
    video_tags {
        bigint video_id PK,FK
        bigint tag_id PK,FK
    }
    comments {
        bigint id PK
        bigint video_id FK
        bigint author_id FK
        bigint parent_id FK "NULL = comentario raíz"
        text content
        bigint like_count "contador cacheado"
        timestamptz created_at
        timestamptz updated_at
    }
    comment_likes {
        bigint user_id PK,FK
        bigint comment_id PK,FK
        timestamptz created_at
    }
    video_likes {
        bigint user_id PK,FK
        bigint video_id PK,FK
        timestamptz created_at
    }
    video_views {
        bigint id PK
        bigint video_id FK
        bigint user_id FK "NULL = anónimo"
        timestamptz viewed_at
    }
```

### Tabla → requisito

| Tabla | Para qué sirve | Requisitos |
|---|---|---|
| `users` | Identidad: registro/login local, OAuth Google y canales importados | US-01, US-02, US-03 |
| `user_profiles` | Perfil público: nombre visible, avatar, banner, descripción y privacidad | Epic 2 (perfil) |
| `videos` | Catálogo, reproducción, subida y edición | US-04 … US-07 |
| `categories`, `tags` + `video_categories`, `video_tags` | Metadatos editables (relación N:M) | US-07 |
| `comments` | Comentarios y respuestas en hilo | US-09, US-10 |
| `comment_likes`, `video_likes` | Un like por usuario | US-10, US-11 |
| `video_views` | Una fila por reproducción → gráficas por fecha | US-11 |

### Convenciones del esquema

| Convención | Ejemplo | Motivo |
|---|---|---|
| Nombres en `snake_case`, tablas en plural | `video_views.viewed_at` | Postgres pasa todo a minúsculas; evita comillas |
| PK `BIGINT GENERATED ALWAYS AS IDENTITY` | `id` | Estándar SQL, secuencial, no se desborda |
| Fechas `TIMESTAMPTZ` | `created_at` | Guarda el instante exacto, sin líos de zona horaria |
| Tablas puente con PK compuesta | `PRIMARY KEY (video_id, tag_id)` | Impide duplicados sin columna `id` extra |
| Prefijos de nombres | `ix_` índice, `ux_` índice único, `chk_` CHECK | Se sabe qué es cada cosa en un error |

---

## 4. Tablas en detalle

### 👤 `users` — un único modelo para tres tipos de usuario

| `auth_provider` | Cómo nace | `email` | `password_hash` | `provider_id` |
|---|---|---|---|---|
| `LOCAL` | Formulario de registro | ✅ obligatorio | ✅ obligatorio (BCrypt) | — |
| `GOOGLE` | Primer login con Google | ✅ (de Google) | — | ✅ `sub` de Google |
| `IMPORTED` | Autor/canal del JSON del videoGrabber | — | — | — |

> Un usuario `IMPORTED` no puede iniciar sesión (no tiene credenciales): solo existe para ser autor de
> vídeos y comentarios importados.

### 🪪 `users` vs `user_profiles` — identidad separada del perfil

```mermaid
flowchart LR
    subgraph ID["users · identidad (¿quién eres?)"]
        direction TB
        a["username · email<br/>password_hash<br/>auth_provider · provider_id"]
    end
    subgraph PF["user_profiles · perfil (¿cómo te ven?)"]
        direction TB
        b["display_name · avatar_url · banner_url<br/>description<br/>subscriptions_visibility · liked_videos_visibility"]
    end
    ID -- "1 : 1<br/>user_profiles.user_id = users.id" --- PF
    L["🔐 Login / Spring Security"] --> ID
    E["✏️ Editar perfil / ver canal"] --> PF
```

| | `users` | `user_profiles` |
|---|---|---|
| Responde a | ¿Quién eres y cómo entras? | ¿Qué se muestra de ti? |
| Lo usa | Login, registro, OAuth (`UserDetailsService`) | Página de canal, editor de perfil |
| Cambia | Casi nunca | A menudo |
| Clave | `id` | `user_id` (PK **y** FK a `users.id`) |

**Por qué:** el login solo carga lo que necesita (credenciales), y editar el perfil nunca toca la entidad que usa
Spring Security. Además añadir campos de perfil no afecta al código de autenticación.

**Visibilidad** (`subscriptions_visibility`, `liked_videos_visibility`):

| Valor | Quién ve la lista |
|---|---|
| `PRIVATE` *(por defecto)* | Solo el propio usuario |
| `PUBLIC` | Cualquiera que visite su canal |

> ℹ️ `subscriptions_visibility` se deja preparado para la mejora "Subscriptions to other users"; la tabla de
> suscripciones todavía no existe.

### 🎬 `videos`

- `file_name` / `thumbnail_name` → ruta relativa a `ENV_PROTUBE_STORE_DIR`. `file_name` es único.
- `view_count` / `like_count` → **contadores cacheados** (ver más abajo).

#### ¿Por qué hay contadores si ya existen `video_views` y `video_likes`?

```mermaid
flowchart LR
    A["Usuario da like"] --> B["INSERT en video_likes<br/>(el detalle: quién y cuándo)"]
    A --> C["UPDATE videos<br/>SET like_count = like_count + 1<br/>(el total, para mostrarlo rápido)"]
```

- Mostrar "1.234 likes" en el catálogo no debería obligar a contar filas de `video_likes` en cada petición.
- Las tablas de detalle permiten las **gráficas por fecha** de analytics.
- Los vídeos importados traen sus contadores del JSON, aunque no tengan filas de detalle.

### 🏷️ `categories` y `tags`

Relación **N:M** con `videos` (un vídeo tiene varios tags y un tag está en varios vídeos), por eso las tablas
puente. Los nombres se guardan **en minúsculas**: `Music` y `music` son el mismo tag.

### 💬 `comments` — hilos con `parent_id`

```mermaid
flowchart TD
    V["🎬 Vídeo 12"] --> C1["💬 id=1 · parent_id=NULL<br/>'¡Gran vídeo!'"]
    V --> C2["💬 id=2 · parent_id=NULL<br/>'No me ha gustado'"]
    C1 --> R1["↳ id=3 · parent_id=1<br/>'Totalmente de acuerdo'"]
    C1 --> R2["↳ id=4 · parent_id=1<br/>'Yo también'"]
```

- `parent_id IS NULL` → comentario raíz.
- `parent_id = X` → respuesta al comentario `X` (del **mismo vídeo**, lo valida el servicio).

### ❤️ `video_likes` / `comment_likes`

La clave primaria es `(user_id, video_id)` / `(user_id, comment_id)`: la BD **impide** que un usuario dé dos
likes a lo mismo. Quitar el like = borrar la fila.

### 📊 `video_views`

Una fila por reproducción. `user_id` es `NULL` si quien mira no ha iniciado sesión. Ejemplo de consulta para
la gráfica de analytics:

```sql
SELECT date_trunc('day', viewed_at) AS dia, COUNT(*) AS vistas
FROM video_views
WHERE video_id = :videoId
GROUP BY dia
ORDER BY dia;
```

---

## 5. Reglas que el backend debe cumplir

La BD **rechaza** con un error de constraint cualquier dato que no siga estas reglas. Si ves un
`DataIntegrityViolationException`, busca aquí el nombre de la constraint.

| Regla | Dónde aplica en el código | Lo garantiza |
|---|---|---|
| `users.email`, `tags.name` y `categories.name` se guardan **en minúsculas** (`.toLowerCase()`) | Registro, login, importador, editor de metadatos | `chk_users_email_lowercase`, `chk_tags_name_lowercase`, `chk_categories_name_lowercase` |
| Login con Google: buscar por `auth_provider = 'GOOGLE'` + `provider_id` | Login OAuth | `ux_users_provider` (único) |
| Usuario `LOCAL` siempre con email y `password_hash` | Registro | `chk_users_local_has_credentials` |
| `auth_provider` solo `LOCAL`, `GOOGLE` o `IMPORTED` | Enum Java | `chk_users_provider` |
| **Todo usuario tiene su fila en `user_profiles`**, creada en la **misma transacción** que el usuario | Registro, primer login con Google, importador | ⚠️ **Solo el servicio** (la BD no lo puede exigir) |
| Visibilidades solo `PUBLIC` o `PRIVATE` (por defecto `PRIVATE`) | Enum Java, editor de perfil | `chk_user_profiles_subscriptions_visibility`, `chk_user_profiles_liked_videos_visibility` |
| Respetar la visibilidad: si es `PRIVATE`, la API no devuelve la lista a otros usuarios | Endpoints de canal / likes / suscripciones | ⚠️ **Solo el servicio** |
| Contadores nunca negativos | Likes / quitar like | `chk_videos_view_count_positive`, `chk_videos_like_count_positive`, `chk_comments_like_count_positive` |
| `width`, `height` > 0 y `duration_sec` ≥ 0 (o `NULL`) | Subida e importación | `chk_videos_width_positive`, `chk_videos_height_positive`, `chk_videos_duration_positive` |
| Comentario no vacío | Comentar / responder | `chk_comments_content_not_blank` |
| `username`, `email` y `file_name` únicos | Registro, subida, importador | `UNIQUE` |
| Una respuesta debe ser del **mismo vídeo** que su padre | Servicio de comentarios | ⚠️ **Solo el servicio** (no la BD) |

### Cómo dar un like correctamente

Hay que hacerlo **en la BD y de forma atómica**, no leyendo el contador en Java y guardándolo:

```sql
-- 1. Registrar el like (si ya existía, no hace nada)
INSERT INTO video_likes (user_id, video_id) VALUES (:userId, :videoId)
ON CONFLICT DO NOTHING;

-- 2. Solo si el INSERT ha insertado una fila:
UPDATE videos SET like_count = like_count + 1 WHERE id = :videoId;
```

> ❌ `video.setLikeCount(video.getLikeCount() + 1)` pierde likes si dos usuarios pulsan a la vez.

---

## 6. Qué pasa al borrar

```mermaid
flowchart TD
    U["🗑️ DELETE user"] --> P["user_profiles del usuario → se borra"]
    U --> V["videos del usuario → se borran"]
    U --> C["comments del usuario → se borran"]
    U --> L["video_likes / comment_likes del usuario → se borran"]
    U --> VV["video_views del usuario → user_id = NULL<br/>(se conservan las estadísticas)"]
    V --> V2["comments, likes, views, video_tags,<br/>video_categories del vídeo → se borran"]
    C --> C2["respuestas y likes del comentario → se borran"]
    T["🗑️ DELETE video"] --> V2
```

| Se borra… | Efecto |
|---|---|
| Un usuario | Se borran su perfil, sus vídeos, comentarios y likes. Sus visualizaciones quedan como anónimas |
| Un vídeo | Se borran sus comentarios, likes, vistas y enlaces a tags/categorías. **Los tags y categorías se mantienen** |
| Un comentario | Se borran sus respuestas y sus likes |
| Un tag o categoría | Desaparece de todos los vídeos (el vídeo no se borra) |

> ⚠️ Borrar un vídeo de la BD **no** borra el `.mp4`/`.webp` del disco: eso lo tiene que hacer el servicio.

---

## 7. Índices y para qué sirven

Postgres crea índices automáticos para las `PRIMARY KEY` y los `UNIQUE`. El resto se crean a mano en V1:

| Índice | Columnas | Acelera… |
|---|---|---|
| `ix_videos_owner` | `videos(owner_id)` | "Vídeos de este usuario", borrados en cascada |
| `ix_videos_created_at` | `videos(created_at DESC)` | Listado del catálogo, más nuevos primero |
| `ix_video_categories_category` | `video_categories(category_id)` | "Vídeos de esta categoría" |
| `ix_video_tags_tag` | `video_tags(tag_id)` | "Vídeos con este tag" |
| `ix_comments_video` | `comments(video_id, created_at)` | Comentarios de un vídeo ordenados |
| `ix_comments_parent` | `comments(parent_id)` | Respuestas de un comentario |
| `ix_comments_author` | `comments(author_id)` | Comentarios de un usuario |
| `ix_comment_likes_comment` | `comment_likes(comment_id)` | Likes de un comentario |
| `ix_video_likes_video` | `video_likes(video_id, created_at)` | Likes de un vídeo y gráficas por fecha |
| `ix_video_views_video_date` | `video_views(video_id, viewed_at)` | Gráficas de vistas por día |
| `ix_video_views_user` | `video_views(user_id)` | Historial de un usuario, borrado de usuario |
| `ux_users_provider` | `users(auth_provider, provider_id)` | Login con Google (y evita duplicados) |

> `user_profiles.user_id` no necesita índice propio: al ser la PK ya lo tiene.
>
> Regla aplicada: **toda clave foránea tiene un índice** que empieza por ella. Postgres no los crea solo y sin
> ellos los JOIN y los `ON DELETE CASCADE` recorren la tabla entera.

---

## 8. Importación del videoGrabber

Al arrancar, el importador lee cada `<id>.json` de `ENV_PROTUBE_STORE_DIR` y lo reparte así:

```mermaid
flowchart LR
    J["📄 12.json"] --> U1["user → users (IMPORTED)<br/>+ user_profiles"]
    J --> V["title, meta.description,<br/>width, height, duration → videos"]
    J --> T["meta.tags → tags + video_tags"]
    J --> C["meta.categories → categories + video_categories"]
    J --> CM["meta.comments[] → comments"]
    CM --> U2["comments[].author → users (IMPORTED)<br/>+ user_profiles"]
```

| Campo del JSON | Destino | Nota |
|---|---|---|
| `user` | `users.username` (`IMPORTED`) + `user_profiles.display_name` | Reutilizar si ya existe. Crear también su perfil (visibilidades por defecto) |
| `title` | `videos.title` | |
| `meta.description` | `videos.description` | |
| `width`, `height`, `duration` | `videos.width/height/duration_sec` | |
| `timestamp` (epoch, segundos) | `videos.created_at` | Fecha original de publicación |
| `meta.view_count`, `meta.like_count` | `videos.view_count`, `videos.like_count` | Contadores iniciales (sin filas de detalle) |
| `<id>.mp4` / `<id>.webp` | `videos.file_name` / `thumbnail_name` | |
| `meta.tags[]` | `tags.name` + `video_tags` | **En minúsculas**, reutilizar si existe |
| `meta.categories[]` | `categories.name` + `video_categories` | **En minúsculas**, reutilizar si existe |
| `meta.comments[].author` | `users.username` (`IMPORTED`) + `user_profiles.display_name` | Igual que `user` |
| `meta.comments[].text` | `comments.content` | Saltar los vacíos |
| `meta.comments[].like_count` | `comments.like_count` | |
| `meta.comments[].timestamp` | `comments.created_at` | |
| `meta.channel_follower_count` | — | No se guarda (no hay suscripciones en el MVP) |

> 💡 El `username` es único entre **todos** los usuarios: si un canal importado se llama igual que alguien que
> se registra después, chocarán. El importador debería añadir un prefijo (p. ej. `yt_<nombre>`).

---

## 9. Mapeo a entidades JPA

Como Hibernate está en `validate`, las entidades deben coincidir **exactamente** con el SQL:

| Tipo SQL | Tipo Java | Anotación |
|---|---|---|
| `BIGINT GENERATED ALWAYS AS IDENTITY` | `Long` | `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` |
| `VARCHAR(n)` | `String` | `@Column(length = n)` |
| `TEXT` | `String` | `@Column(columnDefinition = "text")` |
| `TIMESTAMPTZ` | `Instant` (o `OffsetDateTime`) | `@CreationTimestamp` / `@UpdateTimestamp` |
| `NUMERIC(10,2)` | `BigDecimal` | `@Column(precision = 10, scale = 2)` |
| `auth_provider` | `enum AuthProvider` | `@Enumerated(EnumType.STRING)` |
| `*_visibility` | `enum Visibility { PUBLIC, PRIVATE }` | `@Enumerated(EnumType.STRING)` |
| `user_profiles.user_id` (PK = FK) | `User user` en `UserProfile` | `@Id Long userId` + `@OneToOne @MapsId` |
| Tabla puente (`video_tags`) | `Set<Tag>` | `@ManyToMany` + `@JoinTable` |
| `parent_id` | `Comment parent` | `@ManyToOne` |

> ❌ No uses `LocalDateTime` para `TIMESTAMPTZ`: pierde la zona horaria.
> ℹ️ `updated_at` no tiene trigger en la BD: se actualiza solo vía `@UpdateTimestamp`.

---

## 10. Operaciones habituales (cheatsheet)

> En Git Bash (Windows) antepón `MSYS_NO_PATHCONV=1` a los comandos `docker` para que no traduzca las rutas.

| Quiero… | Comando |
|---|---|
| Levantar Postgres | `docker compose up -d` |
| Abrir una consola SQL | `docker exec -it postgres_db psql -U admin -d postgres` |
| Ver las tablas | `\dt` dentro de `psql` |
| Ver la estructura de una tabla | `\d videos` dentro de `psql` |
| Ver qué migraciones hay aplicadas | `docker exec postgres_db psql -U admin -d postgres -c "SELECT version, description, success FROM flyway_schema_history;"` |
| Borrar todo el esquema (empezar de cero) | `docker exec postgres_db psql -U admin -d postgres -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"` |
| Borrar también el contenedor y sus datos | `docker compose down -v` |
| Aplicar las migraciones | Arrancar el backend con la run config **Production** |
| Aplicar las migraciones **sin arrancar el backend** | `docker run --rm --network container:postgres_db -v "<ruta>/backend/src/main/resources/db/migration:/flyway/sql:ro" flyway/flyway:10.10 -url=jdbc:postgresql://localhost:5432/postgres -user=admin -password=password migrate` |

### Añadir un cambio al esquema (paso a paso)

```mermaid
flowchart LR
    A["1. Crear<br/>V2__descripcion.sql"] --> B["2. Ajustar<br/>entidades JPA"]
    B --> C["3. Actualizar<br/>DATABASE.md"]
    C --> D["4. Arrancar con prod<br/>y comprobar"]
    D --> E["5. PR con<br/>referencia al prompt"]
```

1. Crear `backend/src/main/resources/db/migration/V2__descripcion.sql` (siguiente número libre).
2. Ajustar las entidades JPA (si no, `ddl-auto=validate` no deja arrancar).
3. Actualizar **este documento**: diagrama, tabla y reglas del backend si cambian.
4. Arrancar con perfil `prod` y comprobar `flyway_schema_history`.
5. Abrir PR (si se ha usado IA, guardar el prompt en `prompts/` y referenciarlo — NFR-07).

### Errores típicos

| Error | Causa | Solución |
|---|---|---|
| `Validate failed: Migration checksum mismatch for migration version 1` | Se ha editado un script ya aplicado | Si no está compartido: borrar el esquema y rearrancar. Si lo está: deshacer la edición y crear una V2 |
| `Schema-validation: missing column [...]` | La entidad JPA no coincide con el SQL | Ajustar la entidad o crear una migración |
| `violates check constraint "chk_..."` | Dato que no cumple una regla | Ver [reglas del backend](#5-reglas-que-el-backend-debe-cumplir) |
| `duplicate key value violates unique constraint` | Email, username, tag… repetido | Comprobar si ya existe antes / reutilizarlo |

---

## 11. Futuro: búsqueda con MongoDB

```mermaid
flowchart LR
    BE["☕ Backend"] -->|escribe| PG[("🐘 PostgreSQL<br/>fuente de verdad")]
    BE -->|sincroniza al crear/editar vídeo| MG[("🍃 MongoDB<br/>índice de búsqueda")]
    S["🔎 Búsqueda"] --> MG
    MG -->|ids de vídeos| BE
```

PostgreSQL sigue siendo la **fuente de verdad**. MongoDB sería solo un índice de lectura (título, descripción,
tags, categorías) que se actualiza cuando se crea o edita un vídeo. **No requiere cambiar este esquema.**
