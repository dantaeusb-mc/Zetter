### Common

* Avoid proxies and OnlyIn

#### Alpha:

* \[LOW\] Remove infinity bounds in getRenderBoundingBox.

#### Beta: 

* \[HIGH\] Disallow to put canvases without frames on the wall;
* \[MED\] Rename canvas name to canvas code;

#### Release tasks:

* \[HIGH\] Check that nothing breaks if player tries to draw to not loaded canvas;
* \[MED\] There's still some desync happening time to time - could be just pixel not written to the buffer;
* \[MED\] Implement canvas naming & sealing _in the picture sewing table_;
* \[MED\] Make sure never use canvas with id 0;
* \[MED\] Add back texture for the frame;
* \[MED\] If painting has some problems, just remove it instead of crashing;
* \[MED\] Use ObjectHolders;
* \[MED\] 1x3 and 3x1 frames;
* \[LOW\] Fix plank texture on an easel;
* \[LOW\] Remove unnecessary edges in composite frames;
* \[LOW\] Trying to unload non-existent canvases sometimes;
* \[LOW\] Fix leg top transparent on an easel;
* \[LOW\] Looks like if color in palette somehow got wrong value it's broken;
* \[LOW\] Implement bucket tool;
* \[LOW\] Close or update screen when canvas removed;
* \[LOW\] Remove network getters/setters: they're useless, and it's actually a bad pattern;

#### Planned features:

* Pencil size with more palette "damage";
* Pencil cursor;
* Pencil color jitter;
* Pencil transparency;
* Show canvas in hands like map;
* Texture dispatcher which will prevent not only client request canvases too quick but server to sync canvases too frequent;
* Think about creating own atlas map with loaded paintings.