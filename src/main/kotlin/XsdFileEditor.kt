package com.korol

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefBrowser
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class XsdFileEditor(private val project: Project, private val file: VirtualFile) : UserDataHolderBase(), FileEditor {
    private val browser = JBCefBrowser()

    init {
        loadGraph()
    }

    private fun loadGraph() {
        val xsdContent = String(file.contentsToByteArray())
        val mermaidData = parseXsdToMermaid(xsdContent)
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <script src="https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js"></script>
                <script>
                    mermaid.initialize({ 
                        startOnLoad: true,
                        flowchart: {
                            useMaxWidth: false,
                            htmlLabels: true
                        }
                    });
                </script>
                <style>
                    body { background-color: white; margin: 20px; }
                    .mermaid { overflow: auto; }
                </style>
            </head>
            <body>
                <div class="mermaid">
                    $mermaidData
                </div>
            </body>
            </html>
        """.trimIndent()
        browser.loadHTML(html)
    }

    private fun parseXsdToMermaid(content: String): String {
        val sb = StringBuilder("graph LR\n")
        val processedNodes = mutableSetOf<String>()

        try {
            val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(org.xml.sax.InputSource(java.io.StringReader(content)))

            // Extract complexTypes first to know which types have structure
            val complexTypeMap = mutableMapOf<String, org.w3c.dom.Element>()
            val simpleTypeMap = mutableMapOf<String, org.w3c.dom.Element>()

            val complexTypes = doc.getElementsByTagNameNS("*", "complexType")
            for (i in 0 until complexTypes.length) {
                val ct = complexTypes.item(i) as org.w3c.dom.Element
                val name = ct.getAttribute("name")
                if (name.isNotEmpty()) complexTypeMap[name] = ct
            }

            val simpleTypes = doc.getElementsByTagNameNS("*", "simpleType")
            for (i in 0 until simpleTypes.length) {
                val st = simpleTypes.item(i) as org.w3c.dom.Element
                val name = st.getAttribute("name")
                if (name.isNotEmpty()) simpleTypeMap[name] = st
            }

            val context = ParserContext(sb, processedNodes, complexTypeMap, simpleTypeMap)

            // Start from top-level elements and types
            val rootElements = doc.documentElement.childNodes
            for (i in 0 until rootElements.length) {
                val node = rootElements.item(i)
                if (node is org.w3c.dom.Element) {
                    if (node.localName == "element") {
                        processElement(node, null, context)
                    } else if (node.localName == "complexType") {
                        val name = node.getAttribute("name")
                        if (name.isNotEmpty()) {
                            val typeId = cleanId(name)
                            if (!processedNodes.contains(typeId)) {
                                processedNodes.add(typeId)
                                sb.append("  $typeId[[\"$name\"]]\n")
                                sb.append("  style $typeId fill:#f9f,stroke:#333,stroke-width:2px\n")
                                processComplexType(node, typeId, context)
                            }
                        }
                    }
                }
            }

        } catch (e: Exception) {
            sb.append("  Error[Parsing Error: ${e.message?.take(50)}]\n")
        }

        if (sb.length <= 9) {
            sb.append("  NoElementsFound[No top-level elements found]\n")
        }

        return sb.toString()
    }

    private data class ParserContext(
        val sb: StringBuilder,
        val processedNodes: MutableSet<String>,
        val complexTypeMap: Map<String, org.w3c.dom.Element>,
        val simpleTypeMap: Map<String, org.w3c.dom.Element>
    )

    private fun cleanId(id: String): String = id.replace(Regex("[^a-zA-Z0-9]"), "_")

    private fun processElement(element: org.w3c.dom.Element, parentId: String?, context: ParserContext) {
        val name = element.getAttribute("name")
        val ref = element.getAttribute("ref")

        val displayName = if (name.isNotEmpty()) name else if (ref.isNotEmpty()) ref.substringAfter(":") else "unknown"
        val elementId = cleanId("${parentId ?: ""}_$displayName")

        context.sb.append("  $elementId(\"$displayName\")\n")
        if (parentId != null) {
            context.sb.append("  $parentId -- contains --> $elementId\n")
        }

        val type = element.getAttribute("type").substringAfter(":")
        if (type.isNotEmpty()) {
            val typeId = cleanId(type)
            context.sb.append("  $elementId -. type .-> $typeId\n")

            if (context.complexTypeMap.containsKey(type) && !context.processedNodes.contains(typeId)) {
                context.processedNodes.add(typeId)
                context.sb.append("  $typeId[[\"$type\"]]\n")
                context.sb.append("  style $typeId fill:#f9f,stroke:#333,stroke-width:2px\n")
                processComplexType(context.complexTypeMap[type]!!, typeId, context)
            } else if (context.simpleTypeMap.containsKey(type) && !context.processedNodes.contains(typeId)) {
                context.processedNodes.add(typeId)
                context.sb.append("  $typeId([\"$type\"])\n")
                context.sb.append("  style $typeId fill:#eef,stroke:#33f,stroke-width:1px\n")
            }
        } else if (ref.isNotEmpty()) {
            val refName = ref.substringAfter(":")
            val refId = cleanId(refName)
            context.sb.append("  $elementId -. ref .-> $refId\n")
        } else {
            // Check for anonymous complex type
            val ctNodes = element.getElementsByTagNameNS("*", "complexType")
            if (ctNodes.length > 0) {
                processComplexType(ctNodes.item(0) as org.w3c.dom.Element, elementId, context)
            } else {
                val stNodes = element.getElementsByTagNameNS("*", "simpleType")
                if (stNodes.length > 0) {
                    context.sb.append("  style $elementId fill:#eef,stroke:#33f\n")
                }
            }
        }
    }

    private fun processComplexType(ctElement: org.w3c.dom.Element, parentId: String, context: ParserContext) {
        // Process extension/restriction
        val extensions = ctElement.getElementsByTagNameNS("*", "extension")
        for (i in 0 until extensions.length) {
            val extension = extensions.item(i) as org.w3c.dom.Element
            val base = extension.getAttribute("base").substringAfter(":")
            if (base.isNotEmpty()) {
                val baseId = cleanId(base)
                context.sb.append("  $parentId -- extends --> $baseId\n")
            }
        }

        val restrictions = ctElement.getElementsByTagNameNS("*", "restriction")
        for (i in 0 until restrictions.length) {
            val restriction = restrictions.item(i) as org.w3c.dom.Element
            val base = restriction.getAttribute("base").substringAfter(":")
            if (base.isNotEmpty() && !base.startsWith("xsd:")) {
                val baseId = cleanId(base)
                context.sb.append("  $parentId -- restricts --> $baseId\n")
            }
        }

        val nestedElements = ctElement.getElementsByTagNameNS("*", "element")
        for (i in 0 until nestedElements.length) {
            val nested = nestedElements.item(i) as org.w3c.dom.Element
            // Only process child elements of the current complexType (avoid deep recursion by getElementsByTagName)
            var p = nested.parentNode
            while (p != null && p != ctElement) {
                if (p.localName == "element" || p.localName == "complexType") break
                p = p.parentNode
            }
            if (p == ctElement) {
                processElement(nested, parentId, context)
            }
        }

        val attributes = ctElement.getElementsByTagNameNS("*", "attribute")
        for (i in 0 until attributes.length) {
            val attr = attributes.item(i) as org.w3c.dom.Element
            val attrName = attr.getAttribute("name")
            if (attrName.isNotEmpty()) {
                val attrId = cleanId("${parentId}_attr_$attrName")
                context.sb.append("  $attrId(\"@$attrName\")\n")
                context.sb.append("  $parentId -- attribute --> $attrId\n")
                context.sb.append("  style $attrId fill:#eee,stroke:#999,stroke-dasharray: 5 5\n")
            }
        }
    }

    override fun getComponent(): JComponent = browser.component

    override fun getPreferredFocusedComponent(): JComponent? = browser.component

    override fun getName(): String = "XSD Graph"

    override fun setState(state: FileEditorState) {}

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = true

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}

    override fun getCurrentLocation(): FileEditorLocation? = null

    override fun dispose() {
        browser.dispose()
    }
}
