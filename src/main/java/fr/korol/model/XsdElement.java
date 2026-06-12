package fr.korol.model;

public class XsdElement {
    private final String name;
    private final String type;
    private final String ref;
    private XsdComplexType anonymousType;

    public XsdElement(String name, String type, String ref) {
        this.name = name;
        this.type = type;
        this.ref = ref;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getRef() {
        return ref;
    }

    public XsdComplexType getAnonymousType() {
        return anonymousType;
    }

    public void setAnonymousType(XsdComplexType anonymousType) {
        this.anonymousType = anonymousType;
    }
}
