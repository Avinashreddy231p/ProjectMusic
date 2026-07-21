# Walkthrough - Fixed Unresolved reference 'R' in LibraryViewModel.kt

The build error `Unresolved reference 'R'` in `LibraryViewModel.kt` was caused by a missing import statement for the generated `R` class.

## Changes Made

### [LibraryViewModel.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/library/LibraryViewModel.kt)

- Added `import com.mardous.projectmusic.R` to the imports section.

```diff
 import com.mardous.projectmusic.coil.CustomPlaylistImageManager
+import com.mardous.projectmusic.R
 import com.mardous.projectmusic.core.model.LibraryMargin
```

## Verification Results

### Automated Tests
- Ran `./gradlew :app:compileDebugKotlin` which now completes successfully.

```bash
$ ./gradlew :app:compileDebugKotlin
BUILD SUCCESSFUL in 12s
```
