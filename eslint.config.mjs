import tseslint from "typescript-eslint";
import eslintPluginPrettierRecommended from "eslint-plugin-prettier/recommended";

export default [
  {
    files: ["./src/**/*.ts"],
    ignores: [
      "build/**/*.*",
      "tsup/*.*",
    ],
  },
  ...tseslint.configs.recommended,
  eslintPluginPrettierRecommended,
];
