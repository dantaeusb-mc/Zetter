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

#### Release tasks:

* \[CRIT\] Implement client sync process & deferred sync requests;
* \[HIGH\] Frame item & TE;
* \[HIGH\] Check that nothing breaks if player tries to draw to not loaded canvas;
* \[HIGH\] Disallow to use canvas with id 0;
* \[MED\] If canvas closed before packetbuffer sent, packetbuffer got lots and changes will disappear;
* \[MED\] Implement canvas sealing;
* \[LOW\] Fix plank texture on easel;
* \[LOW\] Fix leg top transparent on easel;
* \[LOW\] Implement bucket tool;

#### Features:

* Pencil size with more palette "damage";
* Pencil color jitter;
* Pencil transparency;
* Show canvas in hands like map

### Economy

* Basic money printing press;
* Money items;
* Trading/vending machines;