# KahootJ

Reverse engineering kahoot.it

This has similar functionality to the code [here (kahoot-hack)](https://github.com/Arccotangent/kahoot-hack) but it is being rewritten as a library so that you can develop custom Kahoot! apps and clients more easily.

Working as of November 26th, 2016

## Building

This program uses the Gradle build system. Dependencies are managed by Gradle. Open a terminal/cmd window and navigate to the kahoot-hack folder.

To build on \*nix like systems: `./gradlew build`

To build on Windows: `gradlew build`

The build shouldn't take more than a few minutes on the first run. The final built jar can be found in the build/libs folder. The built jar is portable, meaning you can copy it to any spot on your computer (or on any computer) and run it from there.

## Usage

Usage details coming soon(TM).

## Contributing

You are welcome to contribute by submitting a pull request or opening an issue on this repository. Any issues or PRs that are trolls will simply be closed.

## Credits

Thanks to Arccotangent for the original code! I just tidied it up and replaced some libraries with multiplatform ones.
