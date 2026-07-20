# Icônes desktop

Consommées par le bloc `nativeDistributions` de `composeApp/build.gradle.kts`. `jpackage` n'accepte
ni SVG ni les vecteurs Android (`composeResources/drawable/app_icon.xml`) : il lui faut du `.png`
sous Linux et du `.ico` sous Windows, d'où ces fichiers binaires versionnés.

Régénérer depuis la source si le logo change :

```sh
magick -background none -density 512 assets/design/latest/app_icon.svg \
  -resize 512x512 composeApp/desktop-icons/icon-linux.png

magick -background none -density 512 assets/design/latest/app_icon.svg \
  -define icon:auto-resize=256,128,64,48,32,16 composeApp/desktop-icons/icon-windows.ico
```

Vérifier le rendu après coup : ImageMagick sans délégué `rsvg` ou `inkscape` sait produire une image
vide ou tronquée sans le signaler.

Pas de `.icns` : macOS n'est pas une cible, inutile de maintenir un asset pour une plateforme qu'on
ne construit pas.
