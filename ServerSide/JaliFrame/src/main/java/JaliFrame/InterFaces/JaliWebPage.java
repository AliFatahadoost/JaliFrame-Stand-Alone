package JaliFrame.InterFaces;

import com.sun.net.httpserver.HttpServer;
import JaliFrame.PageRelatedEnums.FilesEnum;

public interface JaliWebPage {

    int getObjectId();

    FilesEnum getFile();

    void registerRoute(HttpServer server);
}