SET SQL_SAFE_UPDATES = 0;

DELETE FROM rsvp;
ALTER TABLE rsvp AUTO_INCREMENT = 1;

DELETE FROM guest;
ALTER TABLE guest AUTO_INCREMENT = 1;

DELETE FROM wedding_info;
ALTER TABLE wedding_info AUTO_INCREMENT = 1;

INSERT INTO wedding_info (groom_name, bride_name, city, `date`) VALUES
	('Nicholas', 'Christiana', 'Tampa, Florida', '2026-12-12');
    
INSERT INTO guest (wedding_id, `name`, phone, has_plus_one, is_attending) VALUES
	(1, 'Foo Test', '1234567890', true, false),
    (1, 'Bar Test', null, false, false),
    (1, 'Test McTest', '9876543210', false, false);
    
INSERT INTO rsvp (token, wedding_id, main_guest_id, plus_one_id, responded_at, is_accepted) VALUES
	('abc123', 1, 1, 2, null, false),
    ('123abc', 1, 3, null, null, false);   
    
SET SQL_SAFE_UPDATES = 1;