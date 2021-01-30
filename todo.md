### Common

* A special interface for economy and personal items (locks and money GUI overlay)
* Avoid proxies and OnlyIn

### Locks

#### Release:

* \[HIGH\] Remove craft of the lock without key;
* \[HIGH\] Fix client/server state issues with multiple ingots;
* \[MED\] Force close door on quick action;
* \[LOW\] Fix delay in blockstate (probably change open state manually);

#### Features:

* RFID cards maybe;

### Paintings

#### RC:

* \[HIGH\] Frame item & TE;

#### Release tasks:

* \[HIGH\] Check that nothing breaks if player tries to draw to not loaded canvas;
* \[MED\] Make sure never use canvas with id 0;
* \[MED\] Implement canvas sealing;
* \[MED\] Textures on client renderer kept between worlds;
* \[LOW\] Fix plank texture on easel;
* \[LOW\] Fix leg top transparent on easel;
* \[LOW\] Implement bucket tool;
* \[LOW\] Close or update screen when canvas removed;

#### Features:

* Pencil size with more palette "damage";
* Pencil color jitter;
* Pencil transparency;
* Show canvas in hands like map;
* Texture dispatcher which will prevent not only client request canvases too quick but server to sync canvases too frequent;

### Economy

* Basic money printing press;
* Money items;
* Trading/vending machines;