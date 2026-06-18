USE wedding_db_test;

SET SQL_SAFE_UPDATES = 0;

DELETE FROM rsvp;
ALTER TABLE rsvp AUTO_INCREMENT = 1;

DELETE FROM guest;
ALTER TABLE guest AUTO_INCREMENT = 1;

DELETE FROM photo;
ALTER TABLE photo AUTO_INCREMENT = 1;

DELETE FROM wedding_event;
ALTER TABLE wedding_event AUTO_INCREMENT = 1;

DELETE FROM wedding_info;
ALTER TABLE wedding_info AUTO_INCREMENT = 1;

INSERT INTO wedding_info (groom_name, bride_name, city, `date`) VALUES
	('Nicholas', 'Christiana', 'Tampa, Florida', '2026-08-16-00-00-00');
    
INSERT INTO wedding_event (wedding_id, `name`, location, address, start_time) VALUES
	(1, 'Ceremony', 'Amazing Church', '123 Mary Dr, Tampa', '2026-08-16-08-15-00'),
	(1, 'Dinner', 'Awesome Restaurant', '456 Mall Dr, Tampa', '2026-08-16-09-30-00');

INSERT INTO guest (wedding_id, `name`, phone, has_plus_one, is_attending) VALUES
	(1, 'Foo Test', '1234567890', true, false),
    (1, 'Bar Test', null, false, false),
    (1, 'Test McTest', '9876543210', false, false);
    
INSERT INTO rsvp (token, wedding_id, main_guest_id, plus_one_id, responded_at, is_accepted) VALUES
	('abc123', 1, 1, 2, null, false),
    ('123abc', 1, 3, null, null, false);   
    
INSERT INTO photo (wedding_id, s3_key, uploaded_at, uploaded_by, is_approved) VALUES
	(1, 'test/photo1.jpg', '2026-06-15-09-30-00', 'Nicholas & Christiana', true),
	(1, 'test/photo2.jpg', '2026-06-15-09-31-00', 'Test McTest', true),
	(1, 'test/photo3.jpg', '2026-06-15-09-32-00', 'Foo & Bar Test', true);
    
SET SQL_SAFE_UPDATES = 1;