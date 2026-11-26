<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# XsdViewer Changelog

## [Unreleased]

## [1.2.0] - 2026-11-26

### Added

- Element details dialog (double-click a node in the tree)
- XSD library folder setting (Settings > XSD Viewer) to browse XSD files outside the current project
- "XSD Library" tool window listing every XSD of the library folder
- Tools > Open XSD from Library... action

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
