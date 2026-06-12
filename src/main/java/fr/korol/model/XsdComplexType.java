package fr.korol.model;

import java.util.ArrayList;
import java.util.List;

public class XsdComplexType extends XsdType {
    private final List<XsdElement> elements = new ArrayList<>();
    private final List<XsdAttribute> attributes = new ArrayList<>();
    private String baseType;

    public XsdComplexType(String name) {
        super(name);
    }

    public List<XsdElement> getElements() {
        return elements;
    }

    public List<XsdAttribute> getAttributes() {
        return attributes;
    }

    public String getBaseType() {
        return baseType;
    }

    public void setBaseType(String baseType) {
        this.baseType = baseType;
    }

    public void addElement(XsdElement element) {
        elements.add(element);
    }

    public void addAttribute(XsdAttribute attribute) {
        attributes.add(attribute);
    }
}
