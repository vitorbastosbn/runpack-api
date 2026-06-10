ALTER TABLE friendship
    ADD COLUMN requester_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN addressee_favorite BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_friendship_requester_favorite
    ON friendship(requester_id)
    WHERE requester_favorite = TRUE;

CREATE INDEX idx_friendship_addressee_favorite
    ON friendship(addressee_id)
    WHERE addressee_favorite = TRUE;
