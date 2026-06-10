CREATE TABLE user_notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES "user"(id) ON DELETE CASCADE,
    friend_request BOOLEAN NOT NULL DEFAULT true,
    friend_accepted BOOLEAN NOT NULL DEFAULT true,
    session_started BOOLEAN NOT NULL DEFAULT true,
    friend_run_started BOOLEAN NOT NULL DEFAULT true,
    friend_joined_run BOOLEAN NOT NULL DEFAULT true,
    achievement_unlocked BOOLEAN NOT NULL DEFAULT true,
    run_result BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
