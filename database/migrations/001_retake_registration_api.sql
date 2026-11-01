SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF OBJECT_ID(N'dbo.ecourse_sections', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ecourse_sections (
        id uniqueidentifier NOT NULL CONSTRAINT PK_ecourse_sections PRIMARY KEY,
        course_id uniqueidentifier NOT NULL,
        semester_id uniqueidentifier NOT NULL,
        code nvarchar(50) NOT NULL,
        course_code nvarchar(100) NOT NULL,
        course_name nvarchar(255) NOT NULL,
        credits int NOT NULL,
        max_capacity int NOT NULL,
        current_enrolled int NOT NULL CONSTRAINT DF_ecourse_sections_current_enrolled DEFAULT (0),
        status tinyint NOT NULL CONSTRAINT DF_ecourse_sections_status DEFAULT (1),
        row_version bigint NOT NULL CONSTRAINT DF_ecourse_sections_row_version DEFAULT (0),
        created_at datetime2(7) NOT NULL CONSTRAINT DF_ecourse_sections_created_at DEFAULT (SYSDATETIME()),
        updated_at datetime2(7) NULL,
        deleted_at datetime2(7) NULL,
        CONSTRAINT CK_ecourse_sections_credits CHECK (credits > 0),
        CONSTRAINT CK_ecourse_sections_capacity CHECK (max_capacity > 0 AND current_enrolled >= 0 AND current_enrolled <= max_capacity),
        CONSTRAINT CK_ecourse_sections_status CHECK (status IN (1, 2, 3)),
        CONSTRAINT UQ_ecourse_sections_semester_code UNIQUE (semester_id, code),
        CONSTRAINT FK_ecourse_sections_course FOREIGN KEY (course_id) REFERENCES dbo.courses(id),
        CONSTRAINT FK_ecourse_sections_semester FOREIGN KEY (semester_id) REFERENCES dbo.semesters(id)
    );
END;

IF COL_LENGTH(N'dbo.registration_periods', N'is_open') IS NULL
BEGIN
    ALTER TABLE dbo.registration_periods
        ADD is_open bit NOT NULL
            CONSTRAINT DF_registration_periods_is_open DEFAULT (1);
END;

IF OBJECT_ID(N'dbo.list_students', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.list_students (
        id uniqueidentifier NOT NULL CONSTRAINT PK_list_students PRIMARY KEY,
        registration_period_id uniqueidentifier NOT NULL,
        student_id uniqueidentifier NOT NULL,
        max_allowed_credits int NULL,
        is_active bit NOT NULL CONSTRAINT DF_list_students_is_active DEFAULT (1),
        created_at datetime2(7) NOT NULL CONSTRAINT DF_list_students_created_at DEFAULT (SYSDATETIME()),
        deleted_at datetime2(7) NULL,
        CONSTRAINT FK_list_students_period FOREIGN KEY (registration_period_id) REFERENCES dbo.registration_periods(id),
        CONSTRAINT FK_list_students_student FOREIGN KEY (student_id) REFERENCES dbo.students(id)
    );
    CREATE UNIQUE INDEX UX_list_students_period_student_active
        ON dbo.list_students(registration_period_id, student_id)
        WHERE deleted_at IS NULL AND is_active = 1;
END;

IF OBJECT_ID(N'dbo.manager_students', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.manager_students (
        id uniqueidentifier NOT NULL CONSTRAINT PK_manager_students PRIMARY KEY,
        student_id uniqueidentifier NOT NULL,
        manager_id uniqueidentifier NOT NULL,
        role_type tinyint NOT NULL,
        is_primary bit NOT NULL CONSTRAINT DF_manager_students_is_primary DEFAULT (0),
        created_at datetime2(7) NOT NULL CONSTRAINT DF_manager_students_created_at DEFAULT (SYSDATETIME()),
        deleted_at datetime2(7) NULL,
        CONSTRAINT CK_manager_students_role_type CHECK (role_type IN (1, 2)),
        CONSTRAINT FK_manager_students_student FOREIGN KEY (student_id) REFERENCES dbo.students(id),
        CONSTRAINT FK_manager_students_employee FOREIGN KEY (manager_id) REFERENCES dbo.employees(id)
    );
    CREATE UNIQUE INDEX UX_manager_students_primary_active
        ON dbo.manager_students(student_id)
        WHERE role_type = 1 AND is_primary = 1 AND deleted_at IS NULL;
END;

IF COL_LENGTH(N'dbo.course_registrations', N'ecourse_section_id') IS NULL
BEGIN
    ALTER TABLE dbo.course_registrations ADD ecourse_section_id uniqueidentifier NULL;
END;

IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.course_registrations')
      AND name = N'course_class_id'
      AND is_nullable = 0
)
BEGIN
    ALTER TABLE dbo.course_registrations
        ALTER COLUMN course_class_id uniqueidentifier NULL;
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = N'FK_course_registrations_ecourse_section'
)
BEGIN
    ALTER TABLE dbo.course_registrations WITH CHECK
        ADD CONSTRAINT FK_course_registrations_ecourse_section
        FOREIGN KEY (ecourse_section_id) REFERENCES dbo.ecourse_sections(id);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.course_registrations')
      AND name = N'IX_course_registrations_ecourse_section'
)
BEGIN
    CREATE INDEX IX_course_registrations_ecourse_section
        ON dbo.course_registrations(ecourse_section_id, registration_period_id, student_id, status)
        WHERE deleted_at IS NULL;
END;

COMMIT TRANSACTION;
