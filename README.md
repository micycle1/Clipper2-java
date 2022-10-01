[![](https://jitpack.io/v/micycle1/Clipper2-java.svg)](https://jitpack.io/#micycle1/Clipper2-java)


# Clipper2-java
A Java port of _[Clipper2](https://github.com/AngusJohnson/Clipper2)_.

### Port Info
* _tangiblesoftwaresolutions_' C# to Java Converter did the heavy lifting (but then a lot of manual work was required).
* Wrapper objects are used to replicate C# `ref` (pass-by-reference) behaviour. This isn't very "Javaific" but avoids an unmanageable refactoring effort.
* Code passes all polygon and line tests.
* Uses lower-case (x,y) for point coordinates.
* Benchmarks can be run by appending `jmh:benchmark` to the chosen maven goal.
* `scanlineList` from `ClipperBase` uses Java `TreeSet` (variable renamed to `scanlineSet`).