<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# XsdViewer Changelog

## [Unreleased]

## [1.2.0] - 2026-11-26

### Added

- Element details dialog (double-click a node in the tree)
- XSD library folder setting (Settings > XSD Viewer) to browse XSD files outside the current project
- "XSD Library" tool window listing every XSD of the library folder
- Tools > Open XSD/DTD from Library... action
- DTD support: `.dtd` files open in the tree view with search, XPath, details dialog and tooltips (from comments)
- The XSD library tool window and the Open from Library action also list `.dtd` files

### Changed

- Compatible with IntelliJ IDEA 2026.3 while still supporting 2026.1 and later
- Editor tab renamed from "XSD Tree" to "Schema Tree"
- DTD trees are capped at 50,000 nodes ("Tree too large, truncated") so heavily recursive DTDs open instantly

### Fixed

- `schemaLocation` of imports and includes now resolves backslash paths, absolute paths and `file:` URLs relative to the including file
- `xs:redefine` and `xs:override` schemas are now loaded

## [1.1.0] - 2026-06-17

### Added

- Copy path and copy element name from the tree context menu
- Setting to show namespaces
- XPath search (`/Root/Elem`, `//Elem[@attr]`, `//Elem[1]`)

## [1.0.0] - 2026-06-15

### Added

- XSD tree view editor with documentation tooltips
- Search within the XSD structure
- English and French translations
