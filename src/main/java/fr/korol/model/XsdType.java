package fr.korol.model;

public abstract class XsdType {
    private final String name;

    protected XsdType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
