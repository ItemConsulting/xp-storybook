---
"xp-storybook": major
---

Remove the `javaTypes` and `matchers` query parameters. Model values are typed by the end of their key instead: `…Date` is a `LocalDate`, `LocalDateTime` or `ZonedDateTime` depending on its shape, `…Count` or `…Number` is an integer if it is all digits, and the components of a `…Region` are rendered
