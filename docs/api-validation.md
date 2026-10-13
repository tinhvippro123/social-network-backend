# Shared API validation and test setup

This prerequisite isolates application tests in H2, makes the legacy local database helper opt-in, enforces active-account checks for JWTs, normalizes malformed input errors and declares shared error codes and route policies used by the feature PRs. It does not add feature endpoints.

Run `gradlew.bat build`. Local application.yml changes are not included.
