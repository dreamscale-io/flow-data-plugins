# Project Setup Instructions

To setup the projects:

cd core
./gradlew idea
cd ../intellij-plugin
./gradlew idea


Within Intellij:

Create a new empty project.

Import module core using the core.iml (hit the + sign to add a module then select the core.iml file)
Set the project SDK to Java 1.6

Import module intellij-plugin using the intellij-plugin.iml
Set the Module SDK to IDEA 14 Common Edition

If you do not have the common edition, you'll need to download and install it on your machine.  Then from this dialog click "New > Intellij Platform SDK" and navigate to the ".app" file for the installation.


