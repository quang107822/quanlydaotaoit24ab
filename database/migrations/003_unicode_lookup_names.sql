SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF OBJECT_ID(N'dbo.semesters', N'U') IS NOT NULL
   AND COL_LENGTH(N'dbo.semesters', N'name') IS NOT NULL
   AND EXISTS (
       SELECT 1
       FROM sys.columns
       WHERE object_id = OBJECT_ID(N'dbo.semesters')
         AND name = N'name'
         AND system_type_id <> TYPE_ID(N'nvarchar')
   )
BEGIN
    ALTER TABLE dbo.semesters
        ALTER COLUMN name nvarchar(255) NULL;
END;

IF OBJECT_ID(N'dbo.students', N'U') IS NOT NULL
   AND COL_LENGTH(N'dbo.students', N'full_name') IS NOT NULL
   AND EXISTS (
       SELECT 1
       FROM sys.columns
       WHERE object_id = OBJECT_ID(N'dbo.students')
         AND name = N'full_name'
         AND system_type_id <> TYPE_ID(N'nvarchar')
   )
BEGIN
    ALTER TABLE dbo.students
        ALTER COLUMN full_name nvarchar(255) NULL;
END;

IF OBJECT_ID(N'dbo.ecourse_sections', N'U') IS NOT NULL
   AND COL_LENGTH(N'dbo.ecourse_sections', N'course_name') IS NOT NULL
   AND EXISTS (
       SELECT 1
       FROM sys.columns
       WHERE object_id = OBJECT_ID(N'dbo.ecourse_sections')
         AND name = N'course_name'
         AND system_type_id <> TYPE_ID(N'nvarchar')
   )
BEGIN
    ALTER TABLE dbo.ecourse_sections
        ALTER COLUMN course_name nvarchar(255) NOT NULL;
END;

COMMIT TRANSACTION;
