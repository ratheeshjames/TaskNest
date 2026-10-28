# TaskNest v1.0.0 — Generated APK & Release Artifacts

- **Tag:** `v1.0.0`
- **Release Title:** `TaskNest v1.0.0 — Material 3 Expressive Offline-First To-Do Suite`
- **Application ID:** `com.aistudio.tasknest.vqxklp`
- **Version Code:** `1`
- **Version Name:** `1.0.0`
- **Min SDK / Target SDK:** `26` / `36`

## Creating the Git Tag & GitHub Release

Run the following commands from the repository root after exporting or pushing to GitHub:

```bash
# 1. Create an annotated Git tag for v1.0.0
git tag -a v1.0.0 -m "TaskNest v1.0.0 — Initial production release"

# 2. Push the branch and tag to GitHub
git push origin main --tags

# 3. Publish the GitHub Release with the generated APK and release notes using GitHub CLI
gh release create v1.0.0 \
  "Generated APKs/TaskNest-v1.0.0-release.apk" \
  --title "TaskNest v1.0.0 — Material 3 Expressive Offline-First To-Do Suite" \
  --notes-file RELEASE_NOTES.md
```
