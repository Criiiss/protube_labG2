-- =====================================================================
-- V5: playlists de usuario (US-08 las busca; cualquier usuario puede crearlas).
-- Solo admiten vídeos de la tabla videos (los de Studio usan otro id).
-- =====================================================================

CREATE TABLE playlists (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title        VARCHAR(150) NOT NULL,
    description  TEXT,
    visibility   VARCHAR(20)  NOT NULL DEFAULT 'PUBLIC',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_playlists_title_not_blank CHECK (LENGTH(TRIM(title)) > 0),
    CONSTRAINT chk_playlists_visibility      CHECK (visibility IN ('PUBLIC', 'PRIVATE'))
);

CREATE INDEX ix_playlists_owner ON playlists (owner_id, created_at DESC);

-- PK compuesta: un vídeo aparece una sola vez en cada playlist.
CREATE TABLE playlist_videos (
    playlist_id  BIGINT      NOT NULL REFERENCES playlists (id) ON DELETE CASCADE,
    video_id     BIGINT      NOT NULL REFERENCES videos (id)    ON DELETE CASCADE,
    position     INT         NOT NULL,
    added_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (playlist_id, video_id),
    CONSTRAINT chk_playlist_videos_position_positive CHECK (position >= 0)
);

CREATE INDEX ix_playlist_videos_video ON playlist_videos (video_id);
