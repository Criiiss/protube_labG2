-- =====================================================================
-- ProTube - Esquema inicial (PostgreSQL 16)
-- Migración Flyway V1. NO modificar una vez aplicada: los cambios van en
-- un nuevo fichero V2__..., V3__..., etc.
-- =====================================================================

-- ---------------------------------------------------------------------
-- USUARIOS (Epic 1 Authentication + Epic 2 User profile)
-- auth_provider:
--   LOCAL    -> registro con email + password
--   GOOGLE   -> login OAuth (password_hash = NULL)
--   IMPORTED -> canal/autor creado al importar los JSON del videoGrabber
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username       VARCHAR(100) NOT NULL UNIQUE,
    email          VARCHAR(255) UNIQUE,
    password_hash  VARCHAR(255),
    auth_provider  VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',
    provider_id    VARCHAR(255),
    display_name   VARCHAR(150),
    avatar_url     VARCHAR(500),
    description    TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_users_provider CHECK (auth_provider IN ('LOCAL', 'GOOGLE', 'IMPORTED')),
    CONSTRAINT chk_users_local_has_credentials
        CHECK (auth_provider <> 'LOCAL' OR (email IS NOT NULL AND password_hash IS NOT NULL)),
    -- La app guarda el email en minúsculas: así el UNIQUE no distingue mayúsculas
    -- y un findByEmail normal usa el índice.
    CONSTRAINT chk_users_email_lowercase CHECK (email = LOWER(email))
);

-- Login OAuth: busca por (auth_provider, provider_id) e impide duplicar la cuenta.
-- Las filas con provider_id NULL (LOCAL/IMPORTED) no chocan entre sí.
CREATE UNIQUE INDEX ux_users_provider ON users (auth_provider, provider_id);

-- ---------------------------------------------------------------------
-- VÍDEOS (Epic 3 catálogo/reproducción + Epic 4 subida/edición)
-- file_name / thumbnail_name son relativos a ENV_PROTUBE_STORE_DIR
-- (p.ej. "12.mp4", "12.webp"); el fichero NO se guarda en la BD.
-- ---------------------------------------------------------------------
CREATE TABLE videos (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id        BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title           VARCHAR(255)  NOT NULL,
    description     TEXT,
    file_name       VARCHAR(255)  NOT NULL UNIQUE,
    thumbnail_name  VARCHAR(255),
    width           INT,
    height          INT,
    duration_sec    NUMERIC(10, 2),
    view_count      BIGINT        NOT NULL DEFAULT 0,  -- contador cacheado (analytics rápidas)
    like_count      BIGINT        NOT NULL DEFAULT 0,  -- contador cacheado
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_videos_view_count_positive CHECK (view_count >= 0),
    CONSTRAINT chk_videos_like_count_positive CHECK (like_count >= 0),
    CONSTRAINT chk_videos_width_positive      CHECK (width > 0),
    CONSTRAINT chk_videos_height_positive     CHECK (height > 0),
    CONSTRAINT chk_videos_duration_positive   CHECK (duration_sec >= 0)
);

CREATE INDEX ix_videos_owner      ON videos (owner_id);
CREATE INDEX ix_videos_created_at ON videos (created_at DESC);

-- ---------------------------------------------------------------------
-- CATEGORÍAS y TAGS (US-07 editor de metadatos). Relación N:M con vídeos
-- porque el videoGrabber ya entrega listas de categories y tags.
-- Los nombres se guardan en minúsculas ("Music" y "music" son el mismo).
-- ---------------------------------------------------------------------
CREATE TABLE categories (
    id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name  VARCHAR(100) NOT NULL UNIQUE,
    CONSTRAINT chk_categories_name_lowercase CHECK (name = LOWER(name))
);

CREATE TABLE video_categories (
    video_id     BIGINT NOT NULL REFERENCES videos (id)     ON DELETE CASCADE,
    category_id  BIGINT NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    PRIMARY KEY (video_id, category_id)
);

CREATE TABLE tags (
    id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name  VARCHAR(100) NOT NULL UNIQUE,
    CONSTRAINT chk_tags_name_lowercase CHECK (name = LOWER(name))
);

CREATE TABLE video_tags (
    video_id  BIGINT NOT NULL REFERENCES videos (id) ON DELETE CASCADE,
    tag_id    BIGINT NOT NULL REFERENCES tags (id)   ON DELETE CASCADE,
    PRIMARY KEY (video_id, tag_id)
);

CREATE INDEX ix_video_categories_category ON video_categories (category_id);
CREATE INDEX ix_video_tags_tag            ON video_tags (tag_id);

-- ---------------------------------------------------------------------
-- COMENTARIOS (Epic 6). parent_id NULL = comentario raíz; con valor = respuesta.
-- ---------------------------------------------------------------------
CREATE TABLE comments (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    video_id    BIGINT       NOT NULL REFERENCES videos (id)   ON DELETE CASCADE,
    author_id   BIGINT       NOT NULL REFERENCES users (id)    ON DELETE CASCADE,
    parent_id   BIGINT                REFERENCES comments (id) ON DELETE CASCADE,
    content     TEXT         NOT NULL,
    like_count  BIGINT       NOT NULL DEFAULT 0,  -- contador cacheado
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_comments_content_not_blank   CHECK (LENGTH(TRIM(content)) > 0),
    CONSTRAINT chk_comments_like_count_positive CHECK (like_count >= 0)
);

CREATE INDEX ix_comments_video  ON comments (video_id, created_at);
CREATE INDEX ix_comments_parent ON comments (parent_id);
CREATE INDEX ix_comments_author ON comments (author_id);

-- Likes a comentarios: la PK compuesta impide que un usuario dé 2 likes.
CREATE TABLE comment_likes (
    user_id     BIGINT      NOT NULL REFERENCES users (id)    ON DELETE CASCADE,
    comment_id  BIGINT      NOT NULL REFERENCES comments (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, comment_id)
);

CREATE INDEX ix_comment_likes_comment ON comment_likes (comment_id);

-- ---------------------------------------------------------------------
-- LIKES A VÍDEOS y VISUALIZACIONES (Epic 7 Analytics)
-- ---------------------------------------------------------------------
CREATE TABLE video_likes (
    user_id     BIGINT      NOT NULL REFERENCES users (id)  ON DELETE CASCADE,
    video_id    BIGINT      NOT NULL REFERENCES videos (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, video_id)
);

CREATE INDEX ix_video_likes_video ON video_likes (video_id, created_at);

-- Una fila por reproducción -> permite gráficas de vistas por día.
-- user_id NULL = visitante anónimo.
CREATE TABLE video_views (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    video_id   BIGINT      NOT NULL REFERENCES videos (id) ON DELETE CASCADE,
    user_id    BIGINT               REFERENCES users (id)  ON DELETE SET NULL,
    viewed_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_video_views_video_date ON video_views (video_id, viewed_at);
CREATE INDEX ix_video_views_user       ON video_views (user_id);
