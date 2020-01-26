# Project Setup Instructions

To setup the projects:

`cd core`

`./gradlew idea`

`cd ../intellij-plugin`

`./gradlew idea`

The core plugin also depends on the gridtime-client API, so first, go into the gridtime project, and run:

From gridtime:

`./gradlew pubLocal`

Then from core:

`./gradlew clean check`


Within Intellij:

Create a new empty project.

Import module core using the core.iml (hit the + sign to add a module then select the core.iml file)
Set the project SDK to Java 1.8

Import module intellij-plugin using the intellij-plugin.iml
Set the Module SDK to IDEA 18 Common Edition

You can do this from within the "Open Module Settings" dialog under "SDKs" first add a new Intellj Platform SDK.  Then once the new SDK is added, go to "Modules" tab, select the "intellij-plugin" module, select the "Dependencies" tab within the module, and change the "Module SDK" to your new Intellij Platform SDK.

If you do not have the common edition, you'll need to download and install it on your machine.  Then from this dialog click "New > Intellij Platform SDK" and navigate to the ".app" file for the installation.


