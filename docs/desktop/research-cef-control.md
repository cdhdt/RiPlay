# Piloter music.youtube.com via JS dans le CEF caché — état de l'art

Contexte : `CefPlayerController.kt` charge la vraie page `music.youtube.com/watch?v=...` dans un
Chromium embarqué (JCEF) et pilote aujourd'hui uniquement play/pause/stop/seek/volume/mute via
`document.querySelector('video')` et `document.getElementById('movie_player')` (voir `tickScript()`
et `js()` dans ce fichier). Objectif de cette note : lister ce que `#movie_player` (et le DOM YT
Music) exposent déjà pour next/prev, queue, shuffle/repeat, métadonnées, paroles et chargement de
morceau — pour éviter de réinventer un lecteur.

**Limite de méthode à signaler tout de suite** : la recherche web (developers.google.com,
th-ch/youtube-music) a été demandée mais l'outil de recherche web a été refusé dans cette session
(la vérification live n'a pas pu être faite). Les points 1, 3, 4, 6 s'appuient donc sur :
(a) le code du dépôt qui exerce déjà `#movie_player` avec succès (`playVideo()` fonctionne, preuve
directe et vérifiée en l'état actuel du code), (b) le fichier `ayp_youtube_player.html` du dépôt,
qui instancie l'**API IFrame officielle YouTube** (`new YT.Player(...)`) et expose déjà
`nextVideo`, `previousVideo`, `setLoop`, `setShuffle`, `getPlaylist`, `getPlaylistIndex`,
`playVideoAt`, `loadVideoById`, `cueVideoById` comme méthodes de l'objet `player` — ce sont les
noms de méthodes documentés de l'API IFrame YouTube (connaissance d'entraînement, non re-vérifiée
en direct ce tour-ci). Chaque affirmation ci-dessous est marquée avec son niveau de confiance réel.

## 1. Suivant / Précédent

**Réponse** : `document.getElementById('movie_player').nextVideo()` /
`.previousVideo()`.

- Source directe et déjà vérifiée dans ce dépôt : `CefPlayerController.kt` ligne 163 utilise déjà
  `document.getElementById('movie_player')` avec succès pour appeler `.playVideo()` sur
  music.youtube.com (donc l'élément existe bien à cet id sur cette page, et expose au moins l'API
  player standard).
- `extensions/ayp/src/main/res/raw/ayp_youtube_player.html` (lignes 325-331) confirme que sur un
  player YouTube construit avec l'API IFrame officielle, l'objet player expose `nextVideo()` et
  `previousVideo()` — ce sont des méthodes documentées de l'API IFrame Player
  (developers.google.com/youtube/iframe_api_reference — **non re-vérifié en direct cette
  session**, connaissance d'entraînement).
- Le player interne de music.youtube.com (`#movie_player`) partage le même cœur JS que le player
  IFrame classique (base.js commun) ; il est rapporté par des projets communautaires (ex.
  th-ch/youtube-music, Electron) qu'ils appellent directement `nextVideo()` /
  `previousVideo()` sur cet élément pour piloter youtube.com/music.youtube.com. **Non vérifié en
  direct cette session** (recherche web refusée) — à confirmer empiriquement (voir section
  recommandations).

**Confiance** : moyenne. `playVideo()` marche déjà (fait vérifié) ; `nextVideo()`/`previousVideo()`
sont très probablement présentes sur le même objet mais ce n'est pas encore testé dans ce dépôt.

**Alternative DOM (fallback)** si les méthodes JS ne se comportent pas comme attendu sur YT Music
(ex. si YT Music gère sa file d'attente côté SPA plutôt que côté player natif) : cliquer les
boutons de la barre de lecture, élément custom `ytmusic-player-bar` (shadow DOM), classes connues
`.next-button` / `.previous-button` (`ytmusic-player-bar.shadowRoot.querySelector('.next-button')
.click()`). **Confiance faible** — noms de classes internes Google, changent sans préavis, non
vérifiés en direct cette session.

## 2. File d'attente (queue)

**Réponse** : deux couches distinctes à ne pas confondre.

- `#movie_player.getPlaylist()` / `.getPlaylistIndex()` : renvoient la playlist/l'index au sens de
  l'API IFrame classique (tableau d'IDs vidéo + index courant). Confirmé utilisé dans
  `ayp_youtube_player.html` ligne 261-267 (`sendVideoIdFromPlaylistIfAvailable`). **Mais** sur
  music.youtube.com, la "queue" visible dans l'UI (radio/autoplay générée par YT Music) est un
  concept **côté SPA** (composant `ytmusic-player-queue`, alimenté par les réponses InnerTube
  `next`), pas forcément synchronisé avec `getPlaylist()` du player natif — surtout quand le
  morceau est chargé par simple navigation `watch?v=ID` (comme le fait déjà
  `CefPlayerController.pollLoop()`, ligne 132) plutôt que par une vraie playlist `list=`.
  **Confiance faible à moyenne sur la synchronisation `getPlaylist()` ↔ queue visible YT Music** —
  point à vérifier empiriquement (log console dans le CEF).
- RiPlay a déjà sa propre notion de morceau courant côté app (pas de queue app-level pour
  l'instant sur desktop : `ThreeColumnsApp.kt` ligne ~102 appelle juste `player.load(videoId)`
  pour le morceau sélectionné, un seul `videoId` en state Compose, aucune liste "next up" gérée
  côté Kotlin desktop aujourd'hui).
- Ajouter/retirer/réordonner la queue YT Music via JS : pas de méthode publique connue et fiable
  sans passer par le DOM (bouton "Ajouter à la file d'attente" dans les menus contextuels,
  `⋮` menu). **Non recherché en profondeur** (hors scope raisonnable sans accès web) —
  **confiance faible**, à traiter comme incertain plutôt que d'inventer une API.

**Confiance globale** : moyenne pour lire le morceau courant (déjà fait dans `tickScript` via
`<video>`/`movie_player`), faible pour lire/modifier la queue YT Music elle-même.

## 3. Shuffle / Repeat

**Réponse** : `#movie_player.setShuffle(bool)` / `.setLoop(bool)` sont les méthodes de l'API IFrame
(confirmées dans `ayp_youtube_player.html` lignes 337-343). Ce sont les méthodes officielles
documentées côté API IFrame YouTube pour une vraie playlist — **non re-vérifiées en direct sur
music.youtube.com cette session**.

**Point d'incertitude explicite** : YT Music a son **propre** état shuffle/repeat (icônes dans la
barre de lecture, 3 états pour repeat : off / all / one) qui pilote la génération de la queue côté
SPA — ce n'est pas garanti que ce soit le même flag que `setLoop`/`setShuffle` du player IFrame
sous-jacent, qui eux s'appliquent à la "playlist" au sens `getPlaylist()`. Sur une navigation
simple `watch?v=`, il n'y a probablement pas de "playlist" IFrame au sens strict, donc
`setShuffle`/`setLoop` pourraient ne rien changer d'observable.

**Confiance** : faible. Recommandé de vérifier par clic DOM plutôt que de miser sur ces méthodes
(voir recommandations).

**Fallback DOM** : boutons dans `ytmusic-player-bar` — shuffle et repeat ont des classes/`id`
internes (non vérifiés cette session). Cliquer le bouton réel reproduit exactement ce qu'un
utilisateur ferait et reste correct quel que soit l'état interne réel de YT Music. **Confiance
moyenne sur la stratégie (cliquer un vrai bouton est toujours fiable), faible sur les sélecteurs
précis** (à récupérer par inspection DOM live, pas par recherche web).

## 4. Métadonnées du morceau courant

**Réponse** : deux sources fiables, déjà à portée :

- **API player** : `#movie_player.getVideoData()` (retourne `{video_id, title, author, ...}` —
  méthode non documentée officiellement mais largement utilisée dans l'écosystème IFrame ; **non
  vérifiée en direct cette session**, confiance moyenne) ; `.getDuration()` / `.getCurrentTime()`
  déjà utilisées de fait dans `tickScript()` via `video.duration` / `video.currentTime` (plus
  simple, déjà en prod dans ce dépôt — **confiance élevée**, c'est du code qui tourne).
  Note : `video.duration`/`currentTime` suffisent déjà pour ce que `CefPlayerController` fait ;
  pas besoin de `getVideoData()` pour la durée/position.
- **Titre/artiste/album/pochette** : RiPlay a déjà une base locale (`db.getSong(videoId)` dans
  `ThreeColumnsApp.kt` ligne 101) et surtout le module `extensions/environment` (InnerTube) qui
  sait déjà récupérer ces métadonnées côté API sans DOM (c'est le mécanisme utilisé pour peupler
  la bibliothèque). **Confiance élevée** — pas besoin de scraper le DOM YT Music pour ça, la donnée
  existe déjà côté app avant même de charger la page.

**Confiance** : élevée pour timestamp/durée (déjà en prod) ; élevée pour titre/artiste/album via
la voie existante (DB locale + `extensions/environment`), pas de nouveau risque à prendre.

## 5. Paroles

**Réponse** : **ne pas scraper l'onglet "Lyrics" du DOM YT Music.** Le dépôt a déjà un chemin API
propre et fonctionnel pour ça, à deux niveaux :

- `extensions/environment/src/main/kotlin/it/fast4x/environment/requests/Lyrics.kt` : appelle
  l'endpoint InnerTube `next` pour récupérer le `browseId` de l'onglet paroles
  (`tabs.tabRenderer.endpoint.browseEndpoint.browseId`, avec un `MUSIC_PAGE_TYPE_TRACK_LYRICS`
  défini dans `models/Endpoint.kt` ligne 65), puis appelle `browse` et lit
  `contents.sectionListRenderer.contents.musicDescriptionShelfRenderer.description.text` — texte
  brut, **non synchronisé**. **Confiance élevée**, code du dépôt, déjà écrit et fonctionnel.
- `extensions/lrclib/.../LrcLib.kt` : `LrcLib.lyrics(artist, title, duration)` interroge
  `lrclib.net/api/search` et renvoie des paroles **synchronisées** (format LRC, parsées en
  `sentences: List<Pair<Long, String>>` par `LrcLib.Lyrics.sentences`, timestamps en ms). Source
  HTTP indépendante, aucun rapport avec le CEF. **Confiance élevée**.
- `extensions/kugou/.../KuGou.kt` : source alternative de paroles synchronisées (même famille
  d'usage que LrcLib côté RiPlay Android, probablement déjà branché comme fallback).

Ces trois chemins sont **indépendants du CEF** : ils utilisent directement les extensions déjà
présentes dans le dépôt et fonctionnent même quand le player CEF n'est pas encore chargé. C'est
la voie la moins chère et la plus fiable — le DOM de l'onglet Lyrics de YT Music n'a même pas
besoin d'être inspecté.

**Confiance globale** : élevée — solution déjà écrite dans le dépôt, à réutiliser telle quelle
côté desktop (pas de nouveau code JS/DOM nécessaire).

## 6. Charger un morceau/une playlist par ID sans reload complet

**Réponse actuelle du dépôt** : `CefPlayerController.pollLoop()` (ligne 130-133) utilise
`browser.loadURL("https://music.youtube.com/watch?v=$target")`, c'est-à-dire une **vraie
navigation de page** (pas SPA), ré-émise tant que `location.href` ne contient pas le videoId
attendu. C'est un fait vérifié du code actuel, pas une hypothèse.

**Alternative envisageable** : `#movie_player.loadVideoById(videoId)` (méthode IFrame officielle,
confirmée utilisée dans `ayp_youtube_player.html` ligne 295-296 côté `loadVideo()`) éviterait le
rechargement complet de page et la navigation SPA de YT Music (avec ses risques de bandeau consent
qui réapparaît, cf. `clearConsent()` dans `CefPlayerController.kt`). **Non testé sur
music.youtube.com dans ce dépôt** — incertitude réelle : il est possible que `loadVideoById` ne
déclenche pas la mise à jour de l'UI YT Music (barre de lecture, onglet paroles, queue) puisque ces
éléments sont pilotés par le routeur SPA de YT Music (Polymer/Lit + InnerTube `next`), pas par le
seul player natif. Autrement dit, le son pourrait changer sans que le reste de la page suive.

**Confiance** : faible à moyenne. Le mécanisme actuel (navigation complète via `loadURL`) est plus
lent mais **prouvé fiable** (c'est ce qui tourne en prod aujourd'hui). `loadVideoById` est une
piste d'optimisation, pas un remplacement à faire sans test A/B.

**Pour une playlist** : `music.youtube.com/watch?v=ID&list=PLxxxx` en navigation complète est la
voie la plus sûre et cohérente avec l'approche actuelle (mêmes garanties que le point unique
`watch?v=`) — pas de nouvelle API à apprendre.

---

## Recommandations d'implémentation (stratégie JS par fonctionnalité, à câbler dans `CefPlayerController`)

1. **Next/Prev** : ajouter `next()`/`previous()` qui appellent
   `document.getElementById('movie_player').nextVideo()/previousVideo()` en fire-and-forget comme
   `play()`/`pause()` aujourd'hui. **Vérifier empiriquement en premier** (log console via
   `-Driplay.cefvisible` + DevTools, ou juste observer si `location.href` avance dans le
   `pollLoop`) avant de s'appuyer dessus — si ça ne fait rien d'observable, fallback en pilotant
   la queue applicative RiPlay elle-même (RiPlay a déjà l'algorithme de "prochain morceau" côté DB
   / bibliothèque, il suffit de rappeler `load(nextVideoId)` comme le fait déjà
   `ThreeColumnsApp.kt` pour le morceau initial). C'est probablement la voie la plus sûre à terme :
   RiPlay pilote sa propre queue et appelle `load()` à chaque changement, exactement comme
   aujourd'hui pour un seul morceau — pas besoin de dépendre du comportement interne (encore flou)
   de la queue YT Music.
2. **Queue (lecture)** : ne pas dépendre de `getPlaylist()`/queue YT Music. RiPlay n'a pas encore
   de queue app-level côté desktop (`ThreeColumnsApp.kt` ne gère qu'un `videoId` courant) — la
   feature "file d'attente" est donc d'abord un manque côté RiPlay lui-même, pas un manque côté
   pilotage JS. Prévoir une queue Kotlin (liste de `videoId`/`Song`) au niveau app, `next()`
   consomme cette liste et appelle `player.load(id)` — ce que fait déjà l'app aujourd'hui pour un
   seul morceau, juste étendu à une liste.
3. **Shuffle/Repeat** : commencer par le fallback DOM (cliquer les vrais boutons de
   `ytmusic-player-bar`) car la sémantique interne YT Music (3 états de repeat, shuffle qui
   régénère la queue) est probablement découplée de `setShuffle`/`setLoop` du player IFrame.
   Sélecteurs précis à récupérer par inspection live (DevTools sur le CEF visible via
   `-Driplay.cefvisible`), pas par recherche web.
4. **Métadonnées** : ne rien changer — continuer d'utiliser la DB locale +
   `extensions/environment` (déjà fait dans `ThreeColumnsApp.kt`) pour titre/artiste/pochette, et
   `video.duration`/`currentTime` (déjà dans `tickScript`) pour le temps. Zéro nouveau code JS
   nécessaire ici.
5. **Paroles** : ne rien changer — réutiliser `Environment.lyrics()` (texte) et
   `LrcLib.lyrics()`/`KuGou` (synchronisées), tous deux déjà dans le dépôt et indépendants du CEF.
   Aucun DOM scraping à écrire.
6. **Chargement par ID** : garder le mécanisme actuel (`loadURL` + poll jusqu'à confirmation dans
   `href`) — il est prouvé fiable en prod. Tester `loadVideoById` seulement comme optimisation de
   latence, derrière un flag, en comparant que la queue/l'UI YT Music restent cohérentes avant de
   remplacer le mécanisme actuel.

**Incertitude à lever en priorité (avant tout code)** : lancer le CEF visible
(`-Driplay.cefvisible`), ouvrir les DevTools JCEF, et taper à la main dans la console
`document.getElementById('movie_player').nextVideo` /`.getPlaylist()` /`.setShuffle` /
`.loadVideoById` pour confirmer leur présence réelle et leur effet observable sur
music.youtube.com — c'est la seule vérification qui manque à ce document (recherche web
indisponible cette session).
