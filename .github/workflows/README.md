# GitHub Actions Workflows

## build.yml - Automatic APK Build

### Purpose
This workflow automatically builds the Android APK on every commit to verify that the application compiles correctly.

### Triggers
- **Push** to any branch
- **Pull Request** to any branch

### What it does
1. Checks out the code
2. Sets up JDK 17 (required for Android builds)
3. Grants execute permission to gradlew
4. Builds both Debug and Release APKs
5. Uploads APK files as artifacts (kept for 30 days)
6. Creates a build summary

### Artifacts
After each successful build, you can download:
- **app-debug-{commit-sha}.apk** - Debug version with full logging
- **app-release-{commit-sha}.apk** - Release version (unsigned)

### Important Notes
- ⚠️ **This workflow does NOT create tags** - Tags are created manually
- ✅ This prevents situations where a broken APK is tagged as a release
- 📦 APK artifacts are kept for 30 days and then automatically deleted
- 🔍 Build failures will be visible in the Actions tab

### Accessing Build Artifacts
1. Go to the **Actions** tab in GitHub
2. Click on the workflow run you want
3. Scroll down to **Artifacts** section
4. Download the APK file you need

### Version Management
Current version is managed in:
- `app/build.gradle` - `versionName`
- `app/src/main/java/com/vovaplusexp/gpscontroller/BuildConfig.java` - `VERSION_NAME`

Both should be kept in sync. Current version: **0.0-dev1**

### Creating Releases
When ready to create a release:
1. Manually create and push a tag: `git tag v0.1.0 && git push origin v0.1.0`
2. Create a GitHub Release from that tag
3. Attach the APK from the corresponding build artifacts
4. Update version numbers for next development cycle
