# Implementation Plan - Fix Last.fm Login Error

The user is experiencing a generic "unexpected error" when trying to log in to Last.fm. This is likely due to the current login flow being overly sensitive to failures in fetching user information before actually authenticating.

## User Review Required

> [!IMPORTANT]
> The fix involves changing the sequence of Last.fm login calls. Instead of fetching user profile details first, the app will now attempt to establish a session with the provided credentials directly. This improves compatibility with different login methods (like using an email address) and makes the process more robust against minor API errors.

## Proposed Changes

### Data Layer

#### [MODIFY] [NetworkRepository.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/data/local/repository/NetworkRepository.kt)
- Refactor `loginToLastFm` to:
    1. Call `lastFmService.createSession(username, password)` first.
    2. If successful, use the canonical username from the session response.
    3. Attempt to fetch `userInfo` for the profile URL and images.
    4. If `userInfo` fails (e.g. user entered email, or transient API error), gracefully fall back to a default `LastFmUser` object using the session's username.
    5. Save the session and update the login state.

## Verification Plan

### Manual Verification
- Attempt to log in to Last.fm using a valid username and password.
- Attempt to log in to Last.fm using a valid email address and password (if supported by the account).
- Verify that entering incorrect credentials still shows the appropriate "Invalid credentials" error message instead of a generic one.
- Verify that the profile URL is still correctly set and clickable after a successful login.
