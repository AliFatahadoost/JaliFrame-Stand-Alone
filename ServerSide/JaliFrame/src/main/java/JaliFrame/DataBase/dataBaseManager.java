package JaliFrame.DataBase;

import JaliFrame.ConfigAndLauncherManager.readConfig;
import java.util.concurrent.atomic.AtomicBoolean;
import java.sql.*;

public class dataBaseManager {
        
        static private PooledConnection[] dbConnections = new PooledConnection[readConfig.MAX_CONNECTION_POOL];
        static private AtomicBoolean[] isConnectionAvaliable = new AtomicBoolean[readConfig.MAX_CONNECTION_POOL];
        static private boolean isDBInitilized = false;
        
    
    public static class PooledConnection implements AutoCloseable {
    private final Connection connection;
    private final int poolIndex;
    
        public PooledConnection(Connection connection, int poolIndex) {
            this.connection = connection;
            this.poolIndex = poolIndex;
        }

        public Connection getConnection() {
            return connection;
        }

        @Override
        public void close() {
            // This returns the connection to the pool instead of closing it
            isConnectionAvaliable[poolIndex].set(true);
        }
    }
        
        
    public static void InitilizeDataBase() throws ClassNotFoundException, SQLException
    {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");   
        for(int i = 0; i < readConfig.MAX_CONNECTION_POOL; i ++)
        {
            dbConnections[i] = new PooledConnection(DriverManager.getConnection(readConfig.url, readConfig.username, readConfig.password), i);
            isConnectionAvaliable[i] = new AtomicBoolean(true);
        }
        
        isDBInitilized = true;
    }
    
    private static boolean isValidConnection(Connection conn) {
        if (conn == null) return false;
        
        try {
            if (conn.isClosed()) {
                return false;
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1")) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }
    
    public static PooledConnection getPooledConnection() throws SQLException, ClassNotFoundException
    {

                if(!isDBInitilized)
                    InitilizeDataBase();
                
                for(int i = 0; i < readConfig.MAX_CONNECTION_POOL; i++)
                {
                    if(isConnectionAvaliable[i].compareAndSet(true, false))
                    {
                        
                        if(isValidConnection(dbConnections[i].getConnection()))
                        {
                            return dbConnections[i];
                        }
                        else
                        {
                            
                            dbConnections[i] = new PooledConnection(DriverManager.getConnection(readConfig.url, readConfig.username, readConfig.password), i);
                            return dbConnections[i];
                        }
                    }
                }   
                
            throw new SQLException("No available connections in the pool. All " + 
                          readConfig.MAX_CONNECTION_POOL + " connections are in use.");
           
    }
}

