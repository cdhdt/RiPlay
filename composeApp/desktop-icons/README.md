# Desktop icons

Consumed by the `nativeDistributions` block in `composeApp/build.gradle.kts`. jpackage accepts
neither SVG nor the Android vector in `composeResources/drawable/app_icon.xml`, so these raster
files are checked in.

Regenerate from the source when the logo changes:

```sh
magick -background none -density 512 assets/design/latest/app_icon.svg \
  -resize 512x512 composeApp/desktop-icons/icon-linux.png

magick -background none -density 512 assets/design/latest/app_icon.svg \
  -define icon:auto-resize=256,128,64,48,32,16 composeApp/desktop-icons/icon-windows.ico
```

Check the output afterwards: without an `rsvg` or `inkscape` delegate, ImageMagick can silently
produce a blank or clipped image.

No `.icns`, since macOS is not a build target.
