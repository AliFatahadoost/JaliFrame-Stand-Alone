package JaliFrame.PageRelatedEnums;

import com.sun.net.httpserver.HttpServer;
import JaliFrame.ConfigAndLauncherManager.readConfig;
import JaliFrame.WebServerHandlers.pageHandlerOpener;
import JaliFrame.InterFaces.JaliWebPage;
import JaliFrame.DataBase.DataBaseInit;

public enum WebPagesEnum implements JaliWebPage {

    // =========================================================
    // SYSTEM
    // =========================================================

    Login(
        1,
        FilesEnum.Login,
        "/"
    ),

    Dashboard(
        2,
        FilesEnum.Dashboard,
        "/Dashboard"
    ),

    Home(
        3,
        FilesEnum.Home,
        "/home"
    );

    private final int objectId;
    private final FilesEnum pageFile;
    private final String route;

    private boolean shouldAuth = true;

    WebPagesEnum(
        int objectId,
        FilesEnum pageFile,
        String route
    )
    {
        this.objectId = objectId;
        this.pageFile = pageFile;
        this.route = route;
    }

    public void shouldNotAuthenticate()
    {
        this.shouldAuth = false;
    }

    @Override
    public int getObjectId()
    {
        return this.objectId;
    }

    @Override
    public FilesEnum getFile()
    {
        return this.pageFile;
    }

    @Override
    public void registerRoute(HttpServer server)
    {
        DataBaseInit.loadObjectIntoObjectList(
            objectId,
            this.name()
        );

        server.createContext(
            this.route,
            new pageHandlerOpener(
                readConfig.BASE_FILE_ADDRESS,
                this.pageFile,
                this.shouldAuth
            )
        );
    }
}