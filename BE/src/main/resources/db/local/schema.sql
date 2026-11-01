CREATE TABLE age_classification (
    age_classification_id BIGINT PRIMARY KEY,
    classification_code VARCHAR(30) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    minimum_age INTEGER,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL
);

CREATE TABLE film_genre (
    genre_id BIGINT PRIMARY KEY,
    genre_code VARCHAR(30) NOT NULL,
    genre_name VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE film (
    film_id BIGINT PRIMARY KEY,
    film_code VARCHAR(40) NOT NULL,
    title_local VARCHAR(300) NOT NULL,
    title_original VARCHAR(300),
    synopsis CLOB,
    runtime_minutes INTEGER NOT NULL,
    age_classification_id BIGINT,
    poster_uri VARCHAR(1000),
    trailer_uri VARCHAR(1000),
    approval_status VARCHAR(20) NOT NULL
);

CREATE TABLE film_genre_map (
    film_id BIGINT NOT NULL,
    genre_id BIGINT NOT NULL,
    PRIMARY KEY (film_id, genre_id)
);

CREATE TABLE cinema (
    cinema_id BIGINT PRIMARY KEY,
    cinema_code VARCHAR(30) NOT NULL,
    cinema_name VARCHAR(200) NOT NULL,
    address_line VARCHAR(1000) NOT NULL,
    province_name VARCHAR(120),
    district_name VARCHAR(120),
    ward_name VARCHAR(120),
    phone VARCHAR(30),
    email VARCHAR(320),
    time_zone VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE room_type (
    room_type_id BIGINT PRIMARY KEY,
    type_code VARCHAR(30) NOT NULL,
    type_name VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE cinema_room (
    room_id BIGINT PRIMARY KEY,
    cinema_id BIGINT NOT NULL,
    room_type_id BIGINT NOT NULL,
    room_code VARCHAR(30) NOT NULL,
    room_name VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE film_version (
    film_version_id BIGINT PRIMARY KEY,
    film_id BIGINT NOT NULL,
    version_code VARCHAR(40) NOT NULL,
    format_code VARCHAR(40) NOT NULL,
    runtime_minutes INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE screening (
    screening_id BIGINT PRIMARY KEY,
    cinema_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    layout_id BIGINT NOT NULL,
    film_id BIGINT NOT NULL,
    film_version_id BIGINT NOT NULL,
    room_type_code_snapshot VARCHAR(30) NOT NULL,
    room_type_name_snapshot VARCHAR(120) NOT NULL,
    format_code VARCHAR(40) NOT NULL,
    screening_type_code VARCHAR(40) NOT NULL,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE show_seat (
    show_seat_id BIGINT PRIMARY KEY,
    screening_id BIGINT NOT NULL,
    room_seat_id BIGINT NOT NULL,
    seat_status VARCHAR(20) NOT NULL
);