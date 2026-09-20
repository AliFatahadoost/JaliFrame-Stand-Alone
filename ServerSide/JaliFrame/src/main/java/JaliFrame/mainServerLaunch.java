package JaliFrame;

import JaliFrame.PageRelatedEnums.WebPagesEnum;
import JaliFrame.ConfigAndLauncherManager.readConfig;
import JaliFrame.PageRelatedEnums.CrudQueriesEnum;
import JaliFrame.WebServerHandlers.apiManagement;
import com.sun.net.httpserver.HttpServer;
import JaliFrame.DataBase.DataBaseInit;


import java.io.IOException;

public class mainServerLaunch {

    public static void main(String[] args) throws IOException {

        HttpServer server = readConfig.initiate();

        
        WebPagesEnum.Login.shouldNotAuthenticate();

        WebPagesEnum.Login.registerRoute(server);
        WebPagesEnum.Dashboard.registerRoute(server);
        WebPagesEnum.Home.registerRoute(server);

        
        DataBaseInit.finalizeRegistration();
    }
}