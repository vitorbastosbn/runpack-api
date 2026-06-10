-- Allow deleting a user account to cascade-remove everything they own.
-- group.creator_id and session.created_by were RESTRICT by default.

ALTER TABLE "group" DROP CONSTRAINT group_creator_id_fkey;
ALTER TABLE "group" ADD CONSTRAINT group_creator_id_fkey
    FOREIGN KEY (creator_id) REFERENCES "user"(id) ON DELETE CASCADE;

ALTER TABLE session DROP CONSTRAINT session_created_by_fkey;
ALTER TABLE session ADD CONSTRAINT session_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES "user"(id) ON DELETE CASCADE;

-- Deleting a group (e.g. when its creator is deleted) must also remove its sessions.
ALTER TABLE session DROP CONSTRAINT session_group_id_fkey;
ALTER TABLE session ADD CONSTRAINT session_group_id_fkey
    FOREIGN KEY (group_id) REFERENCES "group"(id) ON DELETE CASCADE;

-- Achievements reference a session optionally; keep the achievement, null the link.
ALTER TABLE user_achievement DROP CONSTRAINT user_achievement_session_id_fkey;
ALTER TABLE user_achievement ADD CONSTRAINT user_achievement_session_id_fkey
    FOREIGN KEY (session_id) REFERENCES session(id) ON DELETE SET NULL;
