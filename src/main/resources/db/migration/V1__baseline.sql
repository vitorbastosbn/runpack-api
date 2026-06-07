-- RunPack baseline schema

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE "user" (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email        VARCHAR(255) NOT NULL UNIQUE,
    name         VARCHAR(255) NOT NULL,
    username     VARCHAR(20)  UNIQUE,
    avatar_url   VARCHAR(500),
    provider     VARCHAR(10)  NOT NULL CHECK (provider IN ('google', 'apple')),
    provider_id  VARCHAR(255) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (provider, provider_id)
);

CREATE TABLE friendship (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_id  UUID NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    addressee_id  UUID NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    status        VARCHAR(10) NOT NULL CHECK (status IN ('pending', 'accepted', 'rejected', 'blocked')),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (requester_id, addressee_id)
);

CREATE TABLE "group" (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(50)  NOT NULL,
    description  VARCHAR(200),
    image_url    VARCHAR(500),
    creator_id   UUID NOT NULL REFERENCES "user"(id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE group_member (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id   UUID NOT NULL REFERENCES "group"(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    role       VARCHAR(10) NOT NULL CHECK (role IN ('admin', 'member')),
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (group_id, user_id)
);

CREATE TABLE invite_token (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash   VARCHAR(64)  NOT NULL UNIQUE,
    type         VARCHAR(10)  NOT NULL CHECK (type IN ('group', 'session')),
    target_id    UUID         NOT NULL,
    created_by   UUID         NOT NULL REFERENCES "user"(id),
    expires_at   TIMESTAMPTZ  NOT NULL,
    used_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE session (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id     UUID REFERENCES "group"(id),
    created_by   UUID NOT NULL REFERENCES "user"(id),
    status       VARCHAR(10) NOT NULL CHECK (status IN ('active', 'finished')) DEFAULT 'active',
    started_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finished_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE session_participant (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id  UUID NOT NULL REFERENCES session(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    left_at     TIMESTAMPTZ,
    UNIQUE (session_id, user_id)
);

CREATE TABLE session_telemetry (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id  UUID    NOT NULL REFERENCES session(id) ON DELETE CASCADE,
    user_id     UUID    NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    elapsed_ms  BIGINT  NOT NULL,
    distance_m  FLOAT   NOT NULL,
    pace_s_km   FLOAT   NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE run_result (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id       UUID NOT NULL REFERENCES session(id) ON DELETE CASCADE,
    user_id          UUID NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    total_distance_m FLOAT   NOT NULL DEFAULT 0,
    total_time_ms    BIGINT  NOT NULL DEFAULT 0,
    avg_pace_s_km    FLOAT   NOT NULL DEFAULT 0,
    final_rank       INT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (session_id, user_id)
);

CREATE TABLE achievement (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug        VARCHAR(50) NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    icon_url    VARCHAR(500)
);

CREATE TABLE user_achievement (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    achievement_id  UUID NOT NULL REFERENCES achievement(id),
    session_id      UUID REFERENCES session(id),
    unlocked_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, achievement_id)
);

CREATE TABLE push_token (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    token      VARCHAR(500) NOT NULL,
    platform   VARCHAR(10)  NOT NULL CHECK (platform IN ('ios', 'android')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, platform)
);

-- Índices
CREATE INDEX idx_friendship_requester ON friendship(requester_id);
CREATE INDEX idx_friendship_addressee ON friendship(addressee_id);
CREATE INDEX idx_group_member_group   ON group_member(group_id);
CREATE INDEX idx_group_member_user    ON group_member(user_id);
CREATE INDEX idx_session_group        ON session(group_id);
CREATE INDEX idx_session_status       ON session(status);
CREATE INDEX idx_telemetry_session    ON session_telemetry(session_id, user_id);
CREATE INDEX idx_run_result_user      ON run_result(user_id);
CREATE INDEX idx_user_achievement     ON user_achievement(user_id);
