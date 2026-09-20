package JaliFrame.WebServerHandlers;

import JaliFrame.DataBase.dataBaseUtils;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;

public class webServerUtils {
    
    
    public static String[][] jsonParser(String json) {
            json = json.replace("\n", "")
                       .replace("\r", "")
                       .replace("[", "")
                       .replace("]", "")
                       .trim();

            String[] objects = json.split("\\},\\s*\\{");

            String[][] result = new String[objects.length][];

            for (int i = 0; i < objects.length; i++) {
                String obj = objects[i]
                        .replace("{", "")
                        .replace("}", "")
                        .trim();

                String[] pairs = obj.split(",");

                result[i] = new String[pairs.length];

                for (int j = 0; j < pairs.length; j++) {
                    String[] kv = pairs[j].split(":", 2);

                    String value = kv[1]
                            .trim()
                            .replace("\"", "");

                    result[i][j] = value;
                }
            }

            return result;
        }

    
    
    static public String extractTokenFromCookie(HttpExchange exchange) {
    String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
    if (cookieHeader == null) return null;
    String[] cookies = cookieHeader.split("; ");
    for (String cookie : cookies) {
        if (cookie.startsWith("token=")) {
            return cookie.substring(6);
        }
    }
    return null;
    }
    
    
    static public void refreshPage(HttpExchange exchange) throws IOException
        {           
            String htmlResponse = "<!DOCTYPE html>\n" +
                            "<html>\n" +
                            "<head>\n" +
                            "    <script>\n" +
                            "        // Refresh the entire parent page (not just iframe)\n" +
                            "        if (window.top !== window.self) {\n" +
                            "            window.top.location.reload();\n" +
                            "        } else {\n" +
                            "            window.location.reload();\n" +
                            "        }\n" +
                            "    </script>\n" +
                            "</head>\n" +
                            "<body>\n" +
                            "    <p>Refreshing page...</p>\n" +
                            "</body>\n" +
                            "</html>";

            byte[] response = htmlResponse.getBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, response.length);

            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();
            return;
        }
    
    
    static public void kickUnAuthenticated(HttpExchange exchange) throws IOException
    {
        String token = webServerUtils.extractTokenFromCookie(exchange);
            if (!dataBaseUtils.isAuthenticated(token)) {   
                exchange.getResponseHeaders().set("Location", "/LoginPage");
                exchange.sendResponseHeaders(302, -1); // 302 Found redirect
                exchange.close();
                try{
                //dataBaseUtils.runSelectQueryGetJSON("EXEC ACTIVITY_LOG_MANAGER 0, ?, ?", token, "User kicked. users token was invalid");
                return;
                }catch(Exception e){ System.out.println("something went wrong cought in Line 135 file webServerUtils : " + e.toString()); }
            }
    }
    
    
    public static String[][] parseGetRequestURL(String URL) {
        if (!URL.contains("?")) {
            String [][] i = new String[2][2];
            i[0][1]= "empty";
            i[1][1]= "";
            i[0][0]= "empty";
            i[1][0]= "";
            return  i;
        }

        String queryString = URL.substring(URL.indexOf("?") + 1);
        String[] pairs = queryString.split("&");
        String[][] values = new String[2][pairs.length];

        for (int i = 0; i < pairs.length; i++) {
            String[] keyValue = pairs[i].split("=", 2);
            if (keyValue.length > 1)
                for(int j = 0; j < 2; j++)
                {
                    values[j][i] = keyValue[j];
                }
            else{
                values[0][i] = keyValue[0];
                values[1][i] = "";
            }
        }
        //System.out.println(URL);
        return values;
    }
}
