-- «Добрый Шкаф»: начальная схема БД (PostgreSQL)
-- Порядок: users -> listings -> listing_photos -> exchange_points -> point_item_types

-- 1. Пользователи
CREATE TABLE users (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email             VARCHAR(255) NOT NULL UNIQUE,
    phone             VARCHAR(20)  UNIQUE,
    password_hash     VARCHAR(255) NOT NULL,
    role              VARCHAR(20)  NOT NULL,
    name              VARCHAR(100),
    city              VARCHAR(100),
    avatar_url        VARCHAR(500),
    terms_accepted_at TIMESTAMPTZ  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT users_role_check CHECK (role IN ('USER', 'ORGANIZER'))
);

-- 2. Объявления (P2P)
CREATE TABLE listings (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    author_id       BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title           VARCHAR(100) NOT NULL,
    item_type       VARCHAR(20)  NOT NULL,
    category        VARCHAR(20)  NOT NULL,
    condition       VARCHAR(20)  NOT NULL,
    description     VARCHAR(500),
    exchange_format VARCHAR(20)  NOT NULL,
    weight_kg       NUMERIC(6,2) NOT NULL,
    city            VARCHAR(100) NOT NULL,
    telegram_url    VARCHAR(255),
    whatsapp_url    VARCHAR(255),
    max_url         VARCHAR(255),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    published_at    TIMESTAMPTZ,
    transferred_at  TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT listings_item_type_check
        CHECK (item_type IN ('CLOTHES', 'SHOES', 'TOYS', 'BOOKS', 'FURNITURE', 'ELECTRONICS')),
    CONSTRAINT listings_category_check
        CHECK (category IN ('CHILDREN', 'ADULT')),
    CONSTRAINT listings_condition_check
        CHECK (condition IN ('NEW', 'GOOD', 'NEEDS_REPAIR')),
    CONSTRAINT listings_exchange_format_check
        CHECK (exchange_format IN ('FREE', 'SWAP')),
    CONSTRAINT listings_status_check
        CHECK (status IN ('DRAFT', 'ACTIVE', 'TRANSFERRED', 'HIDDEN')),
    CONSTRAINT listings_weight_kg_check
        CHECK (weight_kg > 0),
    -- не черновик -> нужна хотя бы одна ссылка на мессенджер
    CONSTRAINT listings_contact_check
        CHECK (status = 'DRAFT'
               OR telegram_url IS NOT NULL
               OR whatsapp_url IS NOT NULL
               OR max_url IS NOT NULL),
    -- transferred_at заполнен тогда и только тогда, когда статус TRANSFERRED
    CONSTRAINT listings_transferred_check
        CHECK ((status = 'TRANSFERRED') = (transferred_at IS NOT NULL))
);

CREATE INDEX idx_listings_status_type_city ON listings (status, item_type, city);
CREATE INDEX idx_listings_author_status    ON listings (author_id, status);
CREATE INDEX idx_listings_published_at     ON listings (published_at DESC);

-- 3. Фото объявлений (до 5 штук: position 0..4)
CREATE TABLE listing_photos (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    listing_id BIGINT       NOT NULL REFERENCES listings (id) ON DELETE CASCADE,
    url        VARCHAR(500) NOT NULL,
    position   SMALLINT     NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT listing_photos_position_check CHECK (position BETWEEN 0 AND 4),
    CONSTRAINT listing_photos_listing_position_uk UNIQUE (listing_id, position)
);

-- 4. Пункты обмена (organizer_id UNIQUE: у организатора не более одного пункта)
CREATE TABLE exchange_points (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    organizer_id  BIGINT       UNIQUE REFERENCES users (id) ON DELETE SET NULL,
    name          VARCHAR(150) NOT NULL,
    address       VARCHAR(255) NOT NULL,
    city          VARCHAR(100) NOT NULL,
    latitude      NUMERIC(9,6) NOT NULL,
    longitude     NUMERIC(9,6) NOT NULL,
    working_hours VARCHAR(255) NOT NULL,
    phone         VARCHAR(20)  NOT NULL,
    description   TEXT,
    photo_url     VARCHAR(500),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT exchange_points_status_check
        CHECK (status IN ('ACTIVE', 'TEMPORARILY_CLOSED'))
);

CREATE INDEX idx_exchange_points_status_city ON exchange_points (status, city);

-- 5. Типы вещей, принимаемых пунктом
CREATE TABLE point_item_types (
    point_id  BIGINT      NOT NULL REFERENCES exchange_points (id) ON DELETE CASCADE,
    item_type VARCHAR(20) NOT NULL,
    PRIMARY KEY (point_id, item_type),
    CONSTRAINT point_item_types_item_type_check
        CHECK (item_type IN ('CLOTHES', 'SHOES', 'TOYS', 'BOOKS', 'FURNITURE', 'ELECTRONICS'))
);
