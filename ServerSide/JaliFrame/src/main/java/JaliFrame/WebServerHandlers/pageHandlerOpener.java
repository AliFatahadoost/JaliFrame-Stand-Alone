package JaliFrame.WebServerHandlers;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import JaliFrame.DataBase.dataBaseUtils;
import JaliFrame.ConfigAndLauncherManager.readConfig;
import JaliFrame.InterFaces.JaliFiles;

public class pageHandlerOpener implements HttpHandler{
    
        private String fileAddress;
        JaliFiles fileInfo;
        private boolean shouldAuth = true;

        public pageHandlerOpener(String baseAddress, JaliFiles fileInfo, boolean shouldAuth)
        {      
            this.fileInfo = fileInfo;
            this.fileAddress = baseAddress + this.fileInfo.relativeAddress();   
            File file = new File(this.fileAddress);
            String filesName = file.getName().contains(".")?file.getName().substring(0, file.getName().lastIndexOf('.')) : file.getName();
            this.shouldAuth = shouldAuth;
            //dataBaseUtils.runSelectQueryGetJSON("EXEC UPDATING_LIST_OF_SYS_OBJECTS ?, ?", filesName, this.accessId+"");

        }
        
    @Override
        public void handle(HttpExchange exchange) throws IOException
        {
            
            String token = webServerUtils.extractTokenFromCookie(exchange);
            if(!shouldAuth || dataBaseUtils.isAuthenticated(token)){
                System.out.println("should Auth" + shouldAuth);
                File file = new File(this.fileAddress);

                byte[] response;

                if (file.exists()) {

                    // --------------------------------------------------
                    // Binary files (images, fonts, …) — serve raw
                    // --------------------------------------------------
                    String fileType = this.fileInfo.getFileType();

                    if (!fileType.startsWith("text/")) {

                        response = Files.readAllBytes(file.toPath());

                        exchange.getResponseHeaders().set("Content-Type", fileType);
                        exchange.sendResponseHeaders(200, response.length);

                        OutputStream rawOs = exchange.getResponseBody();
                        rawOs.write(response);
                        rawOs.close();
                        return;
                    }

                    // --------------------------------------------------
                    // Text files — filter by access + rewrite base URL
                    // --------------------------------------------------

                    byte[] filteredBytes = filterHtmlByAccess(Files.readAllBytes(file.toPath()), token);

                    String htmlString = new String(filteredBytes, StandardCharsets.UTF_8);
                    htmlString = htmlString.replace("http://127.0.0.1:8080", readConfig.serversBaseUrl);
                    response = htmlString.getBytes(StandardCharsets.UTF_8);

                    exchange.getResponseHeaders().set("Content-Type", fileType);
                    exchange.sendResponseHeaders(200, response.length);

                } else {
                    response = "404 - File not found - sorry".getBytes();
                    exchange.sendResponseHeaders(404, response.length);
                }

                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
            }
            else
            {
             exchange.getResponseHeaders().set("Location", "/");
             exchange.sendResponseHeaders(302, -1);
             exchange.close();
            }
        }
        
        
        
        

public byte[] filterHtmlByAccess(byte[] htmlBytes, String token) {
    String html = new String(htmlBytes, StandardCharsets.UTF_8);
    StringBuilder out = new StringBuilder();
    int skipDepth = 0;
    int i = 0;
    int len = html.length();

    while (i < len) {
        char c = html.charAt(i);
        if (c == '<') {
            // Parse the tag
            int tagStart = i;
            int tagEnd = html.indexOf('>', i + 1);
            if (tagEnd == -1) {
                // No closing '>' – treat as plain text (shouldn't happen in valid HTML)
                out.append(c);
                i++;
                continue;
            }
            String tagContent = html.substring(i + 1, tagEnd).trim();
            boolean isEndTag = tagContent.startsWith("/");
            boolean isSelfClosing = tagContent.endsWith("/") || tagContent.endsWith("/" + (char)0); // handle possible whitespace
            // Remove trailing slash for self-closing detection
            String tagName;
            String attrString;
            if (isSelfClosing) {
                // remove the trailing '/'
                tagContent = tagContent.replaceFirst("/$", "").trim();
            }
            // Split tag name and attributes
            int firstSpace = tagContent.indexOf(' ');
            if (firstSpace == -1) {
                tagName = tagContent;
                attrString = "";
            } else {
                tagName = tagContent.substring(0, firstSpace);
                attrString = tagContent.substring(firstSpace + 1);
            }
            // Extract data-AccessCode value (only double-quoted)
            String accessCode = null;
            int attrIdx = attrString.indexOf("data-AccessCode");
            if (attrIdx != -1) {
                int eqPos = attrString.indexOf('=', attrIdx);
                if (eqPos != -1) {
                    int quoteStart = attrString.indexOf('"', eqPos);
                    if (quoteStart != -1) {
                        int quoteEnd = attrString.indexOf('"', quoteStart + 1);
                        if (quoteEnd != -1) {
                            accessCode = attrString.substring(quoteStart + 1, quoteEnd);
                        }
                    }
                }
            }

            // Now handle the tag based on type and skipDepth
            if (isEndTag) {
                // Closing tag
                if (skipDepth > 0) {
                    skipDepth--;
                    // Do NOT output the closing tag
                } else {
                    out.append('<').append(tagContent).append('>');
                }
                i = tagEnd + 1;
                continue;
            }

            // Start tag (including self-closing)
            boolean isStartTag = !isEndTag;
            if (isStartTag && !isSelfClosing) {
                // Normal start tag
                if (skipDepth > 0) {
                    // we are inside a skipped block: just increment depth and skip output
                    skipDepth++;
                } else {
                    // Check permission
                    boolean allowed = true;
                    if (accessCode != null && token != null) {
                        try {
                            int code = Integer.parseInt(accessCode);
                            allowed = dataBaseUtils.isAllowed(token, code, "READ");
                        } catch (NumberFormatException e) {
                            // invalid code – treat as not allowed? We'll skip if denied.
                            allowed = false;
                        }
                    }
                    if (!allowed) {
                        // start skipping
                        skipDepth = 1;
                        // do NOT output this tag
                    } else {
                        out.append('<').append(tagContent).append('>');
                    }
                }
                i = tagEnd + 1;
                continue;
            }

            // Self-closing tag (e.g., <img ... />)
            if (isSelfClosing) {
                if (skipDepth > 0) {
                    // inside skipped block – just skip
                } else {
                    boolean allowed = true;
                    if (accessCode != null && token != null) {
                        try {
                            int code = Integer.parseInt(accessCode);
                            allowed = dataBaseUtils.isAllowed(token, code, "READ");
                        } catch (NumberFormatException e) {
                            allowed = false;
                        }
                    }
                    if (allowed) {
                        out.append('<').append(tagContent).append("/>");
                    }
                    // if not allowed, we skip it entirely
                }
                i = tagEnd + 1;
                continue;
            }

            // fallback (should not happen)
            out.append(c);
            i++;
        } else {
            // regular character
            if (skipDepth == 0) {
                out.append(c);
            }
            i++;
        }
    }

    return out.toString().getBytes(StandardCharsets.UTF_8);
}
}
