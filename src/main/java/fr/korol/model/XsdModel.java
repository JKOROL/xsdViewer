package fr.korol.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XsdModel {
    private final Map<String, XsdElement> globalElements = new HashMap<>();
    private final Map<String, XsdComplexType> complexTypes = new HashMap<>();
    private final Map<String, XsdSimpleType> simpleTypes = new HashMap<>();

    public void addGlobalElement(XsdElement element) {
        globalElements.put(element.getName(), element);
        if (element.getFullName() != null) {
            globalElements.put(element.getFullName(), element);
        }
    }

    public Map<String, XsdElement> getGlobalElements() {
        return globalElements;
    }

    public void addComplexType(XsdComplexType type) {
        complexTypes.put(type.getName(), type);
        if (type.getFullName() != null) {
            complexTypes.put(type.getFullName(), type);
        }
    }

    public Map<String, XsdComplexType> getComplexTypes() {
        return complexTypes;
    }

    public void addSimpleType(XsdSimpleType type) {
        simpleTypes.put(type.getName(), type);
        if (type.getFullName() != null) {
            simpleTypes.put(type.getFullName(), type);
        }
    }

    public Map<String, XsdSimpleType> getSimpleTypes() {
        return simpleTypes;
    }
}
