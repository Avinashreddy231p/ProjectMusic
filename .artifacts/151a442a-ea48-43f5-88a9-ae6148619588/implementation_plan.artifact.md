# Update Icon in Wiki

The user wants to update the app icon in the project wiki. The project wiki is managed in the `.wiki_init` directory, which is a separate Git repository for GitHub Wikis.

## Proposed Changes

### Wiki Documentation

#### [MODIFY] [Home.md](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/.wiki_init/Home.md)

Update the Wiki home page to include the project header and the new square icon. Since the Wiki is a separate repository on GitHub, we will use an absolute URL to the main repository's raw content to ensure the icon renders correctly.

**New Header:**
```markdown
# 🎵 Project Music (Booming Music)

<p align="center">
  <img src="https://raw.githubusercontent.com/Avinashreddy231p/ProjectMusic/master/metadata/en-US/images/icon.svg" width="128">
</p>
```

## Verification Plan

### Manual Verification
- Review the content of `.wiki_init/Home.md` to ensure it matches the branding in `README.md`.
- Note: Final verification requires pushing the changes to the GitHub Wiki repository, which should be done by the user.
