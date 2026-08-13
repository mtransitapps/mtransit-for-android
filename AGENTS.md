This Android app as multiple Gradle modules organized around git submodules.
The pull requests and branch build is run on all git repositories with changes with the same branch name or the default branch names.

The main repositories with business logic are:
- https://github.com/mtransitapps/mtransit-for-android/: the main Android app
- https://github.com/mtransitapps/commons-android/: the shared Android library code between the main Android app and the other "agency modules" apps
- https://github.com/mtransitapps/parser/ : the JVM data parsing code for GTFS based "agency modules"
- https://github.com/mtransitapps/commons-java/ : the shared JVM code
- https://github.com/mtransitapps/commons/ : the shared shells and automatic code
