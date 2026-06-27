# ByeBox Version Naming Policy

This document establishes version naming guidelines for coding assistant agents working on the ByeBox project. All agents modifying version configurations in `build.gradle.kts` MUST adhere to these rules.

## 1. Structure
All versions must follow Semantic Versioning (SemVer) format:
```
MAJOR.MINOR.PATCH[-SUFFIX]
```

* **MAJOR** (e.g., `1.0.0`): Bumped for large-scale changes, complete app redesigns, or backwards-incompatible API changes (e.g., replacement of core xray components).
* **MINOR** (e.g., `0.7.0`): Bumped for new features, new screens, or additional configurations (e.g., adding dynamic profiles, DNS strategic options, or custom routing profiles).
* **PATCH** (e.g., `0.7.1`): Bumped for bug fixes, code refactoring, optimizations, layout adjustments, and translation updates.

## 2. Suffix Guidelines
* **`-alpha`**: Experimental, initially developed features that are highly unstable. Used only for private dev builds (e.g., `0.5.0-alpha`).
* **`-Beta`**: Feature-complete builds containing new changes that are ready for testing (e.g., `0.7.0-Beta`).
* **`-RC1`, `-RC2`**: Release Candidates for release validation.
* **No Suffix**: Stable production-ready releases (e.g., `0.7.0`).

## 3. versionCode Increment Rule
The `versionCode` in `app/build.gradle.kts` is an integer that uniquely identifies the build.
* **CRITICAL**: On **any** change to `versionName` or when compiling a new test/beta release build, `versionCode` **MUST be incremented by exactly 1** (e.g., from `11` to `12`) to ensure Android packages update correctly on physical devices.
