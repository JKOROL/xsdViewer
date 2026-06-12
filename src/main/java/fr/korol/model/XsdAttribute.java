package fr.korol.model;

public class XsdAttribute {
    private final String name;
    private final String type;

    public XsdAttribute(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}
