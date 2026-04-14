ALTER TABLE applications
    ADD COLUMN status_priority TINYINT
    GENERATED ALWAYS AS (IF(status = 'PENDING', 0, 1)) STORED NOT NULL;

CREATE INDEX idx_applications_team_priority_id
    ON applications (team_id, status_priority, id DESC);

CREATE INDEX idx_applications_post_priority_id
    ON applications (post_id, status_priority, id DESC);
