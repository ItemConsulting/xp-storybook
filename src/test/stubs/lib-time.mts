// Stub for "/lib/time" (lib-xp-time). The real module is Java-backed and has no Node
// implementation, so tests get tagged objects — enough to assert which branch of
// deserializeJavaObjects ran, and with what argument.
export const LocalDate = { parse: (v: string) => ({ kind: "LocalDate", v }) };
export const LocalDateTime = { parse: (v: string) => ({ kind: "LocalDateTime", v }) };
export const ZonedDateTime = { parse: (v: string) => ({ kind: "ZonedDateTime", v }) };
