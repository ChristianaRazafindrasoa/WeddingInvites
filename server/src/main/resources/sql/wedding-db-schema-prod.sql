DROP DATABASE IF EXISTS wedding_db_prod;
CREATE DATABASE wedding_db_prod;
USE wedding_db_prod;

CREATE TABLE wedding_info (
    wedding_id INT AUTO_INCREMENT PRIMARY KEY,
    groom_name VARCHAR(255) NOT NULL,
    bride_name VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    `date` DATETIME NOT NULL
);

CREATE TABLE wedding_event (
	event_id INT AUTO_INCREMENT PRIMARY KEY,
    wedding_id INT DEFAULT 1,
    `name` VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    start_time DATETIME NOT NULL,
    FOREIGN KEY (wedding_id)
        REFERENCES wedding_info(wedding_id)
);

CREATE TABLE guest (
    guest_id INT AUTO_INCREMENT PRIMARY KEY,
    wedding_id INT DEFAULT 1,
    `name` VARCHAR(255) NOT NULL,
    phone VARCHAR(255),
    has_plus_one BOOLEAN,
    is_attending BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (wedding_id)
        REFERENCES wedding_info(wedding_id)
);

CREATE TABLE rsvp (
    rsvp_id INT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(50) NOT NULL UNIQUE,
    wedding_id INT DEFAULT 1,
    main_guest_id INT NOT NULL,
    plus_one_id INT,
    responded_at DATETIME,
    is_accepted BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (wedding_id)
        REFERENCES wedding_info(wedding_id),
	FOREIGN KEY (main_guest_id)
        REFERENCES guest(guest_id),
	FOREIGN KEY (plus_one_id)
        REFERENCES guest(guest_id)
);

CREATE TABLE photo (
	photo_id INT AUTO_INCREMENT PRIMARY KEY,
    wedding_id INT DEFAULT 1,
    s3_key VARCHAR(50) NOT NULL UNIQUE,
    uploaded_at DATETIME NOT NULL,
    uploaded_by VARCHAR(255) NOT NULL,
    is_approved BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (wedding_id)
        REFERENCES wedding_info(wedding_id)
);