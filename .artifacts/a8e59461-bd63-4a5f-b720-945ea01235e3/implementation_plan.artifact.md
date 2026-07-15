# Implementation Plan - Run Desktop Head Unit (DHU)

Launch the Android Auto Desktop Head Unit (DHU) emulator to test the recent lyrics and library redesign.

## User Review Required

> [!IMPORTANT]
> **Before proceeding, please ensure you have followed these steps on your mobile device (`2f2621ac`):**
> 1.  **Enable Android Auto Developer Mode**: Open Android Auto settings, scroll to the bottom, and tap "Version" 10 times.
> 2.  **Start Head Unit Server**: In Android Auto settings, tap the three-dot menu in the top right and select **Start head unit server**.
> 3.  **Unlock Device**: Ensure your phone screen is unlocked.

## Proposed Steps

### 1. Port Forwarding
- Forward socket connections from your computer's port 5277 to the mobile device.
- **Command**: `adb -s 2f2621ac forward tcp:5277 tcp:5277` (Already completed)

### 2. Launch DHU
- Start the DHU binary from the Android SDK.
- **Path**: `C:\Users\Avina\AppData\Local\Android\Sdk\extras\google\auto\desktop-head-unit.exe`
- **Action**: Run the executable as a background process.

## Verification Plan

### Manual Verification
- A window should appear on your desktop emulating the Android Auto head unit.
- If the screen is blank, please check your phone for any permission prompts or terms of service agreements.
- Navigate to "Booming Music" and verify the new UI changes.
