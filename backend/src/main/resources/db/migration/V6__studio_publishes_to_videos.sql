-- =====================================================================
-- V6: los vídeos de Studio se publican en la tabla videos.
-- studio_videos queda como cola de subida/procesado; cuando un vídeo está
-- listo, Studio crea (y mantiene sincronizada) su fila en videos.
-- =====================================================================

ALTER TABLE videos
    ADD COLUMN publish_at       TIMESTAMPTZ,                      -- status = 'SCHEDULED' hasta esta fecha
    ADD COLUMN age_restricted   BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN comments_allowed BOOLEAN     NOT NULL DEFAULT TRUE,
    ADD COLUMN language         VARCHAR(10);

-- UNLISTED: no sale en catálogo ni búsqueda, pero se puede ver con el enlace.
ALTER TABLE videos DROP CONSTRAINT chk_videos_visibility;
ALTER TABLE videos
    ADD CONSTRAINT chk_videos_visibility CHECK (visibility IN ('PUBLIC', 'PRIVATE', 'UNLISTED')),
    ADD CONSTRAINT chk_videos_scheduled_has_date CHECK (status <> 'SCHEDULED' OR publish_at IS NOT NULL);

-- Solo hay que mirar los programados para publicarlos a su hora.
CREATE INDEX ix_videos_scheduled ON videos (publish_at) WHERE status = 'SCHEDULED';

ALTER TABLE studio_videos
    ADD COLUMN video_id BIGINT UNIQUE REFERENCES videos (id) ON DELETE SET NULL;
