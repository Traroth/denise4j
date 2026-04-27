# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`denise4j` est une bibliotheque graphique Java inspiree des effets demoscene des annees 80/90. Elle exploite les classes d'images Java (`BufferedImage`, etc.) pour produire des effets et animations graphiques.

**Domaine :** scrolling, zoom, rotation, deformation, changement de palette, compositing d'images.

- **groupId:** `fr.dufrenoy.imagefx`
- **artifactId:** `denise4j`
- **Java version:** 11
- **Build tool:** Maven

## Commands

```bash
# Compiler
mvn clean install

# Lancer les tests
mvn test
```

## Architecture

### Principes fondamentaux

- Palette 32 bits ARGB — pas de mode palette indexee
- Zoom bilinear par defaut
- Pas de couplage avec Swing, JavaFX ou SWT — Denise4J est une bibliotheque purement graphique
- La sortie est une `BufferedImage` (via `Stage`) — l'utilisateur l'affiche comme il veut
- Architecture 100% portable : fonctionne avec Swing, JavaFX, SWT, export fichier, etc.

### Classes principales

#### `Stage` — `fr.dufrenoy.imagefx.Stage`

Buffer de pixels. Represente la scene sur laquelle on peint.

- Encapsule un `BufferedImage` de type `TYPE_INT_ARGB`
- Expose le tableau `int[]` sous-jacent via `DataBufferInt` pour un acces direct aux pixels
- Expose `getImage()` pour que l'utilisateur affiche le resultat dans le toolkit de son choix
- Expose ses dimensions (`getWidth()`, `getHeight()`)
- Porte la `backgroundColor` (defaut `0xFF000000`) — le `EffectPipeline.render(stage)` clear le buffer avec cette couleur en debut de frame
- Aucune dependance sur Canvas, JFrame, Swing ou JavaFX

#### `PixelSource` — `fr.dufrenoy.imagefx.PixelSource`

Interface representant une source de pixels lisible par le pipeline et les effets.

```java
public interface PixelSource {
    int getPixel(int x, int y);
    int getWidth();
    int getHeight();
}
```

#### `ImageSource` — `fr.dufrenoy.imagefx.ImageSource`

Wrapper d'une `BufferedImage` en `PixelSource`.

#### `TileSet` — `fr.dufrenoy.imagefx.TileSet`

Planche de tuiles (spritesheet) decoupee en tuiles de taille fixe.

- Construit a partir d'une `BufferedImage` + largeur et hauteur de tuile
- Indexation : ligne par ligne, de haut-gauche a bas-droite (sens de lecture)

#### `TileMap` — `fr.dufrenoy.imagefx.TileMap`

Grille de tuiles referencant un `TileSet`. Implemente `PixelSource`.

- Alimentation par zone rectangulaire : `setTiles(col, row, int[][])`
- Politique de bord configurable : `WRAP`, `CLIP`, `FEED`
- Le developpeur pilote le chargement progressif explicitement

#### `ParamInt` / `ParamDouble` — `fr.dufrenoy.imagefx`

Parametres mutables pour le pipeline. Le pipeline est construit une fois et reutilise a chaque frame, seuls les parametres changent.

- `ParamInt` — valeurs discretes (offsets en pixels, indices)
- `ParamDouble` — valeurs continues (facteur de zoom, angle de rotation)

#### `EffectPipeline` — `fr.dufrenoy.imagefx.EffectPipeline`

Chaine d'effets graphiques. API fluide, stateless (structure fixe, parametres variables via Param).

- `addSource(PixelSource)` ouvre un nouveau layer
- Effets chainables par layer : `.scrollH()`, `.scrollV()`, `.zoom()`, `.zoom(factor, cx, cy)`, `.rotate()`, `.rotate(angle, cx, cy)`
- `.transparentColor(int)` definit le color key pour le compositing du layer
- `.fuseLayer()` fusionne les layers ; les effets suivants s'appliquent au resultat fusionne jusqu'au prochain `addSource`
- `.build()` finalise la construction
- `.render(stage)` : remplit le Stage avec `backgroundColor`, puis applique le modele "always overwrite" layer par layer (si la source couvre la position et que le pixel ne correspond pas a la `transparentColor`, il ecrase ; sinon la valeur precedente reste)

### Exemple d'utilisation typique

```java
// Sources
TileSet tileSet = new TileSet(spriteSheet, 16, 16);
TileMap background = new TileMap(tileSet, 40, 30, TileMap.EdgePolicy.WRAP);
background.setTiles(0, 0, levelData);

ImageSource sprites = new ImageSource(spritesImage);

// Parametres
ParamInt bgScroll = new ParamInt(0);
ParamInt fgScroll = new ParamInt(0);
ParamDouble fgZoom = new ParamDouble(1.0);

// Pipeline (construit une fois)
EffectPipeline pipeline = new EffectPipeline()
    .addSource(background)
        .scrollH(bgScroll)
    .addSource(sprites)
        .transparentColor(0xFF00FF)
        .scrollH(fgScroll)
        .zoom(fgZoom)
    .build();

// Rendu d'une frame (Stage cree avec backgroundColor par defaut 0xFF000000)
Stage stage = new Stage(800, 600);
bgScroll.add(1);
fgScroll.add(4);
pipeline.render(stage);

// Affichage (au choix de l'utilisateur)
Graphics g = canvas.getGraphics();
g.drawImage(stage.getImage(), 0, 0, null);
```

### Effets implémentes

- `scrollH(ParamInt offset)` — scroll horizontal (srcX = stageX + offset)
- `scrollV(ParamInt offset)` — scroll vertical (srcY = stageY + offset)
- `zoom(ParamDouble factor)` — zoom bilineaire, centre = milieu du stage
- `zoom(ParamDouble factor, ParamDouble cx, ParamDouble cy)` — centre explicite
- `rotate(ParamDouble angle)` — rotation horaire bilineaire, centre = milieu du stage
- `rotate(ParamDouble angle, ParamDouble cx, ParamDouble cy)` — centre explicite

Les transforms se chaininent dans l'ordre de declaration. Le cos/sin de rotate est precalcule une fois par render (methode `prepare()`). Le zoom invalide au render (facteur <= 0) leve `IllegalArgumentException`.

### Ce qui n'est pas encore implemente (backlog)

- Deformation (wave, distortion)
- Changement de palette (cycling, fading, remapping)
- Compositing avance (blending, masking, overlay)
- Le niveau animation (Orchestrator : boucle de frames, double-buffering, timing)
- Rendu multithread via parallel streams
- Acceleration SIMD via Vector API (Java 21+, optionnel)