# RiPlay Desktop — Spec de design UI

Portage desktop (Compose Multiplatform / JVM) d'un lecteur de musique YouTube.
Direction artistique : **structure et ergonomie du Spotify desktop actuel**, mais
**identité verte RiPlay** conservée comme couleur d'accent (le vert du logo, pas le vert Spotify).

La maquette de référence à valider est `docs/desktop/mockup.html` (écran d'accueil « For You »).

---

## 1. Structure de layout

Layout 3 zones fixes + 1 barre pleine largeur, calqué sur Spotify :

```
┌───────────┬──────────────────────────────────────────────────────┐
│           │  TOPBAR  [< >]        [ Rechercher…        ]   [ 👤 ]  │ 64px
│  SIDEBAR  ├──────────────────────────────────────────────────────┤
│           │                                                        │
│  logo     │  CONTENU (scroll vertical)                            │
│  Accueil  │                                                        │
│  Recherche│   Bonsoir                                              │
│  Bibliothq│   ┌──────┐ ── rangée « scroll horizontal » ────────►  │
│  ───────  │   │ card │ [card][card][card][card][card][card]       │
│  Playlists│   └──────┘                                            │
│  · Liked  │   Nouveaux albums              Tout afficher          │
│  · Mix 1  │   [card][card][card][card][card][card][card] ──────►  │
│  · Mix 2  │   Artistes similaires          Tout afficher          │
│  · …      │   [rond][rond][rond][rond][rond][rond] ────────────►  │
│           │                                                        │
├───────────┴──────────────────────────────────────────────────────┤
│  NOW-PLAYING BAR   🎵 titre/artiste ♥ | ⏮ ⏯ ⏭  ──seek── | 🔊 ▭   │ 88px
└───────────────────────────────────────────────────────────────────┘
```

- **Sidebar** : largeur fixe **240px**, pleine hauteur (moins la now-playing bar). Contient : logo RiPlay en haut, nav primaire (Accueil / Recherche / Bibliothèque), séparateur, puis liste scrollable des playlists/bibliothèque.
- **Zone contenu** : `1fr`, contient une **TopBar** collante (64px) + une zone scrollable verticale avec les rangées de cartes.
- **Now-Playing Bar** : **pleine largeur**, hauteur **88px**, ancrée en bas (au-dessus de tout, sidebar comprise), comme Spotify.

Grille CSS de référence (transposée en Compose : `Column { Row(weight) { Sidebar; Column{ TopBar; ScrollableContent } }; NowPlayingBar }`) :

```
columns: 240px 1fr
rows:    1fr  88px
sidebar → (row1, col1) · main → (row1, col2) · nowplaying → (row2, col1/span2)
```

### Mapping vers les écrans Compose existants
| Zone / section        | Écran Compose actuel        |
|-----------------------|-----------------------------|
| Accueil « For You »   | `QuickPicsScreen`           |
| Rangées de cartes     | rows de `QuickPicsScreen`   |
| Écran album           | `AlbumScreen`               |
| Écran artiste         | `ArtistScreen`              |
| Écran playlist        | `PlaylistScreen`            |
| Bibliothèque (titres/albums) | `SongsPage` / `AlbumsPage` |
| Now-Playing Bar       | `MiniPlayer` (à re-styler)  |

Le `ThreeColumnsApp.kt` actuel (3 colonnes + dividers gris) est **remplacé** par ce layout.

---

## 2. Palette de couleurs

Fond sombre type Spotify + accent **vert RiPlay #58CC86** (extrait de `assets/design/latest/app_icon.svg`, `fill:#58cc86`).

### Fonds & surfaces
| Rôle                              | Hex        | Usage |
|-----------------------------------|------------|-------|
| Fond app (base)                   | `#0A0A0A`  | derrière tout, gouttières |
| Fond sidebar / now-playing        | `#121212`  | panneaux latéral et bas |
| Fond contenu                      | `#121212`  | zone centrale (léger dégradé toléré en haut, cf. perf) |
| Surface carte (repos)             | `#181818`  | cartes album/playlist |
| Surface carte (hover)             | `#282828`  | hover carte, ligne de titre active |
| Surface élevée / champ recherche  | `#2A2A2A`  | input, chips, menus |
| Bordure / séparateur              | `#2A2A2A`  | 1px, séparateurs discrets |

### Accent RiPlay (vert)
| Rôle                    | Hex        | Usage |
|-------------------------|------------|-------|
| Accent primaire         | `#58CC86`  | bouton play carte, progression, nav actif, sliders remplis |
| Accent hover / clair    | `#6FD699`  | hover du bouton play (+ scale 1.04) |
| Accent pressé / foncé   | `#46B873`  | état pressé |
| Accent voilé (10%)      | `rgba(88,204,134,.10)` | fond de l'item nav actif, halos discrets |

Le bouton play principal (now-playing) reste **blanc** (`#FFFFFF`) comme Spotify — le vert sert d'accent d'état (progression, actif), pas de tous les contrôles, pour ne pas saturer.

### Texte
| Rôle                | Hex        | Usage |
|---------------------|------------|-------|
| Texte primaire      | `#FFFFFF`  | titres, valeurs |
| Texte secondaire    | `#B3B3B3`  | sous-titres, artistes, labels nav |
| Texte tertiaire     | `#6A6A6A`  | timers, méta discrète, placeholders |
| Sur accent          | `#0A0A0A`  | icône play sur pastille verte |

### États
- **Hover carte** : fond `#181818 → #282828`, bouton play vert révélé (fade + translateY 8px→0).
- **Nav actif** : texte `#FFFFFF`, fond `rgba(88,204,134,.10)`, barre/point accent vert `#58CC86`.
- **Ligne de titre en lecture** : titre en vert `#58CC86`, petit équaliseur/▶ vert.
- **Focus clavier** : contour `2px #58CC86` (accessibilité — ne pas supprimer les outlines).

---

## 3. Typographie

Police système (aucune ressource externe — perf + offline) :
`-apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif`.
En Compose : `FontFamily.Default` (SansSerif). Éviter le chargement de fonts custom au démarrage.

| Style              | Taille | Poids | Couleur       | Usage |
|--------------------|--------|-------|---------------|-------|
| Titre de page      | 32px   | 700   | `#FFFFFF`     | « Bonsoir », nom d'album/artiste (petit format) |
| Titre de section   | 22–24px| 700   | `#FFFFFF`     | « Nouveaux albums », « Artistes similaires » |
| Titre de carte     | 14–16px| 600   | `#FFFFFF`     | nom d'album/playlist |
| Sous-titre carte   | 13px   | 400   | `#B3B3B3`     | artiste / « Playlist » / année |
| Ligne de liste     | 14px   | 400   | `#FFFFFF`     | titre dans une TrackRow |
| Label nav / méta   | 13–14px| 500   | `#B3B3B3`     | items sidebar |
| Timer / durée      | 12px   | 400   | `#6A6A6A`     | seekbar, colonne durée |
| « Tout afficher »  | 12px   | 700   | `#B3B3B3`     | uppercase, letter-spacing .5px, hover `#FFFFFF` |

Interligne : 1.3–1.4. Titres de carte/sous-titres **tronqués à 1 ligne** (`ellipsis` / en Compose `maxLines=1, TextOverflow.Ellipsis`).

---

## 4. Espacement — grille 4 / 8px

Tout est multiple de 4px, rythme principal en 8px.

| Token | Valeur | Usage |
|-------|--------|-------|
| xs    | 4px    | icône↔label, padding interne fin |
| sm    | 8px    | gap intra-carte (art↔titre) |
| md    | 16px   | padding cartes, gap entre cartes d'une rangée |
| lg    | 24px   | padding zones (contenu, sidebar), gap entre rangées |
| xl    | 32px   | marge haute de page, entre grandes sections |

- **Padding zone contenu** : 24px (droite/gauche), 24px haut.
- **Gap entre cartes** d'une rangée : 16px.
- **Gap vertical entre rangées** : 24–32px.
- **Rayons (corners)** : cartes **8px**, pochettes **6px**, boutons pill **500px**, champ recherche **500px**, pastille play **50%**. Artiste = pochette **ronde** (50%).

---

## 5. Inventaire des composants

### 5.1 Sidebar (240px)
- Bloc haut : logo RiPlay (SVG vert) + wordmark, cliquable → accueil.
- Nav primaire : `Accueil`, `Rechercher`, `Bibliothèque` — icône 20px + label. Item actif = fond vert voilé + texte blanc + accent gauche.
- Séparateur 1px `#2A2A2A`.
- Liste bibliothèque/playlists : chaque item = mini-pochette 40px (rayon 6px) + nom (blanc 14px) + sous-ligne (`Playlist · Auteur`, `#B3B3B3` 12px). Scroll vertical propre à ce bloc.
- *Compose* : `Column` fixe + `LazyColumn` pour la liste. Pas de blur, fond plat.

### 5.2 TopBar (64px, collante)
- Gauche : boutons rond `‹` `›` (nav historique), fond `#0A0A0A`, icône `#B3B3B3`.
- Centre/gauche : champ **Rechercher** — pill `#2A2A2A`, icône loupe, placeholder `#6A6A6A`, largeur ~360px. Focus → contour vert.
- Droite : avatar rond 32px (`#2A2A2A`).
- *Perf* : la TopBar peut devenir opaque au scroll — simple changement d'alpha de fond, **pas** de backdrop-blur.

### 5.3 Card album / playlist
- Conteneur : padding 16px, fond `#181818`, rayon 8px, hover `#282828`.
- Pochette : carré `aspect-ratio 1`, rayon 6px, placeholder = **dégradé CSS 2 couleurs + emoji/note SVG** (en prod : image chargée async avec placeholder de même couleur pour éviter les sauts de layout).
- Titre 14–16px/600 (1 ligne), sous-titre 13px `#B3B3B3` (1 ligne).
- **Bouton play** : pastille verte `#58CC86` 48px, ombre douce, ancrée bas-droite de la pochette ; masquée au repos, révélée au hover (fade + translateY). Icône ▶ `#0A0A0A`.
- *Perf* : une seule ombre, pas de dégradé multi-stop, transition sur `opacity`/`transform` uniquement.

### 5.4 Row scrollable (rangée horizontale)
- En-tête : titre de section (22–24px/700) + lien « Tout afficher » à droite.
- Corps : rangée de cartes en **scroll horizontal isolé** (le body ne scrolle jamais horizontalement).
- *Compose* : `LazyRow` (recyclage = clé perf). Scrollbar masquée, molette/drag horizontal.

### 5.5 TrackRow (liste de titres — album/playlist/biblio)
- Grille : `#/▶` · titre+artiste · album · durée. Hover → fond `#282828` + n° remplacé par ▶.
- Titre en lecture : texte vert `#58CC86` + mini-équaliseur.
- *Compose* : `LazyColumn`, une seule `Row` par item, pas de fond par défaut (peint au hover).

### 5.6 NowPlayingBar (88px, pleine largeur)
Trois zones (`gauche 1fr | centre auto | droite 1fr`) :
- **Gauche** : pochette 56px (rayon 6px) + titre (blanc 14px) / artiste (`#B3B3B3` 12px) + ♥ (actif = vert).
- **Centre** : contrôles `🔀 ⏮ ⏯ ⏭ 🔁` + seekbar. Play = pastille **blanche** 40px (icône noire). Shuffle/repeat actifs = vert + point sous l'icône. Seekbar : `temps — piste — temps`, remplissage vert `#58CC86`, poignée blanche visible au hover.
- **Droite** : file d'attente, périphérique, **volume** (slider remplissage vert), plein écran.

### 5.7 Boutons de contrôle
- Icônes 16–24px, couleur repos `#B3B3B3`, hover `#FFFFFF` (+ scale 1.06).
- Play principal : cercle blanc, hover scale 1.06 (pas de changement de couleur).
- Toggles (shuffle/repeat/like) : off `#B3B3B3`, on `#58CC86`.

### 5.8 Seekbar / Slider volume
- Rail : `#4D4D4D` (2–4px, rayon plein). Remplissage : `#58CC86`. Poignée : cercle blanc 12px, **caché au repos, visible au hover** du groupe.
- *Compose* : `Slider` custom léger (track + thumb dessinés), pas d'animation permanente.

---

## 6. Règles d'interaction

- **Hover** partout via changement de fond/opacité (pas de recomposition lourde). Cartes : élévation par couleur, pas par ombre projetée animée.
- **Sélection / actif** : nav et playlist actives = fond vert voilé + accent. Un seul élément actif à la fois par groupe.
- **États de lecture** : l'élément en cours (TrackRow, mini-player) passe le titre en vert `#58CC86` ; l'icône play devient pause.
- **Focus clavier** : contour vert 2px conservé (a11y). Cibles cliquables ≥ 32px.
- **Transitions** : 120–160ms `ease-out`, uniquement sur `opacity`/`transform`/couleur. Aucune animation en boucle (pas d'équaliseur animé permanent hors piste réellement en lecture, et même là : version légère).

---

## 7. Pièges perf Compose à éviter

1. **Pas de blur** (`Modifier.blur`, backdrop-filter) — coûteux au repaint. Profondeur = couleurs de surface, pas flou.
2. **Dégradés simples** seulement (2 stops max, statiques). Pas de dégradés multi-couches animés en fond.
3. **Listes = `LazyRow`/`LazyColumn`** systématiquement (recyclage). Jamais de `Row { forEach }` scrollable pour du contenu long.
4. **Ombres** : une seule `shadow`/`elevation` par composant, réservée au bouton play. Le reste = plat.
5. **Images** : chargement async + placeholder de couleur (évite les reflows) ; ne pas décoder en pleine résolution pour des vignettes.
6. **Transitions** limitées à `alpha`/`scale`/`offset` (pas de relayout animé, pas de `animateContentSize` sur les grilles).
7. **Aucune animation permanente** à l'écran au repos (batterie/CPU sur portable).
8. **Recomposition ciblée** : hover/état via `Modifier` dérivé, pas de `State` global qui redessine toute la grille.

---

## 8. Récap palette (hex)

```
Base app         #0A0A0A
Sidebar / bar    #121212
Carte repos      #181818
Carte hover      #282828
Surface / input  #2A2A2A
Accent RiPlay    #58CC86   (hover #6FD699 · pressé #46B873 · voilé rgba(88,204,134,.10))
Texte primaire   #FFFFFF
Texte secondaire #B3B3B3
Texte tertiaire  #6A6A6A
Rail slider      #4D4D4D
```
