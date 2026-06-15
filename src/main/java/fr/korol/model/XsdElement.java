package fr.korol.model;

public class XsdElement {
    private final String name;
    private final String type;
    private final String ref;
    private final String minOccurs;
    private final String maxOccurs;
    private String documentation;
    private XsdComplexType anonymousType;

    public XsdElement(String name, String type, String ref, String minOccurs, String maxOccurs) {
        this.name = name;
        this.type = type;
        this.ref = ref;
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
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

    public String getMinOccurs() {
        return minOccurs;
    }

    public String getMaxOccurs() {
        return maxOccurs;
    }

    public XsdComplexType getAnonymousType() {
        return anonymousType;
    }

    public void setAnonymousType(XsdComplexType anonymousType) {
        this.anonymousType = anonymousType;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }
}
