# xp-storybook

## 1.3.0

### Minor Changes

- dc457f6: Use latest version of Freemarker
- cc6b05f: Fix localize in Freemarker-preview when multiple baseDirPaths
- dc457f6: Interpret ftlh file extension as Freemarker
- dc457f6: Add Storybook-compatible version of `PortalObject` where `localize` and `assetUrl` use file system.

## 1.2.0

### Minor Changes

- 3b68b67: Add support for deserializing java.time.LocalDate

## 1.1.1

### Patch Changes

- 1013d93: Fix issue where undefined values can crash the renderer

## 1.1.0

### Minor Changes

- 59538b3: Migrate build tool to tsup
- 232056d: Allow comma separated list in xpResourcesDirPath

## 1.0.4

### Minor Changes

- 232056d: Allow comma separated list in xpResourcesDirPath

## 1.0.3

### Minor Changes

- 40f625f: Roll back change of i18n bundle path

## 1.0.2

### Minor Changes

- 482b039: Add support for `assetUrl` in inline templates

## 1.0.1

### Minor Changes

- eff6823: Fixed broken `[@localize /]` in Freemarker-templates

## 1.0.0

### Minor Changes

- 4fafc49: Initial release. Storybook integration for Freemarker or Thymeleaf templates
