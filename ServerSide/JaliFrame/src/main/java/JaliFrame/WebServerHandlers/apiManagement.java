package JaliFrame.WebServerHandlers;


import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import JaliFrame.DataBase.dataBaseUtils;
import JaliFrame.ConfigAndLauncherManager.readConfig;
import JaliFrame.InterFaces.CrudQueries;
import java.util.Arrays;

public class apiManagement{ 

    
    public static class dataApiGen implements HttpHandler {
        
        public static class builder{

            private CrudQueries  query;
            private boolean sendTokenToDB;
            private boolean shouldAuthenticate = true;
            private boolean setCookie = false;
            
            private boolean shouldRedirect = false;
            private String redirectLocation = "";

            public builder setQuery(CrudQueries query){ this.query = query; return this;}
            public builder setRedirectLocation(String redirectLocation){this.shouldRedirect = true; this.redirectLocation = redirectLocation; return this;}
            public builder sendTokenToDB(boolean sendTokenToDB){ this.sendTokenToDB = sendTokenToDB; return this;}
            public builder shouldAuthenticate(boolean shouldAuthenticate){ this.shouldAuthenticate = shouldAuthenticate; return this;}
            public builder shouldSetCookie(boolean setCookie){ this.setCookie = setCookie; return this;}

            public dataApiGen build()
            {
                return new dataApiGen(this.query, this.sendTokenToDB, this.shouldAuthenticate, this.setCookie, this.shouldRedirect, this.redirectLocation);
            }
        }
        

        private final CrudQueries  query;
        
        private final boolean sendTokenToDB;
        private final boolean shouldAuthenticate;
        private final boolean setCookie;
        private final boolean shouldRedirect;
        private final String redirectLocation;
        
        private int countParameters(String query) {
                int count = 0;

                for (char c : query.toCharArray()) {
                    if (c == '?') {
                        count++;
                    }
                }

                return count;
            }
        
        private dataApiGen( CrudQueries query, boolean sendTokenToDB, boolean shouldAuthenticate, boolean setCookie, boolean shouldRedirect, String redirectLocation)
        {
            this.query = query;
            this.sendTokenToDB = sendTokenToDB;
            this.shouldAuthenticate = shouldAuthenticate;
            this.setCookie = setCookie;
            this.shouldRedirect = shouldRedirect;
            this.redirectLocation = redirectLocation;
        }
        
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String token = webServerUtils.extractTokenFromCookie(exchange);
            //System.out.println("something is calling me");
            
            if (!(!(this.shouldAuthenticate) || dataBaseUtils.isAuthenticated(token))) {
                //System.out.println("401");
                exchange.sendResponseHeaders(401, -1);
                exchange.close();
                return;
            }
            
            
            
            if ("PATCH".equals(exchange.getRequestMethod())) {
                //System.out.println("405");
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            } 
                
            if("POST".equals(exchange.getRequestMethod()))
                {         
                    if(this.query.whoDoesItBelongTo() != -1)
                    if(!(dataBaseUtils.isAllowed(token, this.query.whoDoesItBelongTo(), "CREATE" ))){
                        //System.out.println("403");
                        exchange.sendResponseHeaders(403, -1);
                        exchange.close();
                        return;
                    }
                    
                    InputStream is = exchange.getRequestBody();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    String userData = sb.toString();
                    String[][] userSentJSON = webServerUtils.jsonParser(userData);

                    System.out.print("Api is hit with this : " + userData);
                    int parametersCount = countParameters(this.query.getCreateQuery() != null? this.query.getCreateQuery() : "A");

                    if(userSentJSON.length == 0 || userSentJSON[0].length != parametersCount){
                        exchange.sendResponseHeaders(400, -1);
                        exchange.close();
                        return;
                    }
                    try{
                    String statusResult = dataBaseUtils.runSelectQueryGetJSON(
                                            this.query.getCreateQuery(),
                                            userSentJSON[0] 
                                          );
                    
                    String status = webServerUtils.jsonParser(statusResult)[0][0];
                   
                    
                    if(this.setCookie){
                    exchange.getResponseHeaders().add(
                        "Set-Cookie",
                        "token="+ status +"; Path=/; HttpOnly;Max-Age=" + readConfig.MAX_SESSION_TIME + "; SameSite=Strict"
                    );
                    
                    }
                    
                    
                    
                    if(!status.equalsIgnoreCase("0")){
                    
                        if(this.shouldRedirect){
                            System.out.println(exchange.getResponseHeaders().toString());
                            exchange.getResponseHeaders().add("Location", this.redirectLocation);
                            exchange.sendResponseHeaders(302, -1);
                            return;
                        }
                        else
                        exchange.sendResponseHeaders(200, -1);
                    
                    }
                    else if(status.equalsIgnoreCase("0"))
                        exchange.sendResponseHeaders(500, -1);
                    }catch(Exception e){
                    System.out.println("dataGenMod POST error : " + e);
                    try { exchange.sendResponseHeaders(500, 0); exchange.getResponseBody().close(); } catch (Exception ignore) {}
                }
                exchange.close();
            }
            else if("GET".equals(exchange.getRequestMethod()))
            {
                
                if(this.query.whoDoesItBelongTo() != -1)
                    if(!(dataBaseUtils.isAllowed(token, this.query.whoDoesItBelongTo(), "READ" ))){
                        //System.out.println("403");
                        exchange.sendResponseHeaders(403, -1);
                        exchange.close();
                        return;
                    }
                
                String[][] valuesSent = webServerUtils.parseGetRequestURL(exchange.getRequestURI().toString());
                
                int parametersCount = countParameters(this.query.getReadQuery() != null? this.query.getReadQuery() : "A");
                
                
                String Result = "";
                if(this.sendTokenToDB){
                    
                    try{
                    
                    String[][] emptyArray = {
                        {"empty", "empty"},
                        {"", ""}
                    };

                    if (Arrays.deepEquals(valuesSent, emptyArray)) {
                        String[] combinedInputs = new String[1];
                        combinedInputs[0] = token;
                        Result = dataBaseUtils.runSelectQueryGetJSON(this.query.getReadQuery(), combinedInputs);
                    }
                    
                    else{
                        
                    String[] combinedInputs = new String[valuesSent[1].length + 1];
                    combinedInputs[0] = token;
                    System.arraycopy(valuesSent[1], 0, combinedInputs, 1, valuesSent[1].length);
                    Result = dataBaseUtils.runSelectQueryGetJSON(this.query.getReadQuery(), combinedInputs);
                    
                    }
                    }
                    catch(Exception e)
                    {System.out.println("someThing went wrong with the SQL Code for GET in function genDataAPI e : " + e.toString() + "\n given query : "
                    + this.query.getReadQuery());}
                    
                }
                else
                {           
                    try{
                    if(!(valuesSent[1][0] == "" && valuesSent[1][1] == ""))
                        Result = dataBaseUtils.runSelectQueryGetJSON(this.query.getReadQuery(), valuesSent[1]);
                    else
                        Result = dataBaseUtils.runSelectQueryGetJSON(this.query.getReadQuery());
                    }
                    catch(Exception e)
                    {
                       System.out.println("Line 160 apiManamgement e : " + e.toString() + "\n given query : "
                    + this.query.getReadQuery());
                    }
                }
                
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        

                byte[] responseBytes = Result.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBytes.length);

                try(OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
                catch(Exception e) {
                   System.out.println(e);
                   exchange.sendResponseHeaders(500, -1);
                   exchange.close();
                }
            }
            else if("DELETE".equals(exchange.getRequestMethod()))
            {
            
                
                if(this.query.whoDoesItBelongTo() != -1)
                    if(!(dataBaseUtils.isAllowed(token, this.query.whoDoesItBelongTo(), "DELETE" ))){
                        //System.out.println("403");
                        exchange.sendResponseHeaders(403, -1);
                        exchange.close();
                        return;
                    }
                
                String[][] valuesSent = webServerUtils.parseGetRequestURL(exchange.getRequestURI().toString());
                
                if(valuesSent.length != 2) 
                {
                    exchange.sendResponseHeaders(400, -1);
                    exchange.close();
                    return;
                }
                String Result = "";
                
                try{
                Result = dataBaseUtils.runSelectQueryGetJSON(this.query.getDeleteQuery(), valuesSent[1][0]);
                }catch(Exception e){}
                
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        

                byte[] responseBytes = Result.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBytes.length);

                try(OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
                catch(Exception e) {
                   System.out.println(e);
                   exchange.sendResponseHeaders(500, -1);
                }
            }
            if("PUT".equals(exchange.getRequestMethod()))
            {         
                
                if(this.query.whoDoesItBelongTo() != -1)
                    if(!(dataBaseUtils.isAllowed(token, this.query.whoDoesItBelongTo(), "UPDATE" ))){
                        //System.out.println("403");
                        exchange.sendResponseHeaders(403, -1);
                        exchange.close();
                        return;
                    }
                
                InputStream is = exchange.getRequestBody();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                String userData = sb.toString();
                String[][] userSentJSON = webServerUtils.jsonParser(userData);


                String[][] valuesSent = webServerUtils.parseGetRequestURL(exchange.getRequestURI().toString());

                if(userSentJSON.length == 0 || valuesSent.length != 2){
                    exchange.sendResponseHeaders(400, -1);
                    exchange.close();
                    return;
                }

                String[] inputDataBase = new String[userSentJSON[0].length + 1];
                inputDataBase[userSentJSON[0].length] = valuesSent[1][0];  
                System.arraycopy(userSentJSON[0], 0, inputDataBase, 0, userSentJSON[0].length);
                
                try{
                String statusResult = dataBaseUtils.runSelectQueryGetJSON(
                                        this.query.getUpdateQuery(),
                                        inputDataBase
                                      );
                
                
                String status = webServerUtils.jsonParser(statusResult)[0][0];
                if(status.contains("1"))
                    exchange.sendResponseHeaders(200, -1);
                else if(status.contains("0"))
                    exchange.sendResponseHeaders(500, -1);
                }
                catch(Exception e)
                {
                   //Exception Handling BABY they need to learn from me
                }
                
                exchange.close();
            }
        }
        
    }
    
}
