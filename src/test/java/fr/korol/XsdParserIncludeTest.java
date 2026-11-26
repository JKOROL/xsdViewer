package fr.korol;

import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import fr.korol.model.XsdModel;

import java.nio.file.Files;
import java.nio.file.Path;

public class XsdParserIncludeTest extends BasePlatformTestCase {

    private static final String XS = "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"";

    public void testResolvesRelativeBackslashAndFileUrlLocations() throws Exception {
        Path dir = Files.createTempDirectory("xsd");
        Files.createDirectories(dir.resolve("main"));
        Files.createDirectories(dir.resolve("common"));
        Files.writeString(dir.resolve("common/a.xsd"), "<xs:schema " + XS + "><xs:complexType name=\"A\"/></xs:schema>");
        Files.writeString(dir.resolve("common/b.xsd"), "<xs:schema " + XS + "><xs:complexType name=\"B\"/></xs:schema>");
        Files.writeString(dir.resolve("common/c.xsd"), "<xs:schema " + XS + "><xs:complexType name=\"C\"/></xs:schema>");
        Files.writeString(dir.resolve("main/root.xsd"), "<xs:schema " + XS + ">"
                + "<xs:include schemaLocation=\"../common/a.xsd\"/>"
                + "<xs:include schemaLocation=\"..\\common\\b.xsd\"/>"
                + "<xs:import schemaLocation=\"" + dir.resolve("common/c.xsd").toUri() + "\"/>"
                + "</xs:schema>");

        XsdModel model = new XsdParser().parse(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(dir.resolve("main/root.xsd")));

        assertTrue(model.getComplexTypes().containsKey("A"));
        assertTrue(model.getComplexTypes().containsKey("B"));
        assertTrue(model.getComplexTypes().containsKey("C"));
    }
}
