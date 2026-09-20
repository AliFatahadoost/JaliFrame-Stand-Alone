package JaliFrame.PageRelatedEnums;

import JaliFrame.InterFaces.CrudQueries;
import JaliFrame.DataBase.GenerateGenericSQLQuery;

public enum CrudQueriesEnum implements CrudQueries
{

    // =========================================================
    // =========================================================
    // SECTION 1 — AUTH / USERS
    // =========================================================
    // =========================================================

    Login
    (
        "",
        "",
        "EXEC USERS_DATA_AND_PERMISSIONS.SHOULD_LOGIN ?, ?;",
        "",
        1
    ),
    
        UsersList
    (
        " DECLARE @PAGE_ROW_COUNT INT = ? \n" +
        " DECLARE @WHICH_PAGE INT = ? \n" +
        " DECLARE @Code NVARCHAR(50)     = NULLIF(?, N'') \n" +
        " DECLARE @Username NVARCHAR(50) = NULLIF(?, N'') \n" +
        " SELECT \n" +
        "   SYS_USERS_ID \n" +
        "  ,SYS_USER_CODE \n" +
        "  ,SYS_USERNAME \n" +
        " FROM IFMS_DB.USERS_DATA_AND_PERMISSIONS.SYS_USERS \n" +
        " WHERE 1 = 1 \n" +
        " AND CAST(SYS_USER_CODE AS NVARCHAR(50)) LIKE N'%'+ISNULL(@Code,     N'')+N'%' \n" +
        " AND CAST(SYS_USERNAME  AS NVARCHAR(50)) LIKE N'%'+ISNULL(@Username, N'')+N'%' \n" +
        " ORDER BY SYS_USER_CODE \n" +
        " OFFSET ((@WHICH_PAGE - 1) * @PAGE_ROW_COUNT) ROWS FETCH NEXT @PAGE_ROW_COUNT ROWS ONLY ",
        "",
        "",
        "",
        41
    ),

    PermissionsList
    (
        " DECLARE @PAGE_ROW_COUNT INT = ? \n" +
        " DECLARE @WHICH_PAGE INT = ? \n" +
        " DECLARE @Username NVARCHAR(50) = NULLIF(?, N'') \n" +
        " DECLARE @Object   NVARCHAR(50) = NULLIF(?, N'') \n" +
        " DECLARE @Read     NVARCHAR(50) = NULLIF(?, N'') \n" +
        " DECLARE @Create   NVARCHAR(50) = NULLIF(?, N'') \n" +
        " DECLARE @Update   NVARCHAR(50) = NULLIF(?, N'') \n" +
        " DECLARE @Delete   NVARCHAR(50) = NULLIF(?, N'') \n" +
        " SELECT \n" +
        "   P.OBJECT_USER_PERMISSION \n" +
        "  ,ISNULL(U.SYS_USERNAME, N'(unknown)') AS username \n" +
        "  ,ISNULL(O.OBJECT_TITLE, N'(unknown)') AS object_title \n" +
        "  ,P.CAN_READ \n" +
        "  ,P.CAN_CREATE \n" +
        "  ,P.CAN_UPDATE \n" +
        "  ,P.CAN_DELETE \n" +
        " FROM IFMS_DB.USERS_DATA_AND_PERMISSIONS.OBJECT_USER_PERMISSION P \n" +
        " LEFT JOIN IFMS_DB.USERS_DATA_AND_PERMISSIONS.SYS_USERS   U ON U.SYS_USER_CODE = P.USER_CODE \n" +
        " LEFT JOIN IFMS_DB.USERS_DATA_AND_PERMISSIONS.SYS_OBJECTS O ON O.OBJECT_CODE   = P.OBJECT_CODE \n" +
        " WHERE 1 = 1 \n" +
        " AND ISNULL(U.SYS_USERNAME, N'') LIKE N'%'+ISNULL(@Username, N'')+N'%' \n" +
        " AND ISNULL(O.OBJECT_TITLE, N'') LIKE N'%'+ISNULL(@Object,   N'')+N'%' \n" +
        " AND CAST(P.CAN_READ   AS NVARCHAR(50)) LIKE N'%'+ISNULL(@Read,   N'')+N'%' \n" +
        " AND CAST(P.CAN_CREATE AS NVARCHAR(50)) LIKE N'%'+ISNULL(@Create, N'')+N'%' \n" +
        " AND CAST(P.CAN_UPDATE AS NVARCHAR(50)) LIKE N'%'+ISNULL(@Update, N'')+N'%' \n" +
        " AND CAST(P.CAN_DELETE AS NVARCHAR(50)) LIKE N'%'+ISNULL(@Delete, N'')+N'%' \n" +
        " ORDER BY U.SYS_USER_CODE, O.OBJECT_CODE \n" +
        " OFFSET ((@WHICH_PAGE - 1) * @PAGE_ROW_COUNT) ROWS FETCH NEXT @PAGE_ROW_COUNT ROWS ONLY ",
        "",
        "",
        "",
        42
    ),

    createUser
    (
        "",
        "",
        "EXEC USERS_DATA_AND_PERMISSIONS.MAKE_NEW_USER ?, ?, ?;",
        ""
    ),

    getUsernameWithToken
    (
        "SELECT SYS_USERNAME " +
        "FROM IFMS_DB.USERS_DATA_AND_PERMISSIONS.SYS_USERS " +
        "WHERE SYS_LOGIN_SESSION = ?",
        "",
        "",
        ""
    ),

    // Returns the current user's code + name based on the session token.
    // Used by front-end to attribute tickets and log entries.
    // Requires sendTokenToDB(true) in apiManagement wiring.
    CurrentUser
    (
        "SELECT SYS_USER_CODE AS user_code, SYS_USERNAME AS username " +
        "FROM IFMS_DB.USERS_DATA_AND_PERMISSIONS.SYS_USERS " +
        "WHERE SYS_LOGIN_SESSION = ?",
        "",
        "",
        ""
    ),

    // All users for the UserManagement page.
    AllUsers
    (
        "SELECT SYS_USERS_ID, SYS_USER_CODE, SYS_USERNAME " +
        "FROM IFMS_DB.USERS_DATA_AND_PERMISSIONS.SYS_USERS " +
        "ORDER BY SYS_USER_CODE",
        "",
        "",
        ""
    ),

    // Full permission matrix view (for UserManagement page).
    AllUserObjectPermissions
    (
        "SELECT P.OBJECT_USER_PERMISSION, P.USER_CODE, " +
        "       O.OBJECT_CODE, O.OBJECT_TITLE, " +
        "       P.CAN_READ, P.CAN_CREATE, P.CAN_UPDATE, P.CAN_DELETE " +
        "FROM IFMS_DB.USERS_DATA_AND_PERMISSIONS.OBJECT_USER_PERMISSION P " +
        "INNER JOIN IFMS_DB.USERS_DATA_AND_PERMISSIONS.SYS_OBJECTS O " +
        "    ON O.OBJECT_CODE = P.OBJECT_CODE " +
        "ORDER BY P.USER_CODE, O.OBJECT_CODE",
        "",
        "",
        ""
    ),

    // Toggle a single permission row.
    UpdateUserObjectPermission
    (
        "",
        "UPDATE IFMS_DB.USERS_DATA_AND_PERMISSIONS.OBJECT_USER_PERMISSION " +
        "SET CAN_READ   = ISNULL(NULLIF(?, ''), CAN_READ), " +
        "    CAN_CREATE = ISNULL(NULLIF(?, ''), CAN_CREATE), " +
        "    CAN_UPDATE = ISNULL(NULLIF(?, ''), CAN_UPDATE), " +
        "    CAN_DELETE = ISNULL(NULLIF(?, ''), CAN_DELETE) " +
        "WHERE OBJECT_USER_PERMISSION = ?",
        "",
        ""
    );


    // =========================================================
    // =========================================================
    // ENUM FIELDS
    // =========================================================
    // =========================================================

    private final int belongsToObjectCode;
    private final String readQuery;
    private final String updateQuery;
    private final String createQuery;
    private final String deleteQuery;


    // =========================================================
    // CONSTRUCTORS
    // =========================================================

    CrudQueriesEnum(
        String readQuery,
        String updateQuery,
        String createQuery,
        String deleteQuery,
        int belongsToObjectCode
    )
    {
        this.readQuery = readQuery;
        this.updateQuery = updateQuery;
        this.createQuery = createQuery;
        this.deleteQuery = deleteQuery;
        this.belongsToObjectCode = belongsToObjectCode;
    }


    CrudQueriesEnum(
        String readQuery,
        String updateQuery,
        String createQuery,
        String deleteQuery
    )
    {
        this.readQuery = readQuery;
        this.updateQuery = updateQuery;
        this.createQuery = createQuery;
        this.deleteQuery = deleteQuery;
        this.belongsToObjectCode = -1;
    }


    // =========================================================
    // CRUD METHODS
    // =========================================================

    @Override
    public int whoDoesItBelongTo()
    {
        return this.belongsToObjectCode;
    }


    @Override
    public String getReadQuery()
    {
        return this.readQuery;
    }


    @Override
    public String getUpdateQuery()
    {
        return this.updateQuery + " select 1 as status";
    }


    @Override
    public String getDeleteQuery()
    {
        return this.deleteQuery + " select 1 as status";
    }


    @Override
    public String getCreateQuery()
    {
        return this.createQuery + " select 1 as status";
    }

}