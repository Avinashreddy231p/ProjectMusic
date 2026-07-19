# Fix AnimatedVisibility Implicit Receiver Error

The project is failing to build due to a compiler error in `AssignToMetadataDialog.kt`. The error occurs because `AnimatedVisibility` is called inside a `Box` that is nested within a `Column`. This creates an ambiguity between the top-level `AnimatedVisibility` and the `ColumnScope.AnimatedVisibility` extension function, and the `@LayoutScopeMarker` prevents the implicit use of the `ColumnScope` receiver inside the `BoxScope`.

## Proposed Changes

### [Component Name] UI Dialogs

#### [MODIFY] [AssignToMetadataDialog.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/dialogs/metadata/AssignToMetadataDialog.kt)

- Disambiguate the `AnimatedVisibility` call by using its fully qualified name: `androidx.compose.animation.AnimatedVisibility`.

## Verification Plan

### Automated Tests
- Run the Gradle task `:app:compileFdroidDebugKotlin` to verify the fix.
```powershell
./gradlew :app:compileFdroidDebugKotlin
```

### Manual Verification
- None required as this is a compiler error fix.
