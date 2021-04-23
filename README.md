# Tinker Hot-fix Example

Scenarios using Tinker look like this:
1. Code using Tinker convention
2. Find a bug after release
3. Fix the bug
4. Use Tinker to build path apk
5. Publish apk using predefined API in your code
6. Now user can apply the patch without the need to reinstall

## Code using Tinker convention

Use guides and wikis found on Tinker library Github page

## Find a bug after release

By clicking on the "Crash" button, an unhandled exception will occur and app will be force closed.

## Fix the bug

Fix the code by commenting out the problematic part and uncommenting the Toast message indicating successful bug elimination.

## Use Tinker to build path apk

Use tinker gradle options to build a patch.

![](assets/tinker-gradle.PNG)

Patched apks will be available at 'build/outputs/tinkerPatch'  by default after the Tinker build.

> Note: You should modify the gradle script to patch the exact apk that users have installed. You should change 'tinkerOldApkPath', 'tinkerApplyResourcePath', and 'oldApk' variables in your gradle script.

## Publish apk using predefined API in your code

We used [Beeceptor](https://beeceptor.com/) as our Rest API endpoint.

App basically checks if there is any patch available everytime it opens. If there is a new patch available, user will be prompted to download the patch. 

Our API response model:

```json
{
   "patch_available": true,
   "patch_id": "p_1157",
   "patch_url":  "https://bayanbox.ir/download/1689140552403622929/patch-signed-7zip.apk"
}
```

If 'patch_available' is 'true' and 'patch_id' is greater than current patch_id, There is an update!

## Now user can apply the patch without the need to reinstall

The process of downloading and applying the new patch can all be done in the background but for the sake of showing the process each time a new patch is available, user will be prompted to give consent to downloading the patch and installing it.


# Libraries and Resources Used:
[Tinker Hot-fix solution by Tencent](https://github.com/Tencent/tinker)  
[Retrofit](https://github.com/square/retrofit)  
[PR-Downloader](https://github.com/MindorksOpenSource/PRDownloader)  
[Yasham Ihsan Git Repository](https://github.com/iyashamihsan)  
[Tinker Hot-fix solution Wiki's English translation](https://www.programmersought.com/article/26003178715/)
