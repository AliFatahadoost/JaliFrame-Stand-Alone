
package JaliFrame.PageRelatedEnums;

public enum FileTypesEnum {
    
    html
    (
            "text/html"
    ),
    css
    (
            "text/css"
    ),
    js
    (
            "text/javascript"
    ),
        png
    (
            "image/png"
    );
        
    
    
    private final String fileTypeTechnicalName;
    
    FileTypesEnum(String fileTypeTechnicalName){
        this.fileTypeTechnicalName = fileTypeTechnicalName;
    }
    
    public String getTechnicalType(){return this.fileTypeTechnicalName;}
    
}
