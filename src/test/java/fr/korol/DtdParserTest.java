package fr.korol;

import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import fr.korol.model.XsdComplexType;
import fr.korol.model.XsdElement;
import fr.korol.model.XsdModel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class DtdParserTest extends BasePlatformTestCase {

    public void testContentModel() {
        Map<String, int[]> occurs = DtdParser.parseContentModel("(header, (line | note)+, footer?, header)");
        assertEquals(List.of("header", "line", "note", "footer"), List.copyOf(occurs.keySet()));
        assertOccurs(occurs.get("header"), 2, 2);
        assertOccurs(occurs.get("line"), 0, -1);
        assertOccurs(occurs.get("footer"), 0, 1);
        assertOccurs(DtdParser.parseContentModel("(#PCDATA | b)*").get("b"), 0, -1);
        assertTrue(DtdParser.parseContentModel("EMPTY").isEmpty());
    }

    public void testParsesEntitiesExternalSubsetAttributesAndComments() throws Exception {
        Path dir = Files.createTempDirectory("dtd");
        Files.writeString(dir.resolve("common.dtd"), "<!ELEMENT note (#PCDATA)>");
        Files.writeString(dir.resolve("order.dtd"), """
                <!ENTITY % common SYSTEM "common.dtd">
                %common;
                <!ENTITY % remote SYSTEM "http://example.invalid/never-fetched.dtd">
                %remote;
                <!ENTITY % lines "line+">
                <!-- An order -->
                <!ELEMENT order (header, %lines;, note?)>
                <!ATTLIST order status (draft|final) #REQUIRED id ID #IMPLIED>
                <!ELEMENT header EMPTY>
                <!ELEMENT line (#PCDATA)>
                <!ELEMENT orphan EMPTY>
                """);

        DtdParser parser = new DtdParser();
        XsdModel model = parser.parse(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(dir.resolve("order.dtd")));

        assertEquals(List.of("order", "orphan"), parser.getRootNames());
        XsdComplexType order = model.getComplexTypes().get("order");
        assertEquals("An order", order.getDocumentation());
        assertEquals(List.of("header", "line", "note"), order.getElements().stream().map(XsdElement::getName).toList());
        assertEquals("unbounded", order.getElements().get(1).getMaxOccurs());
        assertTrue(order.getAttributes().get(0).isRequired());
        assertEquals(Map.of("draft", "", "final", "").keySet(), model.getSimpleTypes().get("(draft|final)").getEnumerations().keySet());

        XsdTreeNode tree = XsdTreeNode.createTree(model, parser.getRootNames(), "order.dtd");
        assertEquals(2, tree.getChildCount());
    }

    public void testMutuallyRecursiveMixedContentStaysBounded() throws Exception {
        StringBuilder dtd = new StringBuilder("<!ENTITY % inline \"#PCDATA");
        for (int i = 0; i < 30; i++) dtd.append("|e").append(i);
        dtd.append("\"><!ELEMENT doc (%inline;)*>");
        for (int i = 0; i < 30; i++) dtd.append("<!ELEMENT e").append(i).append(" (%inline;)*>");
        Path file = Files.createTempDirectory("dtd").resolve("inline.dtd");
        Files.writeString(file, dtd);

        DtdParser parser = new DtdParser();
        XsdModel model = parser.parse(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(file));
        long start = System.nanoTime();
        XsdTreeNode tree = XsdTreeNode.createTree(model, parser.getRootNames(), "inline.dtd");

        assertTrue(System.nanoTime() - start < 5_000_000_000L);
        assertEquals(30, tree.getChildCount());
        assertTrue(java.util.Collections.list(tree.depthFirstEnumeration()).size() < 100_000);
    }

    private static void assertOccurs(int[] occurs, int min, int max) {
        assertEquals(min, occurs[0]);
        assertEquals(max, occurs[1]);
    }
}
