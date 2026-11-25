-- =====================================================================
-- V4: visibilidad y estado de publicación de los vídeos.
-- Los vídeos que ya existen (importados) quedan públicos y publicados.
-- =====================================================================

ALTER TABLE videos
    ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    ADD COLUMN status     VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    ADD CONSTRAINT chk_videos_visibility CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    ADD CONSTRAINT chk_videos_status     CHECK (status IN ('DRAFT', 'SCHEDULED', 'PUBLISHED', 'ARCHIVED'));

-- "Vídeos de este canal con estado X, más nuevos primero".
CREATE INDEX ix_videos_owner_status_created ON videos (owner_id, status, created_at DESC);

-- Redundante: el índice nuevo empieza por owner_id y ya cubre la FK.
DROP INDEX ix_videos_owner;
