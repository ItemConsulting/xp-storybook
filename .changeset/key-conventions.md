---
"xp-storybook": major
---

Remove the `javaTypes` and `matchers` query parameters. Model values are typed by the end of their key instead: `…Date` is a `LocalDate`, `LocalDateTime` or `ZonedDateTime` depending on its shape, `…Count` or `…Int` is an integer if the value is one, and the components of a top-level `…Region` are rendered with the views given as query parameters.
