-- V3__Add_booked_at_to_bookings.sql
-- Add semantic booked_at column matching JPA entity

ALTER TABLE bookings 
ADD COLUMN booked_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP AFTER created_at;

