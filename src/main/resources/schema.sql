-- =========================================
-- Drop all tables if exist
-- =========================================
DROP TABLE IF EXISTS recommendation_clothes CASCADE;
DROP TABLE IF EXISTS recommendations CASCADE;
DROP TABLE IF EXISTS feed_clothes CASCADE;
DROP TABLE IF EXISTS feed_likes CASCADE;
DROP TABLE IF EXISTS comments CASCADE;
DROP TABLE IF EXISTS feeds CASCADE;
DROP TABLE IF EXISTS clothes_attribute CASCADE;
DROP TABLE IF EXISTS clothes CASCADE;
DROP TABLE IF EXISTS clothes_attributes_def CASCADE;
DROP TABLE IF EXISTS user_oauth_providers CASCADE;
DROP TABLE IF EXISTS user_profiles CASCADE;
DROP TABLE IF EXISTS direct_messages CASCADE;
DROP TABLE IF EXISTS follows CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS weathers CASCADE;
DROP TABLE IF EXISTS weather_locations CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- =========================================
-- Create tables (순서 정리 완료)
-- =========================================

-- 1) Users
CREATE TABLE IF NOT EXISTS users
(
    id                UUID PRIMARY KEY,
    email             VARCHAR(255) NOT NULL,
    name              VARCHAR(20)  NOT NULL,
    password          VARCHAR(255),
    role              VARCHAR(20)  NOT NULL,
    locked            BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL,
    profile_image_url VARCHAR(255),
    provider_user_id   VARCHAR(255),
    provider           VARCHAR(20)  NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_provider_uid UNIQUE (provider, provider_user_id),
    CONSTRAINT chk_provider CHECK (provider IN ('GENERAL','GOOGLE','KAKAO'))
);

-- 2) Weathers
CREATE TABLE IF NOT EXISTS weather_locations
(
    id             UUID PRIMARY KEY,
    latitude       NUMERIC      NOT NULL,
    longitude      NUMERIC      NOT NULL,
    x              INTEGER      NOT NULL,
    y              INTEGER      NOT NULL,
    location_names VARCHAR(255) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,

    CONSTRAINT ck_weather_locations_grid_x CHECK (x BETWEEN 0 AND 500),
    CONSTRAINT ck_weather_locations_grid_y CHECK (y BETWEEN 0 AND 500),
    CONSTRAINT ck_location_names_nonempty CHECK (btrim(location_names) <> ''),
    CONSTRAINT uq_weather_locations_lat_lng UNIQUE (latitude, longitude)
);

CREATE TABLE IF NOT EXISTS weathers
(
    id            UUID PRIMARY KEY,
    forecasted_at TIMESTAMPTZ NOT NULL,
    forecast_at   TIMESTAMPTZ NOT NULL,
    sky_status    VARCHAR(20) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    location_id   UUID        NOT NULL,
    speed_ms      NUMERIC,
    as_word       VARCHAR(20) NOT NULL,
    current_pct   NUMERIC,
    compared_pct  NUMERIC,
    current_c     NUMERIC     NOT NULL,
    compared_c    NUMERIC,
    min_c         NUMERIC,
    max_c         NUMERIC,
    type          VARCHAR(20) NOT NULL,
    amount_mm     NUMERIC,
    probability   NUMERIC     NOT NULL,
    -- domain checks
    CONSTRAINT ck_weathers_sky_status CHECK (sky_status IN ('CLEAR', 'MOSTLY_CLOUDY', 'CLOUDY')),
    CONSTRAINT ck_weathers_as_word CHECK (as_word IN ('WEAK', 'MODERATE', 'STRONG')),
    CONSTRAINT ck_weathers_type CHECK (type IN ('NONE', 'RAIN', 'RAIN_SNOW', 'SNOW', 'SHOWER')),
    CONSTRAINT ck_weathers_prob_range  CHECK (probability >= 0 AND probability <= 1),
    CONSTRAINT ck_weathers_hum_range   CHECK (current_pct IS NULL OR (current_pct >= 0 AND current_pct <= 100)),
    CONSTRAINT ck_weathers_wind_nonneg CHECK (speed_ms IS NULL OR speed_ms >= 0),
    CONSTRAINT fk_weathers_location FOREIGN KEY (location_id) REFERENCES weather_locations(id) ON DELETE RESTRICT,
    CONSTRAINT uq_weathers_loc_target UNIQUE (location_id, forecast_at)
);

-- 3) User Profiles & OAuth
CREATE TABLE IF NOT EXISTS user_profiles
(
    user_id                 UUID PRIMARY KEY,
    gender                  VARCHAR(10) CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    birth_date              DATE,
    latitude                NUMERIC,
    longitude               NUMERIC,
    x                       INTEGER,
    y                       INTEGER,
    location_names          VARCHAR(255),
    temperature_sensitivity INT,
    CONSTRAINT fk_user_profiles_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 4) Clothes
CREATE TABLE IF NOT EXISTS clothes_attributes_def
(
    id            UUID PRIMARY KEY,
    name          VARCHAR(20) NOT NULL,
    select_values VARCHAR(255),
    created_at    TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS clothes
(
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL,
    name       VARCHAR(50) NOT NULL,
    image_url  TEXT        NOT NULL,
    type       VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    CONSTRAINT ck_clothes_type CHECK (
        type IN
        ('TOP', 'BOTTOM', 'DRESS', 'OUTER', 'UNDERWEAR', 'ACCESSORY', 'SHOES', 'SOCKS', 'HAT',
         'BAG', 'SCARF', 'ETC')
        ),
    CONSTRAINT fk_clothes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS clothes_attribute
(
    id            UUID PRIMARY KEY,
    clothes_id    UUID         NOT NULL,
    definition_id UUID         NOT NULL,
    value         VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ,
    CONSTRAINT fk_clothes_attr_clothes FOREIGN KEY (clothes_id) REFERENCES clothes (id) ON DELETE CASCADE,
    CONSTRAINT fk_clothes_attr_def FOREIGN KEY (definition_id) REFERENCES clothes_attributes_def (id)
);

-- 5) Feeds
CREATE TABLE IF NOT EXISTS feeds
(
    id            UUID PRIMARY KEY,
    content       TEXT        NOT NULL,
    like_count    BIGINT      NOT NULL DEFAULT 0,
    comment_count BIGINT      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ,
    author_id     UUID        NOT NULL,
    weather_id    UUID        NOT NULL,
    CONSTRAINT fk_feeds_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_feeds_weather FOREIGN KEY (weather_id) REFERENCES weathers (id)
);

CREATE TABLE IF NOT EXISTS comments
(
    id         UUID PRIMARY KEY,
    content    VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    author_id  UUID         NOT NULL,
    feed_id    UUID         NOT NULL,
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_feed FOREIGN KEY (feed_id) REFERENCES feeds (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS feed_likes
(
    id         UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    feed_id    UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    CONSTRAINT uq_feed_likes UNIQUE (feed_id, user_id),
    CONSTRAINT fk_feed_likes_feed FOREIGN KEY (feed_id) REFERENCES feeds (id) ON DELETE CASCADE,
    CONSTRAINT fk_feed_likes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS feed_clothes
(
    id         UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    feed_id    UUID        NOT NULL,
    clothes_id UUID        NOT NULL,
    CONSTRAINT fk_feed_clothes_feed FOREIGN KEY (feed_id) REFERENCES feeds (id) ON DELETE CASCADE,
    CONSTRAINT fk_feed_clothes_clothes FOREIGN KEY (clothes_id) REFERENCES clothes (id) ON DELETE CASCADE
);

-- 6) Recommendations
CREATE TABLE IF NOT EXISTS recommendations
(
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL,
    weather_id UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_recommendations_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_recommendations_weather FOREIGN KEY (weather_id) REFERENCES weathers (id)
);

CREATE TABLE IF NOT EXISTS recommendation_clothes
(
    id                UUID PRIMARY KEY,
    recommendation_id UUID NOT NULL,
    clothes_id        UUID NOT NULL,
    CONSTRAINT fk_rec_clothes_rec FOREIGN KEY (recommendation_id) REFERENCES recommendations (id) ON DELETE CASCADE,
    CONSTRAINT fk_rec_clothes_clothes FOREIGN KEY (clothes_id) REFERENCES clothes (id) ON DELETE CASCADE
);

-- 7) DirectMessages
CREATE TABLE IF NOT EXISTS direct_messages
(
    id          UUID PRIMARY KEY,
    content     TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    sender_id   UUID        NOT NULL,
    receiver_id UUID        NOT NULL,
    CONSTRAINT fk_dm_sender FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_dm_receiver FOREIGN KEY (receiver_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_dm_sender_receiver_diff CHECK (sender_id <> receiver_id)
);

-- 8) Follows
CREATE TABLE IF NOT EXISTS follows
(
    id           UUID PRIMARY KEY,
    follower_id  UUID        NOT NULL,
    following_id UUID        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_follows UNIQUE (follower_id, following_id),
    CONSTRAINT ck_follows_self CHECK (follower_id <> following_id),
    CONSTRAINT fk_follows_follower FOREIGN KEY (follower_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_follows_following FOREIGN KEY (following_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 9) Notifications
CREATE TABLE IF NOT EXISTS notifications
(
    id          UUID PRIMARY KEY,
    receiver_id UUID         NOT NULL,
    title       VARCHAR(50)  NOT NULL,
    content     VARCHAR(100) NOT NULL,
    level       VARCHAR(10)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_notifications_level CHECK (level IN ('INFO', 'WARNING', 'ERROR')),
    CONSTRAINT fk_notifications_receiver FOREIGN KEY (receiver_id) REFERENCES users (id) ON DELETE CASCADE
);
