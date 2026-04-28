# Architecture decisions — denise4j

This document records the key design decisions made for each class in this
project, along with the alternatives that were considered and the reasons
they were rejected. It is intended to provide context for future contributors
and for AI coding agents working on the project.

---

## Principes fondamentaux

- Palette 32 bits ARGB — pas de mode palette indexée
- Zoom bilinear par defaut
- Pas de couplage avec Swing, JavaFX ou SWT — Denise4J est une bibliotheque purement graphique
- La sortie est une `BufferedImage` (via `Stage`) — l'utilisateur l'affiche comme il veut
- Architecture portable : fonctionne avec Swing, JavaFX, SWT, export fichier, etc.

---

## Vue d'ensemble de l'architecture

```
fr.dufrenoy.imagefx.source  — sources de pixels (100% portable, aucune dependance GUI)
──────────────────────────────────────────────────────
PixelSource          (interface)
ImageSource          (wrapper BufferedImage -> PixelSource)
TileSet              (spritesheet decoupe en tuiles)
TileMap              (grille de tuiles + EdgePolicy)

fr.dufrenoy.imagefx.staging  — pipeline et buffers
──────────────────────────────────────────────────────
ParamInt / ParamDouble (parametres mutables pour le pipeline)
EffectPipeline       (chainage d'effets, stateless, render dans un Stage)
Stage                (BufferedImage wrapper + acces pixels)

fr.dufrenoy.imagefx.orchestration  — boucle d'animation
──────────────────────────────────────────────────────
FrameDropPolicy      (enum: EXCEPTION, WAIT, REPEAT_LAST, WARN_AND_REPEAT, SKIP)
FrameDropException   (RuntimeException pour FrameDropPolicy.EXCEPTION)
FrameCallback        (@FunctionalInterface: onFrame(frameIndex, deltaMs))
StagePool            (pool thread-safe de N stages, double/triple buffering)
Orchestrator         (boucle de frames, timing, pilotage du pipeline)
```

---

## `Stage` — `fr.dufrenoy.imagefx.Stage`

Buffer de pixels. Represente la scene sur laquelle on peint.

**Design choices:**
- Encapsule un `BufferedImage` de type `TYPE_INT_ARGB`
- Expose le tableau `int[]` sous-jacent via `DataBufferInt` pour un acces direct aux pixels
- Expose `getImage()` pour que l'utilisateur affiche le resultat dans le toolkit de son choix
- Expose ses dimensions (`getWidth()`, `getHeight()`)
- Porte la `backgroundColor` (defaut `0xFF000000`, opaque noir) — utilisee par `EffectPipeline.render(stage)` pour clear le buffer en debut de frame ; visible la ou aucun layer ne couvre apres compositing
- Aucune dependance sur Canvas, JFrame, Swing ou JavaFX

**Alternatives rejetees:**
- Encapsuler un `java.awt.Canvas` — imposerait AWT et empecherait JavaFX/SWT/headless
- Gerer le `BufferStrategy` — responsabilite deplacee au futur Orchestrator
- `backgroundColor` portee par `EffectPipeline` — couplait la couleur de fond a un pipeline particulier ; deplacee dans `Stage` car c'est une propriete du buffer, pas du pipeline

---

## `PixelSource` — `fr.dufrenoy.imagefx.PixelSource`

Interface representant une source de pixels lisible par le pipeline.

```java
public interface PixelSource {
    int getPixel(int x, int y);
    int getWidth();
    int getHeight();
}
```

**Design choices:**
- Interface minimale — permet a `ImageSource` et `TileMap` d'etre interchangeables
- Le pipeline et les effets ne travaillent que contre `PixelSource`

---

## `ImageSource` — `fr.dufrenoy.imagefx.ImageSource`

Wrapper d'une `BufferedImage` en `PixelSource`.

**Design choices:**
- Acces direct aux pixels via `DataBufferInt` (tableau `int[]` sous-jacent) — O(1) par pixel, evite le overhead de `BufferedImage.getRGB`
- Si l'image passee n'est pas `TYPE_INT_ARGB`, elle est convertie a la construction via `drawImage` dans une nouvelle image `TYPE_INT_ARGB`; les dimensions sont preservees
- Le tableau `int[]` est cache dans un champ final — le pipeline le lit directement sans passer par l'API `BufferedImage`
- Immutable apres construction (pour les dimensions et le buffer de pixels)

**Alternatives rejetees:**
- `BufferedImage.getRGB(x, y)` par pixel — wrapper AWT lourd, 5-10x plus lent que l'acces tableau direct sur des millions de pixels par frame

---

## `TileSet` — `fr.dufrenoy.imagefx.TileSet`

Planche de tuiles (spritesheet) decoupee en tuiles de taille fixe.

**Design choices:**
- Construit a partir d'une `BufferedImage` + largeur et hauteur de tuile
- Indexation des tuiles : ligne par ligne, de haut-gauche a bas-droite (sens de lecture)
- Index 0 = tuile en haut a gauche
- Indexation non parametrable — convention universelle des editeurs de tilemap

**Alternatives rejetees:**
- Indexation parametrable — complexite inutile, aucun cas d'usage reel

---

## `TileMap` — `fr.dufrenoy.imagefx.TileMap`

Grille de tuiles referencant un `TileSet`. Implemente `PixelSource`.

**Design choices:**
- Compose d'un `TileSet` + grille 2D d'indices de tuiles
- Alimentation par zone rectangulaire : `setTiles(col, row, int[][])`
- Politique de bord configurable via `EdgePolicy`
- Le developpeur pilote le chargement progressif explicitement (pas de callback ni lazy loading)
- Fonctionne en 2D des le depart (scroll horizontal, vertical et diagonal)

**EdgePolicy:**
- `WRAP` — la map boucle sur elle-meme ; `getPixel` accepte n'importe quelle coordonnee entiere (`isUnbounded() == true`). Le pipeline peut appeler `getPixel` avec des coordonnees hors bornes ; la TileMap les ramene modulo les dimensions
- `CLIP` — `getPixel` n'accepte que des coordonnees dans `[0, width) x [0, height)` (`isUnbounded() == false`). Le pipeline ne franchit pas les bornes ; les positions hors zone restent a la `backgroundColor` du Stage
- `FEED` — meme comportement runtime que CLIP. Indique semantiquement que le developpeur alimente les tuiles progressivement au fur et a mesure du scroll

**isUnbounded() sur PixelSource :**
Methode default de l'interface `PixelSource` (retourne `false`). `TileMap` WRAP la surcharge pour retourner `true`. Le pipeline consulte `isUnbounded()` pour savoir s'il peut appeler `getPixel` avec des coordonnees hors bornes ou s'il doit bounds-checker au prealable.

**Modele de compositing ("always overwrite") :**
Pour chaque pixel `(x, y)` du Stage :
1. La valeur initiale est `stage.getBackgroundColor()`
2. Pour chaque layer dans l'ordre :
   - Le pipeline calcule la coordonnee source `(sx, sy)` apres effets
   - Si la source est bornee (`!isUnbounded()`) et `(sx, sy)` hors bornes : le pixel n'est pas ecrit
   - Sinon : `source.getPixel(sx, sy)` est ecrit dans le Stage (overwrite)
   - Si le pixel source correspond a la `transparentColor` du layer : le pixel n'est pas ecrit

**Alternatives rejetees:**
- Chargement pilote par le scroll (le scroll notifie la TileMap) — couple scroll et TileMap, casse l'abstraction PixelSource
- Chargement lazy (la TileMap detecte les lectures hors zone) — latences imprevisibles au rendu
- Alimentation tuile par tuile — trop verbeux
- Alimentation par colonne/ligne — ne couvre pas le cas 2D
- `clipColor` configurable sur `TileMap` — supprimee : c'est le pipeline qui gere les pixels hors zone en ne les ecrivant pas (backgroundColor du Stage reste visible)
- Retourner `0x00000000` pour CLIP/FEED hors zone + interpretation de l'alpha — necessite un modele de compositing avec canal alpha, complexite inutile pour la demoscene

---

## `ParamInt` — `fr.dufrenoy.imagefx.ParamInt`

Parametre mutable de type `int`. Utilise pour les valeurs discretes (offsets en pixels, indices de tuiles).

---

## `ParamDouble` — `fr.dufrenoy.imagefx.ParamDouble`

Parametre mutable de type `double`. Utilise pour les valeurs continues (facteur de zoom, angle de rotation).

---

## `ParamInt` / `ParamDouble` — design commun

**Design choices:**
- Deux types distincts (`ParamInt`, `ParamDouble`) plutot qu'un generique — evite le boxing, API explicite et autodocumentee
- Mutables — le pipeline est construit une fois et reutilise a chaque frame, seuls les parametres changent
- Le pipeline est stateless : la structure (quels effets, quelles sources) est fixe, les `Param` portent l'etat variable

**Alternatives rejetees:**
- Un seul `Param` en `double` pour tout — perd la semantique int vs double
- Generique `Param<T>` — boxing pour les primitifs
- `ParamLong`, `ParamFloat` — aucun cas d'usage reel dans les effets graphiques

---

## `EffectPipeline` — `fr.dufrenoy.imagefx.EffectPipeline`

Chaine d'effets graphiques. Construit une fois, reutilise a chaque frame.

**API fluide:**
```java
EffectPipeline pipeline = new EffectPipeline()
    .addSource(map)
        .scrollH(bgScroll)
    .addSource(sprites)
        .transparentColor(0xFF00FF)
        .scrollH(fgScroll)
        .zoom(fgZoom)
    .build();

// Chaque frame
bgScroll.add(2);
pipeline.render(stage);
```

**Design choices:**
- Fluent API avec chainage
- `addSource(PixelSource)` ouvre un nouveau layer
- Les effets qui suivent s'appliquent au layer courant
- `.transparentColor(int)` definit le color key pour le compositing du layer sur les precedents
- `.fuseLayer()` fusionne les layers accumules ; les effets suivants s'appliquent au resultat fusionne, jusqu'au prochain `addSource` qui ouvre un nouveau layer
- `.render(stage)` : remplit le Stage avec `stage.getBackgroundColor()`, puis pour chaque pixel applique le modele "always overwrite" layer par layer
- Modele de compositing : "always overwrite" — si la source couvre la position et que le pixel ne correspond pas a la `transparentColor`, il ecrase ; sinon la valeur precedente reste (background ou layer precedent)
- La couleur de fond est portee par `Stage`, pas par le pipeline — c'est une propriete du buffer, pas d'un pipeline particulier
- Stateless : la structure est fixe, les parametres varient via `ParamInt`/`ParamDouble`
- Le pipeline ne reconstruit pas ses structures internes a chaque render

**Alternatives rejetees:**
- Pipeline reconstruit a chaque frame — non optimal
- Pipeline stateful (effets avancent automatiquement) — etat cache, difficulte de seek, conflit avec le futur niveau animation
- Parametres positionnels au render — fragile
- Parametres nommes par string — pas type-safe
- `backgroundColor` portee par le pipeline — couplait la couleur de fond a un pipeline particulier ; deplacee dans `Stage`

---

## Effets — architecture interne des transforms

Les effets (`scrollH`, `scrollV`, `zoom`, `rotate`) sont implémentes via une
interface interne `Transform` dans `EffectPipeline` :

```java
interface Transform {
    default void prepare(int stageW, int stageH) {}
    void apply(double[] coords); // coords[0]=srcX, coords[1]=srcY, modifies in place
}
```

**Design choices:**
- `prepare(stageW, stageH)` est appelee une fois par render, par layer, avant la boucle pixels — precompute cos/sin pour rotate, centre pour zoom, evite tout calcul par pixel inutile
- Les coordonnees sont des `double` tout au long de la chaine de transforms — les arrondis (bilineaire vs plus-proche-voisin) sont determines au moment de l'echantillonnage, pas dans les transforms
- `apply(double[] coords)` mutate le tableau en place — un seul appel virtuel par transform par pixel au lieu de deux (`srcX` + `srcY`). Pour `RotateTransform`, dx/dy sont captures avant toute modification du tableau, garantissant que les deux sorties utilisent les coordonnees d'entree originales
- Le pipeline compose les transforms dans l'ordre de declaration (ordre d'application = ordre de lecture dans le code)
- Apres `build()`, chaque layer stocke ses transforms dans un `Transform[]` (tableau fixe) — acces direct sans overhead `ArrayList` iterator

**Rendu parallele:**
La boucle sur les lignes y est parallelisee via `IntStream.range(0, h).parallel()` par layer. Chaque tache y ecrit exclusivement `pixels[y*w .. y*w+w-1]` — pas de contention sur le tableau de pixels. Les sources (`PixelSource`) sont en lecture seule pendant le render. Les champs de Transform (`cosA`, `sinA`, etc.) sont ecrits par `prepare()` avant le `forEach` et lus en lecture seule dans les taches paralleles — garantie happens-before du fork-join pool.

**Alternatives rejetees:**
- `double srcX(x,y)` + `double srcY(x,y)` en deux appels virtuels — `RotateTransform` recalculait `dx = x-cx`, `dy = y-cy` deux fois ; 2× plus de dispatch virtuel par transform par pixel
- `double[] apply(x,y)` retournant un nouveau tableau — allocation par pixel par transform, inacceptable en hot loop

---

## Effets — `scrollH` / `scrollV`

**Algorithm:** transform affine pure (pas de calcul virgule flottante).
- `scrollH(offset)` : `srcX = x + offset.get()`, `srcY = y`
- `scrollV(offset)` : `srcX = x`, `srcY = y + offset.get()`

Le resultat est toujours a coordonnees entieres — le chemin bilineaire est court-circuite (`tx == 0.0 && ty == 0.0`) pour un acces tableau direct.

**Semantique de l'offset:** offset positif deplace le contenu vers la gauche/haut (la source est lue en avance) ; offset negatif deplace le contenu vers la droite/bas.

---

## Effets — `zoom`

**Algorithm:** transform inverse centree. Pour chaque pixel stage `(x, y)` :
```
srcX = cx + (x - cx) / factor
srcY = cy + (y - cy) / factor
```
ou `(cx, cy)` est le centre du zoom (defaut : centre du Stage).

**Design choices:**
- `factor > 1` grossit le contenu (zoom avant) ; `0 < factor < 1` retrecit (zoom arriere)
- `factor <= 0` est interdit — lance `IllegalArgumentException` au moment du `prepare()` (render)
- Le centre est configurable via des `ParamDouble` (cx, cy) ou par defaut le centre du Stage
- Interpolation bilineaire par defaut — evite les artefacts de pixellisation

**Bilineaire — clampage de bord:**
Pour les sources bornees, quand `x0` est au bord (`x0 = srcW - 1`), le voisin `x1 = min(x0+1, srcW-1)` est ramene au bord. Le pixel de bord est repete plutot que de lever une exception. Pour les sources non bornees (WRAP), `x1 = x0 + 1` sans clampage.

**Alternatives rejetees:**
- Zoom miroir pour `factor < 0` — rejet a la demande du developpeur ; les effets miroir seront traites dans les deformations
- Interpolation au plus proche voisin — artefacts visibles a facteur > 1

---

## Effets — `rotate`

**Algorithm:** transform inverse de rotation horaire. Pour chaque pixel stage `(x, y)`,
avec `dx = x - cx`, `dy = y - cy` :
```
srcX = cx + dx * cos(θ) + dy * sin(θ)
srcY = cy - dx * sin(θ) + dy * cos(θ)
```
ou `θ` est l'angle en radians dans le sens horaire, `(cx, cy)` le centre de rotation.

**Design choices:**
- Rotation horaire (sens des aiguilles d'une montre) — coherent avec les conventions graphiques (y axe vers le bas)
- `cos(θ)` et `sin(θ)` sont precalcules dans `prepare()` — un seul appel trig par render, pas par pixel
- Le centre est configurable via des `ParamDouble` (cx, cy) ou par defaut le centre du Stage
- Interpolation bilineaire — meme logique que zoom, incluant clampage de bord pour sources bornees

**Precision numerique:**
Pour des angles comme π, `sin(π)` ≈ 1.2e-16 au lieu de 0. Les tests utilisant des sources WRAP absorbent ces ecarts sans les observer (floorMod gere les coordonnees legerement negatives).

---

## `FrameDropPolicy` — `fr.dufrenoy.imagefx.orchestration.FrameDropPolicy`

Enum definissant le comportement de `StagePool.getFrontBuffer()` quand aucune frame n'est prete.

| Valeur | Comportement |
|--------|-------------|
| `EXCEPTION` | Lance `FrameDropException` |
| `WAIT` | Bloque jusqu'a ce qu'une frame soit presentee |
| `REPEAT_LAST` | Retourne le stage affiche couramment, sans log |
| `WARN_AND_REPEAT` | Log sur `System.err`, puis retourne le stage courant |
| `SKIP` | Retourne `null` — l'appelant ignore ce tick |

**Design choices:**
- Enum plutot qu'interface — les strategies sont closes ; aucun use case pour une strategie personnalisee
- `WAIT` utilise `BlockingQueue.take()` — pas de spin-wait, pas de sleep
- `SKIP` est le seul cas ou `getFrontBuffer()` retourne `null` — documente au contrat JML

**Alternatives rejetees:**
- Interface `FrameDropStrategy` — extensibilite inutile, complexifie le contrat de `StagePool`

---

## `FrameDropException` — `fr.dufrenoy.imagefx.orchestration.FrameDropException`

`RuntimeException` lancee par `StagePool.getFrontBuffer()` quand la politique est `EXCEPTION` et qu'aucune frame n'est prete. Pas d'etat additionnel.

---

## `FrameCallback` — `fr.dufrenoy.imagefx.orchestration.FrameCallback`

`@FunctionalInterface` appele par l'`Orchestrator` une fois par frame, avant le render, depuis le thread de rendu.

```java
void onFrame(long frameIndex, long deltaMs);
```

**Design choices:**
- Appele depuis le thread de rendu — les mutations de `ParamInt`/`ParamDouble` n'ont pas besoin de synchronisation tant qu'elles restent dans ce callback
- `frameIndex` : compteur monotone croissant depuis 0
- `deltaMs` : duree en ms depuis la frame precedente ; vaut `0` pour la frame 0 (aucune frame precedente)
- `@FunctionalInterface` — permet les lambdas ; le cas nominal est une lambda courte

**Alternatives rejetees:**
- Callback appele depuis le thread display — necessite une synchronisation des Param
- Passer `deltaMs` en `double` — precision inutile pour des durees de frames en millisecondes

---

## `StagePool` — `fr.dufrenoy.imagefx.orchestration.StagePool`

Pool thread-safe de N stages pour le double ou triple buffering (N ≥ 2).

**Model producer-consumer borne :**

```
freeQueue  [N-1 slots]   ←── thread display (getFrontBuffer libere l'ancien displayStage)
                         ──→ thread rendu   (acquireBackBuffer prend depuis freeQueue)

readyQueue [N-1 slots]   ←── thread rendu   (present enqueue)
                         ──→ thread display (getFrontBuffer dequeue)

displayStage             :   stage couramment visible (1 slot implicite)
```

A tout moment : `freeQueue.size() + readyQueue.size() + 1 (displayStage) == N`.

**Design choices:**
- `acquireBackBuffer()` bloque via `BlockingQueue.take()` quand tous les stages sont occupes — le thread de rendu attend naturellement, sans spin-wait
- Avec N=2 : le thread de rendu a 0 frame d'avance (doit attendre chaque tick display)
- Avec N=3 : le thread de rendu peut avoir 1 frame d'avance (triple buffering classique)
- Les drops de frames ne surviennent qu'en display (`getFrontBuffer`) — jamais en production
- `FrameDropPolicy` est appliquee uniquement cote display
- `present()` utilise `BlockingQueue.add()` (non bloquant) — la file est dimensionnee pour ne jamais deborder si le protocol d'utilisation est respecte

**Alternatives rejetees:**
- Ring buffer circulaire avec ecrasement — le thread de rendu ecraserait des frames non encore affichees ; comportement impredictible pour une bibliotheque
- `Semaphore` direct — moins lisible, memes garanties
- `N=1` interdit — impossible d'avoir un displayStage et un backBuffer simultanement

---

## `Orchestrator` — `fr.dufrenoy.imagefx.orchestration.Orchestrator`

Boucle d'animation haut niveau. Pilote un `EffectPipeline` a un taux de frames cible.

**Fluent builder API :**
```java
Orchestrator orchestrator = new Orchestrator()
    .pipeline(pipeline)
    .stagePool(pool)
    .targetFps(60)
    .onFrame((frameIndex, deltaMs) -> { scrollOffset.add(2); })
    .build();

orchestrator.start();
// thread display :
Stage front = orchestrator.getFrontBuffer();
orchestrator.stop();
```

**Design choices:**
- Fluent builder avec `build()` qui verrouille la configuration — toute reconfiguration post-build lance `IllegalStateException`
- Thread de rendu daemon — ne bloque pas la JVM a l'arret
- `renderLoop()` : `acquireBackBuffer` → `callback.onFrame` → `pipeline.render` → `present` → sleep
- `deltaMs` vaut `0` pour la frame 0 via le sentinel `lastFrameStart = -1L` — aucun calcul arbitraire avant la premiere frame
- `stop()` interrompt le thread de rendu (`interrupt()`) puis attend sa terminaison (`join()`) — arret propre garanti
- `getFrontBuffer()` delegue a `StagePool.getFrontBuffer()` — la politique de drop est dans le pool

**Thread safety:**
- `running` est `volatile` — visible immediatement entre thread appelant et thread de rendu
- `targetFps` et les autres champs de configuration sont ecrits avant `Thread.start()` — la garantie *happens-before* de `Thread.start()` assure leur visibilite dans le thread de rendu
- `built` est lu uniquement depuis le thread appelant — aucune visibilite multi-thread requise

**Alternatives rejetees:**
- `ScheduledExecutorService` pour le timing — granularite insuffisante pour des cibles > 30 fps ; `Thread.sleep` avec correction de derive est plus precis
- Callback appele depuis le thread display — necessite une synchronisation des Param entre les deux threads