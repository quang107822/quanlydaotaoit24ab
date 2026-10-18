-- Seed 1000 sample users for group I (users table).
-- Run in DataGrip against: quanlisinhvien
-- Password hash below is the same bcrypt sample already used by existing rows.
-- Script is idempotent: usernames sampleuser0001..sampleuser1000 are skipped if they already exist.

USE [quanlisinhvien];
GO

SET NOCOUNT ON;

;WITH N AS (
    SELECT TOP (1000)
           ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n
    FROM sys.all_objects a
    CROSS JOIN sys.all_objects b
), Sample AS (
    SELECT
        n,
        CONCAT('sampleuser', RIGHT('0000' + CAST(n AS varchar(4)), 4)) AS username,
        CONCAT('sampleuser', RIGHT('0000' + CAST(n AS varchar(4)), 4), '@uda.edu.vn') AS email,
        CONCAT(N'Người dùng mẫu ', n) AS full_name,
        CONCAT('09', RIGHT('00000000' + CAST(n AS varchar(8)), 8)) AS phone
    FROM N
)
INSERT INTO dbo.users
    (id, username, password_hash, full_name, email, phone, avatar_url,
     last_login_at, created_at, updated_at, created_by, updated_by,
     deleted_at, deleted_by, is_active)
SELECT
    NEWID(),
    s.username,
    '$2a$10$7EqJtq98hPqEX7fNZaFWoO7GQ7q4Y9h2q5Y8z9fQj7Y4K3j7n4l6G',
    s.full_name,
    s.email,
    s.phone,
    NULL,
    NULL,
    SYSDATETIME(),
    SYSDATETIME(),
    NULL, NULL, NULL, NULL,
    1
FROM Sample s
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.users u WHERE u.username = s.username
);

SELECT COUNT(*) AS total_users FROM dbo.users;
GO
