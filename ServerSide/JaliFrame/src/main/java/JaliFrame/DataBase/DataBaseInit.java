package JaliFrame.DataBase;

public class DataBaseInit {

    private static final String INIT_SCHEMA = "INIT_DATABASE";
    private static final String SECURITY_SCHEMA = "USERS_DATA_AND_PERMISSIONS";

    /*
     * Stores INSERT/UPDATE statements for objects that are registered
     * during application startup.
     */
    private static final StringBuilder queriesWaitingList = new StringBuilder();

    private DataBaseInit() {
        // Utility class
    }

    public static void initBasicDataBaseActions() {

        /*
         * 1. Make sure required schemas exist.
         */
        initSchemas();

        /*
         * 2. Make sure core tables exist and have the expected structure.
         */
        initUserTableAndSchemas();
        initObjectsTable();

        /*
         * 3. Create stored procedures.
         */
        initIsAuthenticatedSP();
        initIsAllowedSP();
        initLoginSP();
        initMakeUser();
        initGetUserCodeFromTokenFunc();

        /*
         * 4. Initialize USERS table and preserve/create ADMIN.
         */
        dataBaseUtils.runStaticQuery(
                "EXEC " + INIT_SCHEMA + ".SET_UP_SYS_USERS_TABLE;"
        );

        /*
         * 5. Initialize SYS_OBJECTS and preserve LOGIN.
         */
        dataBaseUtils.runStaticQuery(
                "EXEC " + INIT_SCHEMA + ".SET_UP_SYS_OBJECTS_TABLE;"
        );

        /*
         * 6. Remove old dynamically registered objects.
         *
         * LOGIN (OBJECT_CODE = 1) is preserved.
         */
        clearPageObjectNumbers();

        /*
         * 7. Insert/update objects registered by the application.
         */
        runWaitingObjectQueries();

        /*
         * 8. NOW initialize/synchronize the permission matrix.
         *
         * This is important:
         * permissions must be generated AFTER SYS_OBJECTS has been populated.
         */
        dataBaseUtils.runStaticQuery(
                "EXEC " + INIT_SCHEMA + ".SET_UP_OBJECT_USER_PERMISSION_TABLE;"
        );
    }
    
        /**
     * Called by mainServerLaunch AFTER all page routes are registered.
     *
     * Flushes the queued object-registration SQL that
     * WebPagesEnum.registerRoute() populated, then re-syncs the
     * permission matrix so every user gets a row for every object
     * and ADMIN is force-granted all four flags.
     */
    public static void finalizeRegistration() {

        // 1. Insert / update the queued objects into SYS_OBJECTS
        runWaitingObjectQueries();

        // 2. Re-sync the permission matrix (creates one row per user × object)
        dataBaseUtils.runStaticQuery(
            "EXEC " + INIT_SCHEMA + ".SET_UP_OBJECT_USER_PERMISSION_TABLE;"
        );

        // 3. Defensive — force-grant everything to ADMIN (USER_CODE = 1)
        dataBaseUtils.runStaticQuery(
            "UPDATE P " +
            "SET CAN_READ = 1, " +
            "    CAN_CREATE = 1, " +
            "    CAN_UPDATE = 1, " +
            "    CAN_DELETE = 1 " +
            "FROM USERS_DATA_AND_PERMISSIONS.OBJECT_USER_PERMISSION P " +
            "WHERE P.USER_CODE = 1;"
        );
    }
    
    /**
     * Creates the required schemas only if they do not already exist.
     */
    private static void initSchemas() {

        dataBaseUtils.runStaticQuery(
                "IF SCHEMA_ID(N'" + INIT_SCHEMA + "') IS NULL " +
                "EXEC(N'CREATE SCHEMA " + INIT_SCHEMA + "');"
        );

        dataBaseUtils.runStaticQuery(
                "IF SCHEMA_ID(N'" + SECURITY_SCHEMA + "') IS NULL " +
                "EXEC(N'CREATE SCHEMA " + SECURITY_SCHEMA + "');"
        );
    }

    /**
     * Executes all object registration queries collected during startup.
     */
    private static void runWaitingObjectQueries() {

        if (queriesWaitingList.length() == 0) {
            return;
        }

        dataBaseUtils.runStaticQuery(
                queriesWaitingList.toString()
        );

        /*
         * Prevent old registrations from being executed again
         * on the next initialization.
         */
        queriesWaitingList.setLength(0);
    }

    /**
     * Removes dynamically registered objects while preserving LOGIN.
     *
     * OBJECT_CODE = 1 is reserved for LOGIN.
     */
    private static void clearPageObjectNumbers() {

        /*
         * Remove stale permissions first.
         */
        dataBaseUtils.runStaticQuery(
                "DELETE FROM " + SECURITY_SCHEMA +
                ".OBJECT_USER_PERMISSION " +
                "WHERE OBJECT_CODE <> 1;"
        );

        /*
         * Remove dynamically registered objects.
         */
        dataBaseUtils.runStaticQuery(
                "DELETE FROM " + SECURITY_SCHEMA +
                ".SYS_OBJECTS " +
                "WHERE OBJECT_CODE <> 1;"
        );
    }

    /**
     * Registers an application object.
     *
     * If the object already exists, its title is updated and it is
     * marked as active instead of attempting another INSERT.
     */
    public static void loadObjectIntoObjectList(
            int objectCode,
            String objectName
    ) {

        if (objectName == null) {
            throw new IllegalArgumentException("objectName cannot be null.");
        }

        /*
         * Escape single quotes for SQL.
         *
         * Example:
         * O'Brien -> O''Brien
         */
        String escapedObjectName = objectName.replace("'", "''");

        queriesWaitingList
                .append("IF EXISTS (")
                .append("SELECT 1 FROM ")
                .append(SECURITY_SCHEMA)
                .append(".SYS_OBJECTS ")
                .append("WHERE OBJECT_CODE = ")
                .append(objectCode)
                .append(")")
                .append("\nBEGIN")
                .append("\n")
                .append("    UPDATE ")
                .append(SECURITY_SCHEMA)
                .append(".SYS_OBJECTS")
                .append("\n")
                .append("    SET OBJECT_TITLE = N'")
                .append(escapedObjectName)
                .append("',")
                .append(" IS_DELETED = 0")
                .append("\n")
                .append("    WHERE OBJECT_CODE = ")
                .append(objectCode)
                .append(";")
                .append("\nEND")
                .append("\nELSE")
                .append("\nBEGIN")
                .append("\n")
                .append("    INSERT INTO ")
                .append(SECURITY_SCHEMA)
                .append(".SYS_OBJECTS")
                .append(" (OBJECT_CODE, OBJECT_TITLE, IS_DELETED)")
                .append("\n")
                .append("    VALUES (")
                .append(objectCode)
                .append(", N'")
                .append(escapedObjectName)
                .append("', 0);")
                .append("\nEND")
                .append("\n\n");
    }

    /**
     * Creates authentication procedure.
     */
    private static void initIsAuthenticatedSP() {

        dataBaseUtils.runStaticQuery(
                "CREATE OR ALTER PROCEDURE " + SECURITY_SCHEMA + ".IS_AUTHENTICATE\n" +
                "@TOKEN NVARCHAR(512)\n" +
                "AS\n" +
                "BEGIN\n" +
                "    SET NOCOUNT ON;\n" +
                "\n" +
                "    IF EXISTS (\n" +
                "        SELECT 1\n" +
                "        FROM " + SECURITY_SCHEMA + ".SYS_USERS\n" +
                "        WHERE SYS_LOGIN_SESSION = @TOKEN\n" +
                "    )\n" +
                "        SELECT 1 AS STATUS;\n" +
                "    ELSE\n" +
                "        SELECT 0 AS STATUS;\n" +
                "END;"
        );
    }

    /**
     * Creates permission-check procedures.
     */
    private static void initIsAllowedSP() {

        dataBaseUtils.runStaticQuery(
                "CREATE OR ALTER PROCEDURE " + SECURITY_SCHEMA + ".IS_ALLOWED_READ\n" +
                "@USER_CODE NUMERIC(18,0),\n" +
                "@OBJECT_CODE NUMERIC(18,0)\n" +
                "AS\n" +
                "BEGIN\n" +
                "    SET NOCOUNT ON;\n" +
                "\n" +
                "    SELECT CASE\n" +
                "        WHEN EXISTS (\n" +
                "            SELECT 1\n" +
                "            FROM " + SECURITY_SCHEMA + ".OBJECT_USER_PERMISSION\n" +
                "            WHERE USER_CODE = @USER_CODE\n" +
                "              AND OBJECT_CODE = @OBJECT_CODE\n" +
                "              AND CAN_READ = 1\n" +
                "        ) THEN 1\n" +
                "        ELSE 0\n" +
                "    END AS STATUS;\n" +
                "END;"
        );

        dataBaseUtils.runStaticQuery(
                "CREATE OR ALTER PROCEDURE " + SECURITY_SCHEMA + ".IS_ALLOWED_CREATE\n" +
                "@USER_CODE NUMERIC(18,0),\n" +
                "@OBJECT_CODE NUMERIC(18,0)\n" +
                "AS\n" +
                "BEGIN\n" +
                "    SET NOCOUNT ON;\n" +
                "\n" +
                "    SELECT CASE\n" +
                "        WHEN EXISTS (\n" +
                "            SELECT 1\n" +
                "            FROM " + SECURITY_SCHEMA + ".OBJECT_USER_PERMISSION\n" +
                "            WHERE USER_CODE = @USER_CODE\n" +
                "              AND OBJECT_CODE = @OBJECT_CODE\n" +
                "              AND CAN_CREATE = 1\n" +
                "        ) THEN 1\n" +
                "        ELSE 0\n" +
                "    END AS STATUS;\n" +
                "END;"
        );

        dataBaseUtils.runStaticQuery(
                "CREATE OR ALTER PROCEDURE " + SECURITY_SCHEMA + ".IS_ALLOWED_UPDATE\n" +
                "@USER_CODE NUMERIC(18,0),\n" +
                "@OBJECT_CODE NUMERIC(18,0)\n" +
                "AS\n" +
                "BEGIN\n" +
                "    SET NOCOUNT ON;\n" +
                "\n" +
                "    SELECT CASE\n" +
                "        WHEN EXISTS (\n" +
                "            SELECT 1\n" +
                "            FROM " + SECURITY_SCHEMA + ".OBJECT_USER_PERMISSION\n" +
                "            WHERE USER_CODE = @USER_CODE\n" +
                "              AND OBJECT_CODE = @OBJECT_CODE\n" +
                "              AND CAN_UPDATE = 1\n" +
                "        ) THEN 1\n" +
                "        ELSE 0\n" +
                "    END AS STATUS;\n" +
                "END;"
        );

        dataBaseUtils.runStaticQuery(
                "CREATE OR ALTER PROCEDURE " + SECURITY_SCHEMA + ".IS_ALLOWED_DELETE\n" +
                "@USER_CODE NUMERIC(18,0),\n" +
                "@OBJECT_CODE NUMERIC(18,0)\n" +
                "AS\n" +
                "BEGIN\n" +
                "    SET NOCOUNT ON;\n" +
                "\n" +
                "    SELECT CASE\n" +
                "        WHEN EXISTS (\n" +
                "            SELECT 1\n" +
                "            FROM " + SECURITY_SCHEMA + ".OBJECT_USER_PERMISSION\n" +
                "            WHERE USER_CODE = @USER_CODE\n" +
                "              AND OBJECT_CODE = @OBJECT_CODE\n" +
                "              AND CAN_DELETE = 1\n" +
                "        ) THEN 1\n" +
                "        ELSE 0\n" +
                "    END AS STATUS;\n" +
                "END;"
        );
    }

    /**
     * Creates login procedure.
     *
     * IMPORTANT:
     * The original version had the authentication condition reversed.
     * It generated a token when the password was NOT correct.
     */
    private static void initLoginSP() {

        dataBaseUtils.runStaticQuery(
                "CREATE OR ALTER PROCEDURE " + SECURITY_SCHEMA + ".SHOULD_LOGIN\n" +
                "@USER_NAME NVARCHAR(50),\n" +
                "@USER_PASSWORD NVARCHAR(50)\n" +
                "AS\n" +
                "BEGIN\n" +
                "    SET NOCOUNT ON;\n" +
                "\n" +
                "    DECLARE @TOKEN VARCHAR(128);\n" +
                "\n" +
                "    IF EXISTS (\n" +
                "        SELECT 1\n" +
                "        FROM " + SECURITY_SCHEMA + ".SYS_USERS\n" +
                "        WHERE UPPER(SYS_USERNAME) = UPPER(@USER_NAME)\n" +
                "          AND SYS_PASSWORD = HASHBYTES(\n" +
                "              'SHA2_512',\n" +
                "              CONCAT(@USER_PASSWORD, SYS_PASSWORD_SALT)\n" +
                "          )\n" +
                "    )\n" +
                "    BEGIN\n" +
                "\n" +
                "        SET @TOKEN = CONVERT(\n" +
                "            VARCHAR(128),\n" +
                "            HASHBYTES(\n" +
                "                'SHA2_512',\n" +
                "                CONVERT(\n" +
                "                    VARCHAR(128),\n" +
                "                    CONCAT('DATA_', '_', NEWID())\n" +
                "                )\n" +
                "            ),\n" +
                "            2\n" +
                "        );\n" +
                "\n" +
                "        UPDATE " + SECURITY_SCHEMA + ".SYS_USERS\n" +
                "        SET SYS_LOGIN_SESSION = @TOKEN\n" +
                "        WHERE UPPER(SYS_USERNAME) = UPPER(@USER_NAME);\n" +
                "\n" +
                "        SELECT @TOKEN AS STATUS;\n" +
                "    END\n" +
                "    ELSE\n" +
                "    BEGIN\n" +
                "        SELECT 0 AS STATUS;\n" +
                "    END\n" +
                "END;"
        );
    }

    /**
     * Creates new user procedure.
     */
    private static void initMakeUser() {

        dataBaseUtils.runStaticQuery(
                "CREATE OR ALTER PROCEDURE " + SECURITY_SCHEMA + ".MAKE_NEW_USER\n" +
                "@USER_NAME NVARCHAR(50),\n" +
                "@USER_PASSWORD NVARCHAR(50),\n" +
                "@USER_CODE NUMERIC(18,0)\n" +
                "AS\n" +
                "BEGIN\n" +
                "    SET NOCOUNT ON;\n" +
                "\n" +
                "    DECLARE @SALT NVARCHAR(23) =\n" +
                "        CONVERT(NVARCHAR(23), GETDATE(), 121);\n" +
                "\n" +
                "    IF EXISTS (\n" +
                "        SELECT 1\n" +
                "        FROM " + SECURITY_SCHEMA + ".SYS_USERS\n" +
                "        WHERE UPPER(SYS_USERNAME) = UPPER(@USER_NAME)\n" +
                "           OR SYS_USER_CODE = @USER_CODE\n" +
                "    )\n" +
                "    BEGIN\n" +
                "        RETURN;\n" +
                "    END\n" +
                "\n" +
                "    INSERT INTO " + SECURITY_SCHEMA + ".SYS_USERS\n" +
                "    (\n" +
                "        SYS_USER_CODE,\n" +
                "        SYS_USERNAME,\n" +
                "        SYS_PASSWORD,\n" +
                "        SYS_PASSWORD_SALT\n" +
                "    )\n" +
                "    VALUES\n" +
                "    (\n" +
                "        @USER_CODE,\n" +
                "        @USER_NAME,\n" +
                "        HASHBYTES(\n" +
                "            'SHA2_512',\n" +
                "            CONCAT(@USER_PASSWORD, @SALT)\n" +
                "        ),\n" +
                "        @SALT\n" +
                "    );\n" +
                "END;"
        );
    }

    /**
     * Gets SYS_USER_CODE from a login token.
     */
    private static void initGetUserCodeFromTokenFunc() {

        dataBaseUtils.runStaticQuery(
                "CREATE OR ALTER PROCEDURE " + SECURITY_SCHEMA + ".GET_USER_ID_FROM_TOKEN\n" +
                "@TOKEN NVARCHAR(512)\n" +
                "AS\n" +
                "BEGIN\n" +
                "    SET NOCOUNT ON;\n" +
                "\n" +
                "    SELECT ISNULL(\n" +
                "        (\n" +
                "            SELECT SYS_USER_CODE\n" +
                "            FROM " + SECURITY_SCHEMA + ".SYS_USERS\n" +
                "            WHERE SYS_LOGIN_SESSION = @TOKEN\n" +
                "        ),\n" +
                "        -1\n" +
                "    ) AS SYS_USER_CODE;\n" +
                "END;"
        );
    }

    /**
     * Creates/synchronizes OBJECT_USER_PERMISSION.
     */
    private static void initObjectUserPermissionsTable() {

        dataBaseUtils.runStaticQuery(
                "CREATE OR ALTER PROCEDURE " + INIT_SCHEMA + ".SET_UP_OBJECT_USER_PERMISSION_TABLE\n" +
                "AS\n" +
                "BEGIN\n" +
                "    SET NOCOUNT ON;\n" +
                "\n" +
                "    DECLARE @DOES_TABLE_EXIST BIT = 0;\n" +
                "    DECLARE @DOES_ID_EXIST BIT = 0;\n" +
                "    DECLARE @DOES_USER_CODE_EXIST BIT = 0;\n" +
                "    DECLARE @DOES_OBJECT_CODE_EXIST BIT = 0;\n" +
                "    DECLARE @DOES_CAN_READ_EXIST BIT = 0;\n" +
                "    DECLARE @DOES_CAN_CREATE_EXIST BIT = 0;\n" +
                "    DECLARE @DOES_CAN_UPDATE_EXIST BIT = 0;\n" +
                "    DECLARE @DOES_CAN_DELETE_EXIST BIT = 0;\n" +
                "\n" +
                "    IF OBJECT_ID(N'" + SECURITY_SCHEMA + ".OBJECT_USER_PERMISSION', 'U') IS NOT NULL\n" +
                "    BEGIN\n" +
                "        SET @DOES_TABLE_EXIST = 1;\n" +
                "\n" +
                "        IF EXISTS (\n" +
                "            SELECT 1\n" +
                "            FROM sys.columns c\n" +
                "            JOIN sys.tables t ON c.object_id = t.object_id\n" +
                "            JOIN sys.schemas s ON t.schema_id = s.schema_id\n" +
                "            WHERE s.name = N'" + SECURITY_SCHEMA + "'\n" +
                "              AND t.name = N'OBJECT_USER_PERMISSION'\n" +
                "              AND c.name = N'OBJECT_USER_PERMISSION'\n" +
                "              AND c.is_identity = 1\n" +
                "              AND c.is_nullable = 0\n" +
                "        )\n" +
                "            SET @DOES_ID_EXIST = 1;\n" +
                "\n" +
                "        IF EXISTS (\n" +
                "            SELECT 1\n" +
                "            FROM sys.columns c\n" +
                "            JOIN sys.tables t ON c.object_id = t.object_id\n" +
                "            JOIN sys.schemas s ON t.schema_id = s.schema_id\n" +
                "            JOIN sys.types ty ON c.user_type_id = ty.user_type_id\n" +
                "            WHERE s.name = N'" + SECURITY_SCHEMA + "'\n" +
                "              AND t.name = N'OBJECT_USER_PERMISSION'\n" +
                "              AND c.name = N'USER_CODE'\n" +
                "              AND ty.name IN (N'numeric', N'int')\n" +
                "              AND c.is_nullable = 0\n" +
                "        )\n" +
                "            SET @DOES_USER_CODE_EXIST = 1;\n" +
                "\n" +
                "        IF EXISTS (\n" +
                "            SELECT 1\n" +
                "            FROM sys.columns c\n" +
                "            JOIN sys.tables t ON c.object_id = t.object_id\n" +
                "            JOIN sys.schemas s ON t.schema_id = s.schema_id\n" +
                "            JOIN sys.types ty ON c.user_type_id = ty.user_type_id\n" +
                "            WHERE s.name = N'" + SECURITY_SCHEMA + "'\n" +
                "              AND t.name = N'OBJECT_USER_PERMISSION'\n" +
                "              AND c.name = N'OBJECT_CODE'\n" +
                "              AND ty.name IN (N'numeric', N'int')\n" +
                "              AND c.is_nullable = 0\n" +
                "        )\n" +
                "            SET @DOES_OBJECT_CODE_EXIST = 1;\n" +
                "\n" +
                "        IF EXISTS (\n" +
                "            SELECT 1\n" +
                "            FROM sys.columns c\n" +
                "            JOIN sys.tables t ON c.object_id = t.object_id\n" +
                "            JOIN sys.schemas s ON t.schema_id = s.schema_id\n" +
                "            JOIN sys.types ty ON c.user_type_id = ty.user_type_id\n" +
                "            WHERE s.name = N'" + SECURITY_SCHEMA + "'\n" +
                "              AND t.name = N'OBJECT_USER_PERMISSION'\n" +
                "              AND c.name = N'CAN_READ'\n" +
                "              AND ty.name = N'bit'\n" +
                "              AND c.is_nullable = 0\n" +
                "        )\n" +
                "            SET @DOES_CAN_READ_EXIST = 1;\n" +
                "\n" +
                "        IF EXISTS (\n" +
                "            SELECT 1\n" +
                "            FROM sys.columns c\n" +
                "            JOIN sys.tables t ON c.object_id = t.object_id\n" +
                "            JOIN sys.schemas s ON t.schema_id = s.schema_id\n" +
                "            JOIN sys.types ty ON c.user_type_id = ty.user_type_id\n" +
                "            WHERE s.name = N'" + SECURITY_SCHEMA + "'\n" +
                "              AND t.name = N'OBJECT_USER_PERMISSION'\n" +
                "              AND c.name = N'CAN_CREATE'\n" +
                "              AND ty.name = N'bit'\n" +
                "              AND c.is_nullable = 0\n" +
                "        )\n" +
                "            SET @DOES_CAN_CREATE_EXIST = 1;\n" +
                "\n" +
                "        IF EXISTS (\n" +
                "            SELECT 1\n" +
                "            FROM sys.columns c\n" +
                "            JOIN sys.tables t ON c.object_id = t.object_id\n" +
                "            JOIN sys.schemas s ON t.schema_id = s.schema_id\n" +
                "            JOIN sys.types ty ON c.user_type_id = ty.user_type_id\n" +
                "            WHERE s.name = N'" + SECURITY_SCHEMA + "'\n" +
                "              AND t.name = N'OBJECT_USER_PERMISSION'\n" +
                "              AND c.name = N'CAN_UPDATE'\n" +
                "              AND ty.name = N'bit'\n" +
                "              AND c.is_nullable = 0\n" +
                "        )\n" +
                "            SET @DOES_CAN_UPDATE_EXIST = 1;\n" +
                "\n" +
                "        IF EXISTS (\n" +
                "            SELECT 1\n" +
                "            FROM sys.columns c\n" +
                "            JOIN sys.tables t ON c.object_id = t.object_id\n" +
                "            JOIN sys.schemas s ON t.schema_id = s.schema_id\n" +
                "            JOIN sys.types ty ON c.user_type_id = ty.user_type_id\n" +
                "            WHERE s.name = N'" + SECURITY_SCHEMA + "'\n" +
                "              AND t.name = N'OBJECT_USER_PERMISSION'\n" +
                "              AND c.name = N'CAN_DELETE'\n" +
                "              AND ty.name = N'bit'\n" +
                "              AND c.is_nullable = 0\n" +
                "        )\n" +
                "            SET @DOES_CAN_DELETE_EXIST = 1;\n" +
                "\n" +
                "        IF @DOES_ID_EXIST = 0\n" +
                "           OR @DOES_USER_CODE_EXIST = 0\n" +
                "           OR @DOES_OBJECT_CODE_EXIST = 0\n" +
                "           OR @DOES_CAN_READ_EXIST = 0\n" +
                "           OR @DOES_CAN_CREATE_EXIST = 0\n" +
                "           OR @DOES_CAN_UPDATE_EXIST = 0\n" +
                "           OR @DOES_CAN_DELETE_EXIST = 0\n" +
                "        BEGIN\n" +
                "            DROP TABLE " + SECURITY_SCHEMA + ".OBJECT_USER_PERMISSION;\n" +
                "            SET @DOES_TABLE_EXIST = 0;\n" +
                "        END\n" +
                "    END\n" +
                "\n" +
                "    IF @DOES_TABLE_EXIST = 0\n" +
                "    BEGIN\n" +
                "        CREATE TABLE " + SECURITY_SCHEMA + ".OBJECT_USER_PERMISSION\n" +
                "        (\n" +
                "            OBJECT_USER_PERMISSION NUMERIC(18,0) IDENTITY(1,1) PRIMARY KEY,\n" +
                "            USER_CODE NUMERIC(18,0) NOT NULL,\n" +
                "            OBJECT_CODE NUMERIC(18,0) NOT NULL,\n" +
                "            CAN_READ BIT NOT NULL DEFAULT 0,\n" +
                "            CAN_CREATE BIT NOT NULL DEFAULT 0,\n" +
                "            CAN_UPDATE BIT NOT NULL DEFAULT 0,\n" +
                "            CAN_DELETE BIT NOT NULL DEFAULT 0,\n" +
                "            CONSTRAINT UQ_OBJECT_USER_PERMISSION\n" +
                "                UNIQUE (USER_CODE, OBJECT_CODE)\n" +
                "        );\n" +
                "    END\n" +
                "\n" +
                "    INSERT INTO " + SECURITY_SCHEMA + ".OBJECT_USER_PERMISSION\n" +
                "    (\n" +
                "        USER_CODE,\n" +
                "        OBJECT_CODE\n" +
                "    )\n" +
                "    SELECT\n" +
                "        U.SYS_USER_CODE,\n" +
                "        O.OBJECT_CODE\n" +
                "    FROM " + SECURITY_SCHEMA + ".SYS_USERS U\n" +
                "    CROSS JOIN " + SECURITY_SCHEMA + ".SYS_OBJECTS O\n" +
                "    WHERE NOT EXISTS\n" +
                "    (\n" +
                "        SELECT 1\n" +
                "        FROM " + SECURITY_SCHEMA + ".OBJECT_USER_PERMISSION P\n" +
                "        WHERE P.USER_CODE = U.SYS_USER_CODE\n" +
                "          AND P.OBJECT_CODE = O.OBJECT_CODE\n" +
                "    );\n" +
                "\n" +
                "    UPDATE P\n" +
                "    SET\n" +
                "        CAN_READ = 1,\n" +
                "        CAN_CREATE = 1,\n" +
                "        CAN_UPDATE = 1,\n" +
                "        CAN_DELETE = 1\n" +
                "    FROM " + SECURITY_SCHEMA + ".OBJECT_USER_PERMISSION P\n" +
                "    INNER JOIN " + SECURITY_SCHEMA + ".SYS_USERS U\n" +
                "        ON P.USER_CODE = U.SYS_USER_CODE\n" +
                "    WHERE U.SYS_USER_CODE = 1;\n" +
                "\n" +
                "END;"
        );
    }

    /**
     * Creates/synchronizes SYS_OBJECTS.
     */
    private static void initObjectsTable() {

        dataBaseUtils.runStaticQuery(
                "CREATE OR ALTER PROCEDURE " + INIT_SCHEMA + ".SET_UP_SYS_OBJECTS_TABLE\n" +
                "AS\n" +
                "BEGIN\n" +
                "    SET NOCOUNT ON;\n" +
                "\n" +
                "    DECLARE @DOES_TABLE_EXIST BIT = 0;\n" +
                "    DECLARE @DOES_OBJECT_CODE_EXIST BIT = 0;\n" +
                "    DECLARE @DOES_OBJECT_TITLE_EXIST BIT = 0;\n" +
                "    DECLARE @DOES_IS_DELETED_EXIST BIT = 0;\n" +
                "    DECLARE @DOES_OBJECT_ID_EXIST BIT = 0;\n" +
                "\n" +
                "    IF OBJECT_ID(N'" + SECURITY_SCHEMA + ".SYS_OBJECTS', 'U') IS NOT NULL\n" +
                "    BEGIN\n" +
                "        SET @DOES_TABLE_EXIST = 1;\n" +
                "\n" +
                "        IF EXISTS (\n" +
                "            SELECT 1\n" +
                "            FROM sys.columns c\n" +
                "            JOIN sys.tables t ON c.object_id = t.object_id\n" +
                "            JOIN sys.schemas s ON t.schema_id = s.schema_id\n" +
                "            JOIN sys.types ty ON c.user_type_id = ty.user_type_id\n" +
                "            WHERE s.name = N'" + SECURITY_SCHEMA + "'\n" +
                "              AND t.name = N'SYS_OBJECTS'\n" +
                "              AND c.name = N'OBJECT_CODE'\n" +
                "              AND ty.name IN (N'numeric', N'int')\n" +
                "              AND c.is_nullable = 0\n" +
                "        )\n" +
                "            SET @DOES_OBJECT_CODE_EXIST = 1;\n" +
                "\n" +
                "        IF EXISTS (\n" +
                "            SELECT 1\n" +
                "            FROM sys.columns c\n" +
                "            JOIN sys.tables t ON c.object_id = t.object_id\n" +
                "            JOIN sys.schemas s ON t.schema_id = s.schema_id\n" +
                "            JOIN sys.types ty ON c.user_type_id = ty.user_type_id\n" +
                "            WHERE s.name = N'" + SECURITY_SCHEMA + "'\n" +
                "              AND t.name = N'SYS_OBJECTS'\n" +
                "              AND c.name = N'OBJECT_TITLE'\n" +
                "              AND ty.name IN (N'nvarchar', N'varchar')\n" +
                "              AND c.is_nullable = 0\n" +
                "        )\n" +
                "            SET @DOES_OBJECT_TITLE_EXIST = 1;\n" +
                "\n" +
                "        IF EXISTS (\n" +
                "            SELECT 1\n" +
                "            FROM sys.columns c\n" +
                "            JOIN sys.tables t ON c.object_id = t.object_id\n" +
                "            JOIN sys.schemas s ON t.schema_id = s.schema_id\n" +
                "            JOIN sys.types ty ON c.user_type_id = ty.user_type_id\n" +
                "            WHERE s.name = N'" + SECURITY_SCHEMA + "'\n" +
                "              AND t.name = N'SYS_OBJECTS'\n" +
                "              AND c.name = N'IS_DELETED'\n" +
                "              AND ty.name = N'bit'\n" +
                "              AND c.is_nullable = 0\n" +
                "        )\n" +
                "            SET @DOES_IS_DELETED_EXIST = 1;\n" +
                "\n" +
                "        IF EXISTS (\n" +
                "            SELECT 1\n" +
                "            FROM sys.columns c\n" +
                "            JOIN sys.tables t ON c.object_id = t.object_id\n" +
                "            JOIN sys.schemas s ON t.schema_id = s.schema_id\n" +
                "            JOIN sys.types ty ON c.user_type_id = ty.user_type_id\n" +
                "            WHERE s.name = N'" + SECURITY_SCHEMA + "'\n" +
                "              AND t.name = N'SYS_OBJECTS'\n" +
                "              AND c.name = N'OBJECT_ID'\n" +
                "              AND ty.name IN (N'numeric', N'int')\n" +
                "              AND c.is_identity = 1\n" +
                "              AND c.is_nullable = 0\n" +
                "        )\n" +
                "            SET @DOES_OBJECT_ID_EXIST = 1;\n" +
                "\n" +
                "        IF @DOES_OBJECT_CODE_EXIST = 0\n" +
                "           OR @DOES_OBJECT_TITLE_EXIST = 0\n" +
                "           OR @DOES_IS_DELETED_EXIST = 0\n" +
                "           OR @DOES_OBJECT_ID_EXIST = 0\n" +
                "        BEGIN\n" +
                "            DROP TABLE " + SECURITY_SCHEMA + ".SYS_OBJECTS;\n" +
                "            SET @DOES_TABLE_EXIST = 0;\n" +
                "        END\n" +
                "    END\n" +
                "\n" +
                "    IF @DOES_TABLE_EXIST = 0\n" +
                "    BEGIN\n" +
                "        CREATE TABLE " + SECURITY_SCHEMA + ".SYS_OBJECTS\n" +
                "        (\n" +
                "            OBJECT_ID NUMERIC(18,0) IDENTITY(1,1) PRIMARY KEY,\n" +
                "            OBJECT_CODE NUMERIC(18,0) NOT NULL UNIQUE,\n" +
                "            OBJECT_TITLE NVARCHAR(50) NOT NULL UNIQUE,\n" +
                "            IS_DELETED BIT NOT NULL DEFAULT 0\n" +
                "        );\n" +
                "    END\n" +
                "\n" +
                "    IF NOT EXISTS (\n" +
                "        SELECT 1\n" +
                "        FROM " + SECURITY_SCHEMA + ".SYS_OBJECTS\n" +
                "        WHERE OBJECT_CODE = 1\n" +
                "    )\n" +
                "    BEGIN\n" +
                "        INSERT INTO " + SECURITY_SCHEMA + ".SYS_OBJECTS\n" +
                "        (\n" +
                "            OBJECT_CODE,\n" +
                "            OBJECT_TITLE,\n" +
                "            IS_DELETED\n" +
                "        )\n" +
                "        VALUES\n" +
                "        (\n" +
                "            1,\n" +
                "            N'LOGIN',\n" +
                "            0\n" +
                "        );\n" +
                "    END\n" +
                "    ELSE\n" +
                "    BEGIN\n" +
                "        UPDATE " + SECURITY_SCHEMA + ".SYS_OBJECTS\n" +
                "        SET IS_DELETED = 0,\n" +
                "            OBJECT_TITLE = N'LOGIN'\n" +
                "        WHERE OBJECT_CODE = 1;\n" +
                "    END\n" +
                "END;"
        );
    }

    /**
     * Creates/synchronizes SYS_USERS.
     */
    private static void initUserTableAndSchemas() {

        dataBaseUtils.runStaticQuery(
                "CREATE OR ALTER PROCEDURE " + INIT_SCHEMA + ".SET_UP_SYS_USERS_TABLE\n" +
                "AS\n" +
                "BEGIN\n" +
                "    SET NOCOUNT ON;\n" +
                "\n" +
                "    DECLARE @DOES_TABLE_EXIST BIT = 0;\n" +
                "    DECLARE @DOES_ID_EXIST BIT = 0;\n" +
                "    DECLARE @DOES_CODE_EXIST BIT = 0;\n" +
                "    DECLARE @DOES_USERNAME_EXIST BIT = 0;\n" +
                "    DECLARE @DOES_PASSWORD_EXIST BIT = 0;\n" +
                "    DECLARE @DOES_SESSION_EXIST BIT = 0;\n" +
                "    DECLARE @DOES_PASSWORD_SALT_EXIST BIT = 0;\n" +
                "\n" +
                "    IF OBJECT_ID(N'" + SECURITY_SCHEMA + ".SYS_USERS', 'U') IS NOT NULL\n" +
                "    BEGIN\n" +
                "        SET @DOES_TABLE_EXIST = 1;\n" +
                "\n" +
                "        IF EXISTS (\n" +
                "            SELECT 1\n" +
                "            FROM sys.columns c\n" +
                "            JOIN sys.tables t ON c.object_id = t.object_id\n" +
                "            JOIN sys.schemas s ON t.schema_id = s.schema_id\n" +
                "            JOIN sys.types ty ON c.user_type_id = ty.user_type_id\n" +
                "            WHERE s.name = N'" + SECURITY_SCHEMA + "'\n" +
                "              AND t.name = N'SYS_USERS'\n" +
                "              AND c.name = N'SYS_USERS_ID'\n" +
                "              AND ty.name IN (N'numeric', N'int')\n" +
                "              AND c.is_identity = 1\n" +
                "              AND c.is_nullable = 0\n" +
                "        )\n" +
                "            SET @DOES_ID_EXIST = 1;\n" +
                "\n" +
                "        IF EXISTS (\n" +
                "            SELECT 1\n" +
                "            FROM sys.columns c\n" +
                "            JOIN sys.tables t ON c.object_id = t.object_id\n" +
                "            JOIN sys.schemas s ON t.schema_id = s.schema_id\n" +
                "            JOIN sys.types ty ON c.user_type_id = ty.user_type_id\n" +
                "            WHERE s.name = N'" + SECURITY_SCHEMA + "'\n" +
                "              AND t.name = N'SYS_USERS'\n" +
                "              AND c.name = N'SYS_USER_CODE'\n" +
                "              AND ty.name IN (N'numeric', N'int')\n" +
                "              AND c.is_nullable = 0\n" +
                "        )\n" +
                "            SET @DOES_CODE_EXIST = 1;\n" +
                "\n" +
                "        IF EXISTS (\n" +
                "            SELECT 1\n" +
                "            FROM sys.columns c\n" +
                "            JOIN sys.tables t ON c.object_id = t.object_id\n" +
                "            JOIN sys.schemas s ON t.schema_id = s.schema_id\n" +
                "            JOIN sys.types ty ON c.user_type_id = ty.user_type_id\n" +
                "            WHERE s.name = N'" + SECURITY_SCHEMA + "'\n" +
                "              AND t.name = N'SYS_USERS'\n" +
                "              AND c.name = N'SYS_USERNAME'\n" +
                "              AND ty.name IN (N'nvarchar', N'varchar')\n" +
                "              AND c.is_nullable = 0\n" +
                "        )\n" +
                "            SET @DOES_USERNAME_EXIST = 1;\n" +
                "\n" +
                "        IF EXISTS (\n" +
                "            SELECT 1\n" +
                "            FROM sys.columns c\n" +
                "            JOIN sys.tables t ON c.object_id = t.object_id\n" +
                "            JOIN sys.schemas s ON t.schema_id = s.schema_id\n" +
                "            JOIN sys.types ty ON c.user_type_id = ty.user_type_id\n" +
                "            WHERE s.name = N'" + SECURITY_SCHEMA + "'\n" +
                "              AND t.name = N'SYS_USERS'\n" +
                "              AND c.name = N'SYS_PASSWORD'\n" +
                "              AND ty.name = N'varbinary'\n" +
                "              AND c.is_nullable = 0\n" +
                "              AND c.max_length >= 64\n" +
                "        )\n" +
                "            SET @DOES_PASSWORD_EXIST = 1;\n" +
                "\n" +
                "        IF EXISTS (\n" +
                "            SELECT 1\n" +
                "            FROM sys.columns c\n" +
                "            JOIN sys.tables t ON c.object_id = t.object_id\n" +
                "            JOIN sys.schemas s ON t.schema_id = s.schema_id\n" +
                "            JOIN sys.types ty ON c.user_type_id = ty.user_type_id\n" +
                "            WHERE s.name = N'" + SECURITY_SCHEMA + "'\n" +
                "              AND t.name = N'SYS_USERS'\n" +
                "              AND c.name = N'SYS_PASSWORD_SALT'\n" +
                "              AND ty.name = N'nvarchar'\n" +
                "              AND c.is_nullable = 0\n" +
                "        )\n" +
                "            SET @DOES_PASSWORD_SALT_EXIST = 1;\n" +
                "\n" +
                "        IF EXISTS (\n" +
                "            SELECT 1\n" +
                "            FROM sys.columns c\n" +
                "            JOIN sys.tables t ON c.object_id = t.object_id\n" +
                "            JOIN sys.schemas s ON t.schema_id = s.schema_id\n" +
                "            JOIN sys.types ty ON c.user_type_id = ty.user_type_id\n" +
                "            WHERE s.name = N'" + SECURITY_SCHEMA + "'\n" +
                "              AND t.name = N'SYS_USERS'\n" +
                "              AND c.name = N'SYS_LOGIN_SESSION'\n" +
                "              AND ty.name = N'nvarchar'\n" +
                "              AND c.is_nullable = 1\n" +
                "        )\n" +
                "            SET @DOES_SESSION_EXIST = 1;\n" +
                "\n" +
                "        IF @DOES_SESSION_EXIST = 0\n" +
                "           OR @DOES_PASSWORD_EXIST = 0\n" +
                "           OR @DOES_USERNAME_EXIST = 0\n" +
                "           OR @DOES_PASSWORD_SALT_EXIST = 0\n" +
                "           OR @DOES_CODE_EXIST = 0\n" +
                "           OR @DOES_ID_EXIST = 0\n" +
                "        BEGIN\n" +
                "            DROP TABLE " + SECURITY_SCHEMA + ".SYS_USERS;\n" +
                "            SET @DOES_TABLE_EXIST = 0;\n" +
                "        END\n" +
                "    END\n" +
                "\n" +
                "    IF @DOES_TABLE_EXIST = 0\n" +
                "    BEGIN\n" +
                "        CREATE TABLE " + SECURITY_SCHEMA + ".SYS_USERS\n" +
                "        (\n" +
                "            SYS_USERS_ID NUMERIC(18,0) IDENTITY(1,1) PRIMARY KEY,\n" +
                "            SYS_USER_CODE NUMERIC(18,0) NOT NULL UNIQUE,\n" +
                "            SYS_USERNAME NVARCHAR(50) NOT NULL UNIQUE,\n" +
                "            SYS_PASSWORD VARBINARY(512) NOT NULL,\n" +
                "            SYS_PASSWORD_SALT NVARCHAR(23) NOT NULL\n" +
                "                DEFAULT CONVERT(NVARCHAR(23), GETDATE(), 121),\n" +
                "            SYS_LOGIN_SESSION NVARCHAR(512) NULL\n" +
                "        );\n" +
                "    END\n" +
                "\n" +
                "    /*\n" +
                "        Guarantee that ADMIN exists with USER_CODE = 1.\n" +
                "        Existing ADMIN credentials are preserved.\n" +
                "    */\n" +
                "    IF NOT EXISTS (\n" +
                "        SELECT 1\n" +
                "        FROM " + SECURITY_SCHEMA + ".SYS_USERS\n" +
                "        WHERE SYS_USER_CODE = 1\n" +
                "          AND UPPER(SYS_USERNAME) = N'ADMIN'\n" +
                "    )\n" +
                "    BEGIN\n" +
                "        DECLARE @SALT NVARCHAR(23) =\n" +
                "            CONVERT(NVARCHAR(23), GETDATE(), 121);\n" +
                "\n" +
                "        /*\n" +
                "            Remove conflicting rows before recreating ADMIN.\n" +
                "        */\n" +
                "        DELETE FROM " + SECURITY_SCHEMA + ".SYS_USERS\n" +
                "        WHERE SYS_USER_CODE = 1\n" +
                "           OR UPPER(SYS_USERNAME) = N'ADMIN';\n" +
                "\n" +
                "        INSERT INTO " + SECURITY_SCHEMA + ".SYS_USERS\n" +
                "        (\n" +
                "            SYS_USER_CODE,\n" +
                "            SYS_USERNAME,\n" +
                "            SYS_PASSWORD,\n" +
                "            SYS_PASSWORD_SALT\n" +
                "        )\n" +
                "        VALUES\n" +
                "        (\n" +
                "            1,\n" +
                "            N'ADMIN',\n" +
                "            HASHBYTES(\n" +
                "                'SHA2_512',\n" +
                "                CONCAT(N'12', @SALT)\n" +
                "            ),\n" +
                "            @SALT\n" +
                "        );\n" +
                "    END\n" +
                "END;"
        );
    }
}