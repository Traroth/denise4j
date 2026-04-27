# denise4j

Bibliothèque Java d'effets graphiques inspirée de la demoscene des années 80/90.
Produit des images animées à partir de sources de pixels, de tuiles et d'effets chaînables —
sans aucun couplage avec un toolkit graphique.

---

## Effets disponibles

| Effet | Méthode | Description |
|---|---|---|
| Scroll horizontal | `scrollH(ParamInt offset)` | Décale la source horizontalement |
| Scroll vertical | `scrollV(ParamInt offset)` | Décale la source verticalement |
| Zoom bilinéaire | `zoom(ParamDouble factor)` | Zoom/dézoom centré sur le Stage |
| Zoom centré | `zoom(ParamDouble factor, ParamDouble cx, ParamDouble cy)` | Centre explicite |
| Rotation | `rotate(ParamDouble angle)` | Rotation horaire, centre Stage |
| Rotation centrée | `rotate(ParamDouble angle, ParamDouble cx, ParamDouble cy)` | Centre explicite |

---

## Utilisation rapide

```java
// 1. Sources de pixels
TileSet tileSet = new TileSet(spriteSheet, 16, 16);
TileMap background = new TileMap(tileSet, 40, 30, TileMap.EdgePolicy.WRAP);
background.setTiles(0, 0, levelData);

ImageSource overlay = new ImageSource(overlayImage);

// 2. Paramètres mutables (modifiés à chaque frame)
ParamInt bgScroll  = new ParamInt(0);
ParamInt fgScroll  = new ParamInt(0);
ParamDouble fgZoom = new ParamDouble(1.0);

// 3. Pipeline (construit une seule fois)
EffectPipeline pipeline = new EffectPipeline()
    .addSource(background)
        .scrollH(bgScroll)
    .addSource(overlay)
        .transparentColor(0xFF00FF00)   // color key : vert transparent
        .scrollH(fgScroll)
        .zoom(fgZoom)
    .build();

// 4. Stage (buffer de sortie)
Stage stage = new Stage(640, 480);   // fond noir par défaut

// 5. Boucle d'animation
while (running) {
    bgScroll.add(1);
    fgScroll.add(3);
    pipeline.render(stage);

    // Affichage — au choix
    Graphics g = canvas.getGraphics();
    g.drawImage(stage.getImage(), 0, 0, null);
}
```

---

## Architecture

```
PixelSource          ← interface : toute source de pixels
├── ImageSource      ← wraps a BufferedImage
└── TileMap          ← grille de tuiles (WRAP / CLIP / FEED)
        └── TileSet  ← spritesheet découpée en tuiles

ParamInt             ← paramètre discret mutable (offset en pixels, ...)
ParamDouble          ← paramètre continu mutable (facteur de zoom, angle, ...)

EffectPipeline       ← chaîne d'effets — stateless, rendu dans un Stage
Stage                ← buffer ARGB 32 bits — sortie vers le toolkit au choix
```

### Principes

- **Portable** — aucune dépendance sur Swing, JavaFX ou SWT. La sortie est une `BufferedImage`,
  affichable dans n'importe quel toolkit ou exportable en fichier.
- **Stateless** — le pipeline est construit une fois, réutilisé à chaque frame.
  Seuls les `ParamInt`/`ParamDouble` changent entre les frames.
- **Direct array access** — les pixels sont lus et écrits via le tableau `int[]` sous-jacent
  (`DataBufferInt`), sans passer par `getRGB`/`setRGB`.
- **Bilinéaire par défaut** — zoom et rotation utilisent l'interpolation bilinéaire.
  Le chemin entier (scroll pur) court-circuite l'interpolation.

### Modèle de compositing

Pour chaque pixel `(x, y)` du Stage :

1. Valeur initiale : `stage.getBackgroundColor()` (opaque noir par défaut)
2. Pour chaque layer, dans l'ordre :
   - Les transforms calculent la coordonnée source `(sx, sy)`
   - Si la source est bornée (`CLIP`/`FEED`) et `(sx, sy)` est hors bornes → pas d'écriture
   - Si le pixel source correspond à la `transparentColor` du layer → pas d'écriture
   - Sinon → le pixel source écrase la valeur courante du Stage

### EdgePolicy

| Valeur | Comportement hors bornes |
|---|---|
| `WRAP` | Coordonnées ramenées modulo les dimensions (`Math.floorMod`) |
| `CLIP` | `IndexOutOfBoundsException` — le pipeline ne franchit jamais les bornes |
| `FEED` | Identique à `CLIP` à l'exécution — signale sémantiquement que les tuiles sont alimentées dynamiquement |

---

## Intégration Maven

```xml
<dependency>
  <groupId>fr.dufrenoy.imagefx</groupId>
  <artifactId>denise4j</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

---

## Build et tests

```bash
# Compiler et lancer les tests
mvn clean install

# Lancer les tests uniquement
mvn test

# Vérification formelle JML (nécessite OpenJML — Linux/macOS)
mvn verify -P openjml-unix

# Vérification formelle JML (Windows via WSL)
mvn verify -P openjml-windows
```

**Prérequis :** Java 11+, Maven 3.8+

---

## Roadmap

- [ ] Déformations (wave, distortion, tunnel, plasma)
- [ ] Changement de palette (cycling, fading, remapping)
- [ ] Compositing avancé (blending, masking, overlay)
- [ ] Orchestrator (boucle de frames, double-buffering, timing)

---

## Licence

GNU Lesser General Public License v3 — voir `LGPL-3.0`.