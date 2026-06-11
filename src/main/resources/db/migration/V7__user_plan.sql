ALTER TABLE "user" ADD COLUMN plan VARCHAR(16) NOT NULL DEFAULT 'free';
ALTER TABLE "user" ADD COLUMN plan_expires_at TIMESTAMPTZ NULL;
ALTER TABLE "user" ADD COLUMN revenuecat_id VARCHAR(255) NULL;
ALTER TABLE "user" ADD CONSTRAINT uq_user_revenuecat_id UNIQUE (revenuecat_id);
