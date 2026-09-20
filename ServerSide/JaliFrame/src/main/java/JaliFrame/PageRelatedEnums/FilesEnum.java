package JaliFrame.PageRelatedEnums;

import JaliFrame.InterFaces.JaliFiles;

public enum FilesEnum implements JaliFiles {

    // =========================================================
    // JALI FRAMEWORK FILES
    // =========================================================

    test(
        "/FrameWorksLib/Jali.js/test.html",
        true,
        FileTypesEnum.html
    ),

    // =========================================================
    // LEAFLET (bundled locally)
    // =========================================================

    leafletJs(
        "/FrameWorksLib/Leaflet/leaflet.js",
        true,
        FileTypesEnum.js
    ),

    leafletCss(
        "/FrameWorksLib/Leaflet/leaflet.css",
        true,
        FileTypesEnum.css
    ),

    leafletMarkerIcon(
        "/FrameWorksLib/Leaflet/images/marker-icon.png",
        true,
        FileTypesEnum.png
    ),

    leafletMarkerIcon2x(
        "/FrameWorksLib/Leaflet/images/marker-icon-2x.png",
        true,
        FileTypesEnum.png
    ),

    leafletMarkerShadow(
        "/FrameWorksLib/Leaflet/images/marker-shadow.png",
        true,
        FileTypesEnum.png
    ),

    // =========================================================
    // MAP ELEMENT
    // =========================================================

    mapBox(
        "/FrameWorksLib/Jali.js/custom_elements/mapBox.js",
        true,
        FileTypesEnum.js
    ),

    mapBoxCss(
        "/FrameWorksLib/JaliFrame.css/mapBox.css",
        true,
        FileTypesEnum.css
    ),
    
    coreJs(
        "/FrameWorksLib/Jali.js/core.js",
        true,
        FileTypesEnum.js
    ),

    tableFormElement(
        "/FrameWorksLib/Jali.js/custom_elements/dataTable.js",
        true,
        FileTypesEnum.js
    ),

    dataCombo(
        "/FrameWorksLib/Jali.js/custom_elements/dataCombo.js",
        true,
        FileTypesEnum.js
    ),

    findObjectBox(
        "/FrameWorksLib/Jali.js/custom_elements/findObjectBox.js",
        true,
        FileTypesEnum.js
    ),

    dateBox(
        "/FrameWorksLib/Jali.js/custom_elements/dateBox.js",
        true,
        FileTypesEnum.js
    ),
    
    fleetTripTable(
        "/FrameWorksLib/Jali.js/custom_elements/fleetTripTable.js",
        true,
        FileTypesEnum.js
    ),

    jaliForm(
        "/FrameWorksLib/Jali.js/custom_elements/jaliForm.js",
        true,
        FileTypesEnum.js
    ),

    cssTableFormData(
        "/FrameWorksLib/JaliFrame.css/readDataTable.css",
        true,
        FileTypesEnum.css
    ),

    dataComboCss(
        "/FrameWorksLib/JaliFrame.css/dataCombo.css",
        true,
        FileTypesEnum.css
    ),

    findObjectBoxCss(
        "/FrameWorksLib/JaliFrame.css/FindObjectBox.css",
        true,
        FileTypesEnum.css
    ),

    dateBoxCss(
        "/FrameWorksLib/JaliFrame.css/dateBox.css",
        true,
        FileTypesEnum.css
    ),

    cssDataForm(
        "/FrameWorksLib/JaliFrame.css/dataForm.css",
        true,
        FileTypesEnum.css
    ),

    // =========================================================
    // SYSTEM PAGES
    // =========================================================

    Login(
        "/Login/Login.html",
        false,
        FileTypesEnum.html
    ),

    Dashboard(
        "/Dashboard/Dashboard.html",
        false,
        FileTypesEnum.html
    ),

    Home(
        "/Home/Home.html",
        false,
        FileTypesEnum.html
    );

    private final String relativeAddress;
    private final boolean loadedByIframe;
    private final FileTypesEnum fileType;

    FilesEnum(
        String relativeAddress,
        boolean loadedByIframe,
        FileTypesEnum fileType
    )
    {
        this.relativeAddress = relativeAddress;
        this.loadedByIframe = loadedByIframe;
        this.fileType = fileType;
    }

    @Override
    public String relativeAddress()
    {
        return this.relativeAddress;
    }

    @Override
    public boolean loadedByIframe()
    {
        return this.loadedByIframe;
    }

    @Override
    public String getFileType()
    {
        return this.fileType.getTechnicalType();
    }
}