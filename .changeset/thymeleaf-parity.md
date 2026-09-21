---
"xp-storybook": minor
---

Bring the Thymeleaf renderer up to parity with the FreeMarker one

- A template that is not on disk is looked up in the named application's resources.
- The Thymeleaf engine is now built per render instead of being configured once and kept.
- A Thymeleaf template that reaches outside its resource directory through a relative include is refused, and an included file must be a template.
- Relative includes resolve against the directory the including template was found in, rather than the name it was requested under.
- A failing Thymeleaf template no longer replaces the whole page.
- Exception messages are escaped before being written into the error response.
