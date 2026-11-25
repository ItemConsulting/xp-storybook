// Copies the version of package.json into gradle.properties, run by `npm run versioning` after `changeset version`.
//
// The jar gets the same version as package.json, prereleases included: the 1.0.0-beta.0 that `changeset pre`
// produces is published as 1.0.0-beta.0, not as a snapshot. Maven sorts it before 1.0.0, and since it does not end
// with -SNAPSHOT, build.gradle publishes it to the releases repository.
import { readFileSync, writeFileSync } from "node:fs";

const { version } = JSON.parse(readFileSync("package.json", "utf8"));

if (!/^\d+\.\d+\.\d+(-[0-9A-Za-z.-]+)?$/.test(version)) {
  throw new Error(`Unexpected version in package.json: "${version}"`);
}

const gradleProperties = readFileSync("gradle.properties", "utf8");
const versionLine = /^version *=.*$/m;

if (!versionLine.test(gradleProperties)) {
  throw new Error('No "version = ..." line found in gradle.properties');
}

writeFileSync("gradle.properties", gradleProperties.replace(versionLine, `version = ${version}`));
console.log(`gradle.properties: version = ${version}`);
