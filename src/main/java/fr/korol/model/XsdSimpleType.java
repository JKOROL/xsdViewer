package fr.korol.model;

import java.util.HashMap;
import java.util.Map;

public class XsdSimpleType extends XsdType {
    private final Map<String, String> restrictions = new HashMap<>();
    private final Map<String, String> enumerations = new HashMap<>();

    public XsdSimpleType(String name) {
        super(name);
    }

    public void addRestriction(String key, String value) {
        restrictions.put(key, value);
    }

    public void addEnumeration(String value, String documentation) {
        enumerations.put(value, documentation);
    }

    public Map<String, String> getRestrictions() {
        return restrictions;
    }

    public Map<String, String> getEnumerations() {
        return enumerations;
    }
}
