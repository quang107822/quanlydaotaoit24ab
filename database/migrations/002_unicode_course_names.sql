SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF OBJECT_ID(N'dbo.ecourse_sections', N'U') IS NOT NULL
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

IF OBJECT_ID(N'dbo.ecourse_sections', N'U') IS NOT NULL
BEGIN
    UPDATE section
    SET course_name = course.name
    FROM dbo.ecourse_sections AS section
    INNER JOIN dbo.courses AS course ON course.id = section.course_id
    WHERE section.course_name LIKE N'%?%'
      AND course.name IS NOT NULL
      AND course.name NOT LIKE N'%?%';
END;

COMMIT TRANSACTION;
