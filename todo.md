### Common

* Avoid proxies and OnlyIn

#### Release tasks:

* \[MED\] Fix frame lighting calculation;
* \[MED\] Implement script builder for models;
* \[MED\] There's still some desync happening time to time - could be just pixel not written to the buffer;
* \[MED\] If painting has some problems, just remove it instead of crashing;
* \[MED\] Use specific light levels for every partial canvas;
* \[MED\] Use ObjectHolders;
* \[MED\] Remove infinity bounds in getRenderBoundingBox; *
* \[LOW\] Fix plank texture on an easel; *
* \[LOW\] Implement bucket tool;
* \[LOW\] Close or update screen when canvas removed from easel; *
  
#### Would-be-nice-to-do:

* \[HIGH\] I don't like how different classes of canvas data created, would be nice to invent something better;
* \[HIGH\] Check that nothing breaks if player tries to draw to not loaded canvas;
* \[LOW\] Looks like if color in a palette somehow getting wrong value, it's unfixable with new color due to alpha channel: maybe we can set alpha to 255 explicitly when picking a color in order to remove potential problem;
* \[LOW\] Remove unnecessary edges in composite frames;
* \[LOW\] Trying to unload non-existent canvases sometimes;
* \[LOW\] Remove network getters/setters: they're useless, and actually looks like a bad pattern;

#### Planned features:

* Pencil size with more palette "damage";
* Pencil cursor;
* Pencil color jitter;
* Pencil transparency;
* Show canvas in hands like map;
* Texture dispatcher which will prevent not only client request canvases too quick but server to sync canvases too frequent;
* Think about creating own atlas map with loaded paintings.