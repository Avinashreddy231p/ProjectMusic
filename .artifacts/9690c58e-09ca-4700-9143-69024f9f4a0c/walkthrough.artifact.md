# Walkthrough - "Roll In & Out" Splash Animation

I have implemented the custom dynamic splash animation sequence as requested.

## Changes Summary

### 🎬 Animation Sequence
1.  **Entrance (Roll In)**:
    - The logo enters from the **top-left** (-108, -108) while rotating 360 degrees.
    - Achieved using a high-performance Animated Vector Drawable (AVD) and the `roll_in_animator.xml`.
2.  **The Hold**:
    - The logo remains perfectly centered for exactly **2 seconds**.
    - Managed via `setKeepOnScreenCondition` in `MainActivity`.
3.  **Exit (Roll Out)**:
    - The logo exits toward the **top-right** of the screen while rotating another 360 degrees.
    - Implemented using the `setOnExitAnimationListener` and ViewPropertyAnimator for a smooth, fast transition.

### 🛠️ Technical Details
- **MainActivity**: Updated to precisely control the splash lifecycle and the exit transition.
- **Resource Optimized**: All entrance animations are hardware-accelerated vectors.
- **Timing**: 500ms Roll In -> 2000ms Hold -> 500ms Roll Out.

## How to Verify
1.  Perform a **Clean & Rebuild** in Android Studio.
2.  Launch the app from the icon.
3.  Observe the logo rolling in from the corner, pausing, and then rolling away to the other corner.

---

> [!TIP]
> The "Roll Out" uses the full screen width and height as coordinates, ensuring it clears the display regardless of device size.
