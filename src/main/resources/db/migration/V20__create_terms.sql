CREATE TABLE terms
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    type           VARCHAR(40)  NOT NULL,
    version        INT          NOT NULL,
    content_url    VARCHAR(2048) NOT NULL,
    mandatory      BOOLEAN      NOT NULL,
    activated_at   DATETIME(6)  NOT NULL,
    deprecated_at  DATETIME(6)  NULL,
    created_at     DATETIME(6)  NOT NULL,
    CONSTRAINT uk_terms_type_version UNIQUE (type, version)
);

CREATE TABLE user_term_agreements
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BINARY(16)  NOT NULL,
    term_id       BIGINT      NOT NULL,
    agreed_at     DATETIME(6) NOT NULL,
    withdrawn_at  DATETIME(6) NULL,
    CONSTRAINT uk_user_term_agreements_user_term UNIQUE (user_id, term_id)
);
