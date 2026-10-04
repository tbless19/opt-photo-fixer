# OPT Photo Fixer

An Android app and website that reframe a headshot to the U.S. 2×2 inch passport-style photo rules
used for OPT (Form I-765), and saves a 600×600 JPEG to your phone.

## What it does

- Pick a photo from your gallery, or share one into the app from Google Photos / Gallery.
- Tap the top of your hair and the bottom of your chin. The app scales and centers the photo so your head is 50–69% of the frame.
- Adjust red, green, blue and brightness, or tap **Auto white background**, to get a white or off-white background.
- See pass/check/fix results for head size, space above the head, filled-in edges, resolution and background color.
- **Save as JPG** writes the photo to `Pictures/OPT Photo` in the app, or downloads it on the website. The file is kept under 240 KB.

Everything runs on your device. The photo is never uploaded, and the Android app has no internet permission.

## Use it on the web

The website is published with GitHub Pages at **https://tbless19.github.io/opt-photo-fixer/**.
It works in any modern browser. On a phone, open it in Chrome and choose **⋮ › Add to Home screen**
(or **Install app**) to get an app icon. After the first visit it also works offline.

One-time setup: in this repository go to **Settings › Pages** and set **Source** to **GitHub Actions**.
After that, every change under `web/` republishes the site.

## Install the Android app

1. Open this repository's **Releases** page on your Android phone (the newest one is at the top).
2. Download **OPT-Photo-Fixer.apk**.
3. Open the download. The first time, Android asks you to allow installs from your browser or Files app; allow it.
4. Open **OPT Photo** from your app drawer.

Every push to `main` builds a new APK automatically (see the **Actions** tab) and publishes it as a new release.
New builds install over the old one.

## Limits

The app crops, scales and color-corrects. It can't fix uneven lighting, shadows or expression, and it can't
create missing parts of your shoulders. If it says a large part of the frame is filled in, retake the photo
from farther away. Check the final file with the U.S. State Department's online photo tool before submitting.

## Project layout

- `web/index.html` holds the whole tool (HTML, CSS and JavaScript). The website and the Android app both use this one file, so a fix applies to both.
- `web/manifest.webmanifest`, `web/sw.js` and `web/icons/` make the website installable and usable offline.
- `app/src/main/java/com/tbless19/optphoto/MainActivity.java` is the Android wrapper: the photo picker, gallery sharing and saving to Photos.
- `app/signing.keystore` is a personal signing key so updates install over earlier versions. It is not a Play Store key. Replace it before you publish anywhere.
