package fr.korol.model;

public abstract class XsdType {
    private final String name;
    private String documentation;

    protected XsdType(String name) {
        this.name = name;
    }

    public String getName() {
        return stripNamespace(name);
    }

    public String getFullName() {
        return name;
    }

    private String stripNamespace(String value) {
        if (value == null) return null;
        return value.contains(":") ? value.substring(value.indexOf(':') + 1) : value;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }
}
