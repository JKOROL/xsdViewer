package fr.korol.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XsdModel {
    private final List<XsdElement> rootElements = new ArrayList<>();
    private final Map<String, XsdComplexType> complexTypes = new HashMap<>();
    private final Map<String, XsdSimpleType> simpleTypes = new HashMap<>();

    public void addRootElement(XsdElement element) {
        rootElements.add(element);
    }

    public List<XsdElement> getRootElements() {
        return rootElements;
    }

    public void addComplexType(XsdComplexType type) {
        complexTypes.put(type.getName(), type);
    }

    public Map<String, XsdComplexType> getComplexTypes() {
        return complexTypes;
    }

    public void addSimpleType(XsdSimpleType type) {
        simpleTypes.put(type.getName(), type);
    }

    public Map<String, XsdSimpleType> getSimpleTypes() {
        return simpleTypes;
    }
}
