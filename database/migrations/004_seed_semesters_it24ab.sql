SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF OBJECT_ID(N'dbo.semesters', N'U') IS NULL
BEGIN
    THROW 50001, 'dbo.semesters does not exist in the current database.', 1;
END;

INSERT INTO dbo.semesters (id, code, name, school_year_name, start_date, end_date, is_active)
SELECT seed.id, seed.code, seed.name, seed.school_year_name, seed.start_date, seed.end_date, 1
FROM (VALUES
    (CONVERT(uniqueidentifier, N'283ff6e3-1d7c-4ad6-82ad-0915105b4f89'), N'20231', N'Học kỳ 1 năm học 2023-2024', N'2023-2024', CONVERT(date, N'2023-08-14'), CONVERT(date, N'2024-01-14')),
    (CONVERT(uniqueidentifier, N'e2781188-cb2d-4c78-8454-301bc3fa11ae'), N'20232', N'Học kỳ 2 năm học 2023-2024', N'2023-2024', CONVERT(date, N'2024-01-15'), CONVERT(date, N'2024-07-31')),
    (CONVERT(uniqueidentifier, N'e8765906-7f86-4caf-a819-e1b1ef10dc17'), N'20241', N'Học kỳ 1 năm học 2024-2025', N'2024-2025', CONVERT(date, N'2024-08-12'), CONVERT(date, N'2025-01-12')),
    (CONVERT(uniqueidentifier, N'dcd82c2f-862f-4cd6-ad74-6259c1c59215'), N'20242', N'Học kỳ 2 năm học 2024-2025', N'2024-2025', CONVERT(date, N'2025-01-13'), CONVERT(date, N'2025-07-31')),
    (CONVERT(uniqueidentifier, N'ffe63b29-bcfb-460b-b1dc-c96a89de50ea'), N'20251', N'Học kỳ 1 năm học 2025-2026', N'2025-2026', CONVERT(date, N'2025-08-11'), CONVERT(date, N'2026-01-11')),
    (CONVERT(uniqueidentifier, N'e28288e5-93b7-4bca-80f4-0e9bc98a6e02'), N'20252', N'Học kỳ 2 năm học 2025-2026', N'2025-2026', CONVERT(date, N'2026-01-12'), CONVERT(date, N'2026-07-31')),
    (CONVERT(uniqueidentifier, N'932ba1e7-ca1b-4609-a015-f874f51c0c59'), N'20261', N'Học kỳ 1 năm học 2026-2027', N'2026-2027', CONVERT(date, N'2026-08-17'), CONVERT(date, N'2027-01-17')),
    (CONVERT(uniqueidentifier, N'69c72be2-f19c-4310-bdd5-9d5aa64e9f4b'), N'20262', N'Học kỳ 2 năm học 2026-2027', N'2026-2027', CONVERT(date, N'2027-01-18'), CONVERT(date, N'2027-07-31'))
) AS seed(id, code, name, school_year_name, start_date, end_date)
WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.semesters AS existing
    WHERE existing.code = seed.code OR existing.id = seed.id
);

COMMIT TRANSACTION;
