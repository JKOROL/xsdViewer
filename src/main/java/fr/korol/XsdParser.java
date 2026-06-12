package fr.korol;

import fr.korol.model.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

public class XsdParser {

    public XsdModel parse(String content) throws Exception {
        XsdModel model = new XsdModel();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(content)));

        NodeList complexTypes = doc.getElementsByTagNameNS("*", "complexType");
        for (int i = 0; i < complexTypes.getLength(); i++) {
            Element ct = (Element) complexTypes.item(i);
            String name = ct.getAttribute("name");
            if (!name.isEmpty()) {
                XsdComplexType complexType = new XsdComplexType(name);
                model.addComplexType(complexType);
            }
        }

        NodeList simpleTypes = doc.getElementsByTagNameNS("*", "simpleType");
        for (int i = 0; i < simpleTypes.getLength(); i++) {
            Element st = (Element) simpleTypes.item(i);
            String name = st.getAttribute("name");
            if (!name.isEmpty()) {
                model.addSimpleType(new XsdSimpleType(name));
            }
        }

        // Second pass to fill complex types
        for (int i = 0; i < complexTypes.getLength(); i++) {
            Element ctElement = (Element) complexTypes.item(i);
            String name = ctElement.getAttribute("name");
            if (!name.isEmpty()) {
                XsdComplexType complexType = model.getComplexTypes().get(name);
                fillComplexType(complexType, ctElement, model);
            }
        }

        NodeList rootElements = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < rootElements.getLength(); i++) {
            Node node = rootElements.item(i);
            if (node instanceof Element element && "element".equals(element.getLocalName())) {
                model.addRootElement(parseElement(element, model));
            }
        }

        return model;
    }

    private void fillComplexType(XsdComplexType complexType, Element ctElement, XsdModel model) {
        NodeList extensions = ctElement.getElementsByTagNameNS("*", "extension");
        for (int i = 0; i < extensions.getLength(); i++) {
            Element extension = (Element) extensions.item(i);
            String baseAttr = extension.getAttribute("base");
            complexType.setBaseType(stripNamespace(baseAttr));
        }

        NodeList restrictions = ctElement.getElementsByTagNameNS("*", "restriction");
        for (int i = 0; i < restrictions.getLength(); i++) {
            Element restriction = (Element) restrictions.item(i);
            String baseAttr = restriction.getAttribute("base");
            complexType.setBaseType(stripNamespace(baseAttr));
        }

        NodeList nestedElements = ctElement.getElementsByTagNameNS("*", "element");
        for (int i = 0; i < nestedElements.getLength(); i++) {
            Element nested = (Element) nestedElements.item(i);
            if (isDirectChildOf(nested, ctElement)) {
                complexType.addElement(parseElement(nested, model));
            }
        }

        NodeList attributes = ctElement.getElementsByTagNameNS("*", "attribute");
        for (int i = 0; i < attributes.getLength(); i++) {
            Element attr = (Element) attributes.item(i);
            String attrName = attr.getAttribute("name");
            if (!attrName.isEmpty()) {
                String attrType = stripNamespace(attr.getAttribute("type"));
                complexType.addAttribute(new XsdAttribute(attrName, attrType));
            }
        }
    }

    private XsdElement parseElement(Element element, XsdModel model) {
        String name = element.getAttribute("name");
        String ref = element.getAttribute("ref");
        String typeAttr = element.getAttribute("type");
        String type = stripNamespace(typeAttr);

        XsdElement xsdElement = new XsdElement(name, type, ref);

        NodeList ctNodes = element.getElementsByTagNameNS("*", "complexType");
        if (ctNodes.getLength() > 0) {
            XsdComplexType anonymousType = new XsdComplexType("");
            fillComplexType(anonymousType, (Element) ctNodes.item(0), model);
            xsdElement.setAnonymousType(anonymousType);
        }

        return xsdElement;
    }

    private String stripNamespace(String value) {
        if (value == null) return "";
        return value.contains(":") ? value.substring(value.indexOf(':') + 1) : value;
    }

    private boolean isDirectChildOf(Element element, Element parent) {
        Node p = element.getParentNode();
        while (p != null && p != parent) {
            if (p instanceof Element pe && ("element".equals(pe.getLocalName()) || "complexType".equals(pe.getLocalName()))) {
                return false;
            }
            p = p.getParentNode();
        }
        return p == parent;
    }
}
