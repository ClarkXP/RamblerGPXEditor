CHANGELOG
===============
**27th July, 2017**

- limitation to POI display, in order to fix the OutOfMemory exception: if POI number in the current 
view exceeds 200, the same bitmap will be used for all of them. No names, no types coloring.

**21 July, 2017**

- Attempt to fix a silent bug (never seen, unreported, but present in the Developer Console crashes
report): `java.lang.NullPointerException: at pl.nwg.dev.rambler.gpx.CustomListNoIcons.getView(CustomListNoIcons.java:36)`
- Build 1.0.3

Please **do report** if the crash on opening the drawer (routes/tracks browsing) happens for you 
(when exactly?).

**20th July, 2017**

- Creator: optimization parameters for received series of coordinates changed to produce more detailed
routes: `GpxUtils.simplifyRoute(Data.osrmRoute, (int) distance / 100, 6d);` replaced with 
`GpxUtils.simplifyRoute(Data.osrmRoute, (int) distance / 50, 2d);`, which means up to 20 route points
 per kilometer at 2 m path precision.

**13th July, 2017**

- MapQuest routing added to the Route Creator. Supports modes: Car / Bicycle / Pedestrian. You need
to fill the `static String sEncodedKey = "YOUR_BASE64_ENCODED_KEY_HERE";` in the `Data` class
with your API key to build your own apk. Get it here: https://developer.mapquest.com. 
- Dialog to switch routing sources added to the Route Creator menu.
- Credits dialog updated, Wiki in About dialog linked.
- First public release (1.0.0) uploaded on the Play Store.


**12th July, 2017**

Preparation to use of alternative routing servers in the Route Creator:

- Own OSRM request and parsing replaced with @osmbonuspack `RoadManager` class (alternative routes no longer available).
- Fields to change the routing source (OSRM / MapQuest) added to the Route Creator/settings dialog.

**4th July, 2017**

- German translation corrected by @RigiRitter (Many thanks!).
- Sample data will only be loaded if the app is launched for the first time.

**3rd July, 2017**

- 'New file' button replaced with 'New data'. It actually did not create a file, but a new data set,
which is being synced to the shared folder.
- Track Manager: bug in setting buttons state fixed.

**1st July, 2017**

- The workaround to disallow overlapping dialog popups showing up on multiple marker clicks applied 
to `RouteEditorActivity` and `RouteCreatorActivity`.
- 'Clear waypoints' entry added to the Route Creator menu.
- Action bar made transparent in all activities that display maps.

**30th June, 2017**

- latest build uploaded on the [Play store](https://play.google.com/store/apps/details?id=pl.nwg.dev.rambler.gpx) (beta channel).

**29th June, 2017**

- Bug in the main activity fixed: on the 1st installation tracks from the sample GPX file used not 
to be parsed.
- The `assets/sample.gpx` file replaced with a new one.
- [Build 0.0.4.9](https://github.com/nwg-piotr/RamblerGPXEditor/blob/master/Application/RamblerGPXeditor-release-0.0.4.9.apk?raw=true) uploaded.

**28th June, 2017**

- New application logo, changes to the overall layout.
- Build 0.0.4.8 uploaded.

**27th June, 2017**

- Main screen rearranged: added the bottom bar with basic I/o buttons. Old entries in the drawer 
kept as a placeholder for future buttons, e.g. project site, wiki etc.
- Removed unused resources.
- Small bug fixes in `PoiActivity` and `RoutesBrowserActivity`.
- Build 0.0.4.7 uploaded.

**25th June, 2017**

- Track Manager: exporting tracks added.
- Route Manager: bugs fixed in exporting routes.
- Route Manager: switchable POI layer added.
- Credits popup updated.
- Main activity: common settings added.
- Common settings: measurement units selection added.
- Common settings: switch to enable/disable map rotation added.
- Build 0.0.4.5 uploaded.

**24th June, 2017**

- Track Manager: track properties menu entry and the dialog popup added.
- Conversion Track(s) into Route(s) added.
- Build 0.0.4.2 uploaded.

**23rd June, 2017**

- The Track Manager partially works. First I/O added: import, delete selected, delete all tracks.
- Build 0.0.4.1 uploaded.

**22nd June, 2017**

- A skeleton for the Track Manager activity added: just a copy of the Route Manager with several
adaptations. Not even half-baked yet. 

**21th June, 2017**

- Route Manager: the menu to select routes by name (former magnifier icon) moved to the application 
drawer.
- Route Manager and POI Manager: a separate Filter button added, instead of the long press action on 
the Zoom-to-fit button.
- Various layout improvements (new app icon, About dialog reformatted etc.). 
- Build 0.0.4.0 uploaded.


**20th June, 2017**

- Current data files root changed: originally files containing current state of `Data.sPoisGpx`, 
`Data.sRoutesGpx` and `Data.sTracksGpx` used to be stored in the apps private folder. Since I'd like 
them to be accessible to other apps,  I changed their location to `sdRoot/RamblerSharedData`.
- Main screen rearranged: added the Track Manager button (inactive at the moment). File operation
buttons from now on available in the drawer only.
- Build 0.0.3.8 uploaded.

**19th June, 2007**

- POI layer added to the Route Editor and Route Creator. In the Editor you can insert or append POI
coordinates as a route point. In the Creator you can append POI as a cardinal way point.

**18th June, 2017**

- Animation turned off in all zoom-to-fit buttons.
- Missing I/O code added to the POI Manager menu (New from coordinates / Clear all / Import / Export).
Possibly buggy at the moment.
- Also some modifications to Routes Browser and Main Activity.
- Build 0.0.3.4 uploaded.


**17th June, 2017**

- Basic POI Manager added. Missing menu: New from..., Clear all, Import, Export.
- MyLocationNewOverlay added (show my position on the map).
- RotationGestureOverlay added (enables rotation gestures).
- POI Manager / refreshMap() only draws markers which are going to be visible: 
`if (mMapViewBoundingBox.contains(markerPosition))`.
- As well above as the RouteEditor needed a substitute for the onMapDragEnd method, which I did not
manage to find in OSM-related libs. To achieve this, I set the `mMapDragged` boolean true, and then
 check the value in `onTouchEvent(MotionEvent motionEvent)`.

**15th June, 2017**

- Exporting routes code added, menu entry activated. The last inactive Router Manager menu entry 
('Visibility') is POI-related, and will be added later.
- 'About' dialog updated; 'Credits' coming soon, temporarily linked to the website.
- Build 0.0.3.2 uploaded.

**14th June, 2017**

- Route Optimizer added.
- Build 0.0.3.1 uploaded.

**13th June, 2017**

- Route Creator sets activity result `NEW_ROUTE_ADDED` to force the Routes Browser open the just created route properties dialog.
- 'Clear all routes' activated in the Browser menu.
- Build 0.0.3.0 uploaded.

**12th June, 2017**

- A null pointer exception bug fixed in the Routes Browser. 
- Routes Browser: importing routes and tracks from external gpx files added.
- Build 0.0.2.9 uploaded (not really tested, seems to work pretty well at first sight).

**10th June, 2017**

- 'Draw manually' entry added to the Routes Browser menu. Creates a new, empty route and launches the Route Editor.
- Build 0.0.2.8 uploaded.

**9th June, 2017**

- Route Point properties dialog added (touch the point to open the dialog popup).
- Last route point marker added (checkered flag - touch to select the route).
- Pencil button in the Routes Browser has now two actions attached: touch to edit route points, long press to edit route properties.
- Build 0.0.2.7 uploaded.

**8th June, 2017**

- In the Routes Browser selecting 'Edit' launches the route properties dialog first. You'll find the 'Edit points' button inside.
Also the on-screen 'Edit' button added (pencil).
- Route Editor: the number of route points (markers) to draw at a time limited to 20 nearest 
to the center of the map. `Utils.getNearestRoutePoints(IGeoPoint mapCenter, Route route)` used instead 
of original `GpxUtils.getPointNamesSortedByDistance)`.
- Google Play Services version updated (11.0.0).
- Fresh build uploaded (0.0.2.5).

**7th June, 2017**

- **RouteEditorActivity** skeleton code added, some basic features already work. 

**6th June, 2017**

- Route labels in the browser touchable: click to select the route.

**30th May, 2017**

- User-Agent and copyright info updated according to [Important notes on using osmdroid in your app](https://github.com/osmdroid/osmdroid/wiki/Important-notes-on-using-osmdroid-in-your-app) Wiki.
- Routes Browser: the Search button (magnifier) launches the dialog to select a route by name. The dialog layout needs to be improved.
Also sorting order must be user-configurable (long press the button to change?).
- Build 0.0.2.4 uploaded.

**29th May, 2017**

- Route deletion activated in the Routes Browser menu. Removes the selected route from Data.mRoutesGpx, and gets back to the refreshed browser display.
- Build 0.0.2.2 uploaded.

**28th May, 2017**

- Route labels added to the Routes Browser (in the half way route point). To be decided later if to make the labels clickable.
- Build 0.0.2.1 uploaded.


**27th May, 2017**

- Refactoring: former Route Picker from now on is the Routes Browser.
- Bugs in the Routes Browser fixed.
- (simple) Support for the routing server error added.
- New build: 0.0.2.0.apk uploaded.

**26th May, 2017**

Route picker:
- **view filtering added.** Long press the Zoom-to-fit button to enter values. *Known bug in the distance filter and the route length filter: 
entering values which produce no results hangs the app. To be fixed tomorrow.*

**25th May, 2017**

Route picker: 
- **buttons to select a route activated (Prev / Next).** Touch the map anywhere to clear selection. *Given up on selecting routes by touching polylines: it's damned difficult to use.*
- The route prompt line displays annotated name of currently selected route.

New apk 0.0.1.9 uploaded.

**24th May, 2017**
- Map display improved, thanks to [osmdroid sample code](https://github.com/osmdroid/osmdroid/tree/master/OpenStreetMapViewer).
- Route Manager button now leads to a very basic browser activity, which will finally become the Route Picker (main activity of the Route Manager).
- Route Creator attached to the Route Manager menu. Save button active, adds the just created route to Data.mRoutesGpx.

**23rd May, 2017**

- All the code imported into a fresh Android Studio project, to finally get rid of the old app rudiments. Project shared from scratch.

**22nd May, 2017**

- MainActivity (together with the FileBrowser) added, needs further adaptation. At the moment the POI Manager button does nothing (this isn't going to change soon), and the Route Manager button leads temporarily to the Route Creator activity.