# Fix: Unresolved reference 'R' in SettingsViewModel

The build was failing due to a missing import for the generated `R` class in `SettingsViewModel.kt`.

## Changes

### [SettingsViewModel.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/settings/SettingsViewModel.kt)

Added the missing import for `com.mardous.projectmusic.R` to allow access to Android resources.

```diff
 package com.mardous.projectmusic.ui.screen.settings

 import android.content.SharedPreferences
 import androidx.lifecycle.ViewModel
 import androidx.lifecycle.viewModelScope
 import androidx.appcompat.app.AppCompatDelegate
 import androidx.core.os.LocaleListCompat
+import com.mardous.projectmusic.R
 import com.mardous.projectmusic.data.model.network.NetworkFeature
 import com.mardous.projectmusic.util.*
```

## Verification Results

### Automated Tests
- Ran `:app:compileFdroidDebugKotlin` which now completes successfully.

```
$ ./gradlew :app:compileFdroidDebugKotlin
...
BUILD SUCCESSFUL in 5s
```
