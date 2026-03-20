-- Normalize booking statuses to the supported enum set before JPA reads them.

UPDATE bookings
SET status = 'PENDING'
WHERE status IS NULL
   OR status NOT IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED');

ALTER TABLE bookings
MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
