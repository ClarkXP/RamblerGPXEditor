CHANGELOG
===============
**28th May, 2017**

- Route labels added to the Routes Browser (in the half way route point).


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