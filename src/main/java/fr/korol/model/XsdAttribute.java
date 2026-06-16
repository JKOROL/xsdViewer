package fr.korol.model;

public class XsdAttribute {
    private final String name;
    private final String type;
    private final boolean required;
    private String documentation;

    public XsdAttribute(String name, String type, boolean required) {
        this.name = name;
        this.type = type;
        this.required = required;
    }

    public String getName() {
        return stripNamespace(name);
    }

    public String getFullName() {
        return name;
    }

    public String getType() {
        return stripNamespace(type);
    }

    public String getFullType() {
        return type;
    }

    private String stripNamespace(String value) {
        if (value == null) return null;
        return value.contains(":") ? value.substring(value.indexOf(':') + 1) : value;
    }

    public boolean isRequired() {
        return required;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }
}
