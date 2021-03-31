### Common

* A special interface for economy and personal items (locks and money GUI overlay)
* Avoid proxies and OnlyIn

#### Beta:

* \[HIGH\] Frame item & TE;
* \[MED\] Implement canvas sealing;
* \[MED\] Textures on client renderer kept between worlds;
* \[LOW\] Remove infinity bounds in getRenderBoundingBox

#### Release tasks:

* \[HIGH\] Check that nothing breaks if player tries to draw to not loaded canvas;
* \[MED\] Make sure never use canvas with id 0;
* \[LOW\] Fix plank texture on easel;
* \[LOW\] Fix leg top transparent on easel;
* \[LOW\] Implement bucket tool;
* \[LOW\] Close or update screen when canvas removed;
* \[LOW\] Remove network getters/setters: they're useless and it's actually a bad pattern;

#### Planned features:

* Pencil size with more palette "damage";
* Pencil color jitter;
* Pencil transparency;
* Show canvas in hands like map;
* Texture dispatcher which will prevent not only client request canvases too quick but server to sync canvases too frequent;'
* Maybe create own atlas map with loaded paintings.