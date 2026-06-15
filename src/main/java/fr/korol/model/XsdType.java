package fr.korol.model;

public abstract class XsdType {
    private final String name;
    private String documentation;

    protected XsdType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }
}
