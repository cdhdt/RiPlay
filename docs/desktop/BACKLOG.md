# RiPlay Desktop — Backlog produit

> Statut au 2026-07-21. Toute affirmation « côté Android » renvoie à un chemin de fichier réel vérifié dans le dépôt.
> Chemins Android relatifs à `androidApp/src/androidMain/kotlin/it/fast4x/riplay/`.
> Chemins desktop relatifs à `composeApp/src/jvmMain/kotlin/it/fast4x/riplay/`.

## Vision & scope du run

Livrer un **lecteur core desktop solide** : navigation accueil / recherche / artiste / album / playlist, lecture fiable via Chromium embarqué (JCEF/KCEF pilotant la vraie page music.youtube.com), contrôles complets (play/pause/suivant/précédent/seek/volume/shuffle/repeat), file d'attente, bibliothèque et paroles. Design cible Spotify sombre + identité verte RiPlay. Tout le reste (statistiques, listener level, events, podcasts, réglages avancés, extras sociaux) est catalogué et priorisé pour les runs suivants.

## Stratégies de portage (rappel)

- **page** — exposé gratuitement via le bridge JS de la page YT Music dans CEF (`player/webview/CefPlayerController.kt` pilote déjà `document.querySelector('video')` et `#movie_player`).
- **InnerTube** — à construire via le module partagé `extensions/environment` (API YouTube ; entrée `extensions/environment/src/main/kotlin/it/fast4x/environment/Environment.kt`).
- **natif** — à construire en Compose Desktop / Room (persistance partagée `composeApp/src/commonMain/kotlin/database/`).

## État desktop existant (acquis, ne pas re-ticketer)

- Lecture audio CEF : `player/webview/CefPlayerController.kt`, `player/webview/CefRuntime.kt`.
- `PlayerController` (interface `player/player/PlayerController.kt`) expose déjà `load/play/pause/stop/seekTo/setVolume/toggleSound` ; `PlayerState` = isPlaying, isMuted, volume, timestamp, duration (`player/player/PlayerState.kt`). **Manquants : next, previous, shuffle, repeat, queue.**
- Navigation catalogue partielle : `ui/ThreeColumnsApp.kt` gère `PageType` = QUICKPICS/ARTIST/ALBUM/PLAYLIST/MOOD (`enums/PageType.kt`). Écrans présents : `ui/screens/QuickPicsScreen.kt`, `ArtistScreen.kt`, `AlbumScreen.kt`, `PlaylistScreen.kt`, `MoodScreen.kt`, `ArtistsScreen.kt`. **Absents : recherche, bibliothèque, file d'attente, paroles.**
- InnerTube déjà branché pour QuickPicks/artiste/album/playlist/moods. API dispo non encore câblée UI : `Environment.next()`, `library()`, `SearchPage`, `SearchSuggestions`, `requests/Lyrics.kt`, `requests/RelatedSongs.kt`.
- Room partagé : entités `Song`, `Queues`, `QueuedMediaItem`, `Playlist`, `SongPlaylistMap`, `Album`, `Artist`, `Lyrics`, `Event`, `SearchQuery`, `Format` (`composeApp/src/commonMain/kotlin/database/entities/`).

---

## MUST — Lecteur core (livré ce run)

Découpé en tickets actionnables, chacun = 1 unité de dev livrable de bout en bout.

### EPIC A — Lecture & contrôles

| ID | Titre | Description | Prio | Cx | Source Android | Stratégie | Dépendances |
|----|-------|-------------|------|----|----------------|-----------|-------------|
| A1 | Contrôles suivant / précédent | Ajouter `next()`/`previous()` à `PlayerController` + impl CEF ; pilotent le chargement du titre voisin de la file. | Must | M | `ui/screens/player/unified/components/controls/` | natif (drive `CefPlayerController.load`) | F1 |
| A2 | Shuffle & repeat | État shuffle + repeat (off/all/one) dans `PlayerState`, appliqués à l'ordre/bouclage de la file. | Must | M | `ui/screens/player/unified/UnifiedGetControls.kt` | natif | F1, A1 |
| A3 | Barre de contrôle complète (player bar) | Barre bas d'écran : titre/artiste/thumbnail, play/pause, next/prev, seekbar draggable, volume, shuffle, repeat. Étendre l'existant `ui/components/PlayerEssential.kt`. | Must | L | `ui/screens/player/unified/UnifiedMiniPlayer.kt`, `UnifiedGetSeekbar.kt` | natif (état déjà exposé par CEF) | A1, A2 |
| A4 | Radio / autoplay en fin de file | Quand la file se vide, alimenter la suite via titres liés. | Must | M | `utils/PlayerUtils.kt` | InnerTube (`Environment.next`, `requests/RelatedSongs.kt`) | F1, A1 |
| A5 | Fiabilité lecture (reprise, anti-suspension, fin de titre) | Détecter fin de piste (`ended`) pour enchaîner, durcir la reprise off-screen déjà amorcée dans `tickScript`. | Must | M | `services/playback/PlayerService.kt` | page (`CefPlayerController` pollLoop/tickScript) | A1 |

### EPIC B — File d'attente

| ID | Titre | Description | Prio | Cx | Source Android | Stratégie | Dépendances |
|----|-------|-------------|------|----|----------------|-----------|-------------|
| F1 | Modèle de file + persistance | File en mémoire (liste ordonnée + index courant) persistée via entités `Queues`/`QueuedMediaItem` déjà présentes en commonMain. | Must | M | `services/playback/PlayerStatePersistence.kt` | natif (Room partagé) | — |
| F2 | Panneau file d'attente (UI) | Colonne droite : liste des titres, titre courant surligné, retirer un item, réordonner (drag). | Must | M | `ui/screens/player/common/Queue.kt` | natif | F1, A3 |
| F3 | Actions « lire » / « ajouter à la file » | Depuis song/album/playlist : lire maintenant (remplace la file) ou ajouter en fin. Câbler sur les écrans catalogue existants. | Must | S | `utils/QueueUtils.kt` | natif | F1 |

### EPIC C — Navigation

| ID | Titre | Description | Prio | Cx | Source Android | Stratégie | Dépendances |
|----|-------|-------------|------|----|----------------|-----------|-------------|
| C1 | Rail de navigation gauche (Spotify-like) | Rail persistant : Accueil, Recherche, Bibliothèque. Remplacer le pilotage par `showPageType` local de `ThreeColumnsApp.kt` par une nav claire. | Must | M | `ui/screens/home/HomeScreen.kt` | natif | — |
| C2 | Accueil / QuickPicks (consolidation) | Vérifier/finaliser l'écran d'accueil existant (sections carousels, moods). | Must | S | `ui/screens/home/homepages/HomePage.kt` | InnerTube (déjà câblé) | C1 |
| C3 | Écran artiste (consolidation) | Header + top titres + albums + singles + « lire »/« file » branchés. | Must | S | `ui/screens/artist/` | InnerTube (`requests/ArtistPage.kt`) déjà câblé | C1, F3 |
| C4 | Écran album (consolidation) | Tracklist + actions lecture/file. | Must | S | `ui/screens/album/` | InnerTube (`requests/AlbumPage.kt`) déjà câblé | C1, F3 |
| C5 | Écran playlist (consolidation) | Tracklist + pagination + actions lecture/file. | Must | S | `ui/screens/playlist/` | InnerTube (`requests/PlaylistPage.kt`, `PlaylistContinuationPage.kt`) déjà câblé | C1, F3 |

### EPIC D — Recherche

| ID | Titre | Description | Prio | Cx | Source Android | Stratégie | Dépendances |
|----|-------|-------------|------|----|----------------|-----------|-------------|
| D1 | Barre de recherche + suggestions | Champ de recherche avec autocomplétion. | Must | M | `ui/screens/search/OnlineSearch.kt`, `SearchScreen.kt` | InnerTube (`requests/SearchSuggestions.kt`) | C1 |
| D2 | Résultats de recherche multi-onglets | Résultats titres/albums/artistes/playlists, chaque item cliquable vers son écran + actions lecture/file. | Must | L | `ui/screens/search/SearchResultsContent.kt`, `ItemsPage.kt` | InnerTube (`requests/SearchPage.kt`) | D1, C3, C4, C5, F3 |
| D3 | Historique de recherche | Persister/afficher les requêtes récentes. | Must | S | `ui/screens/search/SearchScreen.kt` | natif (entité `SearchQuery`) | D1 |

### EPIC E — Bibliothèque

| ID | Titre | Description | Prio | Cx | Source Android | Stratégie | Dépendances |
|----|-------|-------------|------|----|----------------|-----------|-------------|
| E1 | Like / favoris titres | Bouton like sur la player bar et les listes, persisté. | Must | S | `utils/LikeUtils.kt` | natif (Room) | A3 |
| E2 | Écran Bibliothèque (playlists/albums/artistes/titres locaux) | Vue bibliothèque à onglets alimentée par Room. | Must | M | `ui/screens/home/HomePlaylists.kt`, `HomeAlbums.kt`, `HomeArtists.kt`, `HomeSongs.kt` | natif (Room) | C1 |
| E3 | Playlists locales (CRUD) | Créer / renommer / supprimer une playlist locale, ajouter/retirer des titres. | Must | M | `ui/screens/localplaylist/` | natif (Room `Playlist`/`SongPlaylistMap`) | E2 |

### EPIC G — Paroles

| ID | Titre | Description | Prio | Cx | Source Android | Stratégie | Dépendances |
|----|-------|-------------|------|----|----------------|-----------|-------------|
| G1 | Récupération paroles (providers) | Fetch paroles (synchro + texte) pour le titre courant. | Must | M | `extensions/lyricshelper/`, `extensions/lrclib/`, `extensions/kugou/` | InnerTube + providers (`requests/Lyrics.kt`, LrcLib, KuGou) | A3 |
| G2 | Affichage paroles (panneau + synchro) | Panneau paroles, défilement synchronisé sur `timestamp` de `PlayerState`, persistance `Lyrics`. | Must | M | `ui/screens/player/common/Lyrics.kt` | natif | G1 |

---

## SHOULD — après le core

| ID | Titre | Description | Prio | Cx | Source Android | Stratégie | Dépendances |
|----|-------|-------------|------|----|----------------|-----------|-------------|
| S1 | Thème Spotify sombre + vert RiPlay | Palette sombre, accent vert, typographie ; s'appuie sur `styling/ColorPalette.kt`, `ui/theme/`. | Should | M | `ui/screens/settings/UiSettings.kt` | natif | — |
| S2 | Moods & genres (écran dédié) | Navigation moods/chips complète. | Should | S | `ui/screens/moodandchip/` | InnerTube (`requests/DiscoverPage.kt`) | C1 |
| S3 | Charts / nouveautés | Palmarès + nouvelles sorties des artistes suivis. | Should | M | `ui/screens/newreleases/` | InnerTube (`requests/ChartsPage.kt`, `NewReleaseAlbumPage.kt`) | C1 |
| S4 | Historique d'écoute | Écran historique local + sync. | Should | M | `ui/screens/history/` | InnerTube (`requests/HistoryPage.kt`) + natif | E2 |
| S5 | Réglages généraux | Écran réglages desktop (langue, thème, dossier, lecture). | Should | M | `ui/screens/settings/GeneralSettings.kt`, `SettingsScreen.kt` | natif | S1 |
| S6 | Connexion compte YouTube | Login pour bibliothèque perso / playlists YT. | Should | L | `extensions/accountlogin/`, `ui/screens/settings/AccountsSettings.kt` | InnerTube (`Environment` account) | E2 |
| S7 | Sync playlists en ligne | Import/sync des playlists du compte. | Should | L | `utils/OnlineSyncUtils.kt` | InnerTube (`Environment.library`, `addToPlaylist`) | S6, E3 |
| S8 | Sleep timer | Arrêt programmé de la lecture. | Should | S | `utils/TimerUtils.kt` | natif | A3 |
| S9 | Vitesse / pitch de lecture | Contrôles playbackRate (page) ; pitch best-effort. | Should | S | `services/helpers/AudioDRCHelper.kt` | page (`video.playbackRate`) | A3 |
| S10 | Backup / restore base | Export/import de la base Room. | Should | M | `extensions/databasebackup/` | natif | E2 |
| S11 | Recherche locale (bibliothèque) | Filtrer titres/playlists locaux. | Should | S | `ui/screens/search/LocalSongSearch.kt` | natif | E2, D1 |

---

## COULD — extras / différenciation

| ID | Titre | Description | Prio | Cx | Source Android | Stratégie | Dépendances |
|----|-------|-------------|------|----|----------------|-----------|-------------|
| K1 | Statistiques d'écoute | Tableaux top titres/artistes/albums. | Could | M | `ui/screens/statistics/` | natif (Room) | S4 |
| K2 | Listener level / badges | Niveau d'écoute mensuel/annuel. | Could | M | `extensions/listenerlevel/` | natif | K1 |
| K3 | Rewind (« year in music ») | Rétro annuelle. | Could | L | `extensions/rewind/` | natif | K1 |
| K4 | Events (daily/weekly, sorties artistes) | Notifications d'événements catalogue. | Could | M | `ui/screens/events/`, `extensions/scheduled/` | InnerTube + natif | S3 |
| K5 | Blacklist (ignorer artiste/album/titre) | Filtrage de contenu. | Could | S | `ui/screens/blacklist/`, `utils/BlacklistUtils.kt` | natif | E2 |
| K6 | Podcasts | Navigation/lecture podcasts. | Could | L | `ui/screens/podcast/` | InnerTube (`requests/PodcastPage.kt`) | C1 |
| K7 | Édition / traduction de paroles | Éditer et traduire les paroles. | Could | M | `utils/LyricsUtils.kt` | natif + lib Translator | G2 |
| K8 | Égaliseur / audio FX | EQ, bassboost, normalisation. **Limité** : CEF ne donne pas accès au pipeline audio → best-effort WebAudio uniquement. | Could | XL | `extensions/equalizer/`, `services/helpers/EqualizerHelper.kt` | page (WebAudio, incertain) | A3 |
| K9 | Fichiers locaux (on-device) | Lire des fichiers audio locaux. **Nécessite** un moteur audio natif (vlcj présent mais non finalisé : `player/vlcj/`). | Could | XL | `ui/screens/ondevice/`, `utils/OnDeviceUtils.kt` | natif (vlcj) | F1 |
| K10 | Reconnaissance musicale | Identifier titre via micro. | Could | L | `extensions/recorders/AudioRecorder.kt`, `extensions/audiotag/` | natif (API AudioTag) | — |
| K11 | Discord Rich Presence | Statut « en écoute » Discord. | Could | S | `extensions/discord/` | natif | A3 |
| K12 | Last.fm scrobbling | Scrobble des écoutes. | Could | M | `extensions/lastfm/` | natif (API Last.fm) | A5 |
| K13 | Partage rapide | Copier/partager le lien du titre. | Could | S | `extensions/fastshare/`, `utils/ClipBoardUtils.kt` | natif | A3 |
| K14 | Visualizer | Visualisation audio. **Limité** : pas d'accès au flux PCM depuis CEF. | Could | XL | `extensions/nextvisualizer/`, `ui/screens/player/common/NextVisualizer.kt` | page (WebAudio, incertain) | A3 |
| K15 | Check update desktop | Vérif/téléchargement de mise à jour. | Could | S | `extensions/updater/` | natif | S5 |
| K16 | Onboarding / welcome | Écran de première ouverture. | Could | S | `ui/screens/onboarding/`, `ui/screens/welcome/` | natif | C1 |
| K17 | Stats for nerds | Panneau debug lecture (déjà amorcé desktop). | Could | S | `ui/screens/player/common/StatsForNerds.kt` | natif | A3 |

---

## Hors périmètre desktop (WON'T)

| Feature | Source Android | Justification |
|---------|----------------|---------------|
| Android Auto | `MainActivity.kt`, `services/playback/PlayerMediaBrowserService.kt` | API `androidx.car` / MediaBrowserService spécifiques véhicule Android. |
| Android TV | `ui/screens/player/unified/TvUnifiedPlayer.kt` | Leanback/TV runtime Android only. |
| Widgets Glance | `ui/widgets/PlayerVerticalWidget.kt`, `PlayerHorizontalWidget.kt` | Home-screen widgets = AppWidget/Glance, notion inexistante desktop. |
| Picture-in-Picture | `extensions/pip/`, `utils/PipUtils.kt` | PiP = API fenêtrage Android ; pas d'équivalent utile ici. |
| Sonnerie (ringtone) | `utils/RingtoneUtils.kt` | Définir une sonnerie = API téléphonie Android. |
| Capteurs / shake | `utils/DeviceUtils.kt` | Pas d'accéléromètre sur desktop. |
| Touches volume physiques | `MainActivity.kt` (key events) | Gestion des boutons volume matériels Android. |
| Bluetooth / casque auto | `services/helpers/BluetoothConnectHelper.kt`, `NoisyAudioHelper.kt` | Détection casque/BT et AudioManager Android. |
| Saisie vocale | `utils/VoiceUtils.kt` | RecognizerIntent Android. |
| Couleurs Monet / Material You | `utils/MonetUtils.kt`, `enums/ColorPaletteName.kt` (Dynamic) | Extraction dynamique Android 12+. Remplacé par thème vert RiPlay fixe (S1). |
| Google Cast | `cast/ritune/`, `extensions/aypcast/` | Cast SDK Android/Play Services. |
| Scan QR caméra | `extensions/qrcodeanalyzer/` | CameraX ; pas de use-case sur desktop. |
| Flavors F-Droid / stores | `README.md` (installation), fastlane | Distribution mobile ; le desktop a son propre packaging (`desktopDistribution/`). |
| Chaquopy (Python embarqué) | `extensions/chaquopy/` | Runtime Python Android only ; non requis pour le core. |

---

## Ordre de bataille recommandé (tier Must)

Séquence tenant compte des dépendances (F1 débloque toute la file/lecture ; nav avant recherche ; recherche/écrans avant leurs actions) :

1. **F1** — modèle de file + persistance (socle de tout le reste).
2. **A1 → A2** — next/prev puis shuffle/repeat (dépendent de F1).
3. **A5** — fiabilité lecture / enchaînement fin de titre.
4. **A3** — player bar complète (consomme A1/A2, expose les contrôles).
5. **F3 → F2** — actions lire/ajouter, puis panneau file.
6. **A4** — radio/autoplay (InnerTube) une fois la file stable.
7. **C1** — rail de navigation, puis **C2/C3/C4/C5** consolidation accueil/artiste/album/playlist (câbler F3 dessus).
8. **D1 → D2 → D3** — recherche (suggestions, résultats, historique).
9. **E1 → E2 → E3** — like, bibliothèque, playlists locales.
10. **G1 → G2** — paroles (fetch puis affichage synchronisé).

Chemin critique : **F1 → A1/A3 → C1 → D2**. Les EPICs Bibliothèque (E) et Paroles (G) sont parallélisables dès que la player bar (A3) et le rail (C1) existent.
