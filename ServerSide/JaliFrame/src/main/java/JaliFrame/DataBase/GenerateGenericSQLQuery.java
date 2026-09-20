
package JaliFrame.DataBase;

public class GenerateGenericSQLQuery {


    public static class ReadQuery
    {
    
        private String tableName;
        private String[] columnNames;
        private String[] columnDataTypes;
        private String whereQuery = "";
        private String orderByQuery = "";
        
        public ReadQuery setTableName(String tableName){this.tableName = tableName; return this;}
        public ReadQuery setColumnNames(String... columnNames){this.columnNames = columnNames; return this;}
        public ReadQuery setColumnDataTypes(String... columnDataTypes){this.columnDataTypes = columnDataTypes; return this;}
        public ReadQuery setWhereQuery(String whereQuery){this.whereQuery = whereQuery; return this;}
        public ReadQuery setOrderByQuery(String orderByQuery){this.orderByQuery = orderByQuery; return this;}
        
        public String getQuery()
        {
            String Query = "";
        
            Query = " DECLARE @PAGE_ROW_COUNT INT = ? \n" + "DECLARE @WHICH_PAGE INT = ? \n";
          
            Query = Query + " DECLARE @" + columnNames[0] + " " + columnDataTypes[0] + " = 0 \n";
            for(int i = 1; i < this.columnNames.length; i++)
            {
            
                Query = Query + " DECLARE @" + columnNames[i] + " " + columnDataTypes[i] + " = NULLIF(?, N'') \n";
            
            }

            Query += " SELECT \n";
            
            Query = Query + " " + columnNames[0] + " \n";
            for(int i = 1; i < this.columnNames.length; i++)
            {
            
                Query = Query + "," + columnNames[i] + " \n";
            
            }        
            
            Query += " FROM \n" + this.tableName + "\n WHERE 1 = 1\n";
            
            for(int i = 1; i < this.columnNames.length; i++)
            {
            
                Query += " AND CAST(" + columnNames[i] + " AS NVARCHAR) LIKE N'%'+ISNULL(CAST(@" + columnNames[i] +" AS NVARCHAR), N'')+N'%'";
            
            }
            
            Query += this.whereQuery + "\n";
            
            if(this.orderByQuery == "")
                Query += " ORDER BY \n" + this.columnNames[0];
            else
                Query += " ORDER BY \n" + this.orderByQuery;
            
            Query += " OFFSET ((@WHICH_PAGE - 1) * @PAGE_ROW_COUNT) ROWS FETCH NEXT @PAGE_ROW_COUNT ROWS ONLY ";
            System.out.println(Query + "\n\n\n\n");
            return Query;
        }
    }
    
    
    
    public static class CreateQuery
    {
        private String tableName;
        private String[] columnNames;

        public CreateQuery setTableName(String tableName)
        {
            this.tableName = tableName;
            return this;
        }

        public CreateQuery setColumnNames(String... columnNames)
        {
            this.columnNames = columnNames;
            return this;
        }

        public String getQuery()
        {
            StringBuilder query = new StringBuilder();

            query.append(" INSERT INTO ")
                 .append(tableName)
                 .append(" (");

            query.append(columnNames[0]);

            for (int i = 1; i < columnNames.length; i++)
            {
                query.append(", ")
                     .append(columnNames[i]);
            }

            query.append(" ) VALUES (");

            query.append(" ?");

            for (int i = 1; i < columnNames.length; i++)
            {
                query.append(" , ?");
            }

            query.append(" )");

            return query.toString() + " SELECT 1 AS STATUS ";
        }
    }
    
    
    public static class UpdateQuery
    {
        private String tableName;
        private String[] columnNames;
        private String whereQuery;

        public UpdateQuery setTableName(String tableName)
        {
            this.tableName = tableName;
            return this;
        }

        public UpdateQuery setColumnNames(String... columnNames)
        {
            this.columnNames = columnNames;
            return this;
        }

        public UpdateQuery setWhereQuery(String whereQuery)
        {
            this.whereQuery = whereQuery;
            return this;
        }

        public String getQuery()
        {
            StringBuilder query = new StringBuilder();

            query.append(" UPDATE ")
                 .append(tableName)
                 .append(" SET ");

            query.append(columnNames[0])
                 .append(" = ISNULL(NULLIF(?, ''), ")
                 .append(columnNames[0])
                 .append(" )");

            for (int i = 1; i < columnNames.length; i++)
            {
                query.append(" ,\n")
                     .append(columnNames[i])
                     .append(" = ISNULL(NULLIF(?, ''), ")
                     .append(columnNames[i])
                     .append(" )");
            }

            query.append("\n WHERE ")
                 .append(whereQuery);

            return query.toString() + "  SELECT 1 AS STATUS ";
        }
    }
    
    
    public static class DeleteQuery
    {
        private String tableName;
        private String whereQuery;

        public DeleteQuery setTableName(String tableName)
        {
            this.tableName = tableName;
            return this;
        }

        public DeleteQuery setWhereQuery(String whereQuery)
        {
            this.whereQuery = whereQuery;
            return this;
        }

        public String getQuery()
        {
            return " DELETE FROM " + tableName +
                   " WHERE " + whereQuery + " SELECT 1 AS STATUS ";
        }
    }

    
}
