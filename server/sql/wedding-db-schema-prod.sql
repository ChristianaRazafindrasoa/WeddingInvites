DROP DATABASE IF EXISTS wedding_db;
CREATE DATABASE wedding_db;
USE wedding_db;

CREATE TABLE wedding_info (
    wedding_id INT AUTO_INCREMENT PRIMARY KEY,
    groom_name VARCHAR(255) NOT NULL,
    bride_name VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    `date` DATE NOT NULL
);

CREATE TABLE guest (
    guest_id INT AUTO_INCREMENT PRIMARY KEY,
    wedding_id INT NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    phone VARCHAR(255),
    hasPlusOne BOOLEAN,
    isConfirmed BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (wedding_id)
        REFERENCES wedding_info(wedding_id)
);

CREATE TABLE rsvp (
    rsvp_id INT AUTO_INCREMENT PRIMARY KEY,
    wedding_id INT NOT NULL,
    main_guest_id INT NOT NULL,
    plus_one_id INT,
    FOREIGN KEY (wedding_id)
        REFERENCES wedding_info(wedding_id),
	FOREIGN KEY (main_guest_id)
        REFERENCES guest(guest_id),
	FOREIGN KEY (main_guest_id)
        REFERENCES guest(guest_id)
);
 
INSERT INTO wedding_info (groom_name, bride_name, city, `date`) VALUES
	('Nicholas', 'Christiana', 'Tampa, Florida', '2026-12-12');
    
INSERT INTO guest (wedding_id, `name`, phone, hasPlusOne, isConfirmed) VALUES
	(1, 'Foo Test', '1234567890', true, false),
    (1, 'Bar Test', null, false, false),
    (1, 'Test McTest', '9876543210', false, false);
    
INSERT INTO rsvp (wedding_id, main_guest_id, plus_one_id) VALUES
	(1, 1, 2),
    (1, 3, null);   