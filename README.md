[![](https://jitpack.io/v/micycle1/Clipper2-java.svg)](https://jitpack.io/#micycle1/Clipper2-java)


# Clipper2-java
A Java port of _[Clipper2](https://github.com/AngusJohnson/Clipper2)_.

### Port Info
* _tangiblesoftwaresolutions_' C# to Java Converter did the heavy lifting (but then a lot of manual work was required).
* Wrapper objects are used to replicate C# `ref` (pass-by-reference) behaviour. This isn't very Java-esque but avoids an unmanageable refactoring effort.
* Code passes all tests: polygon, line and polytree.
* Uses lower-case (x,y) for point coordinates.
* Private local variables have been renamed to their _camelCase_ variant but public methods (i.e. those of `Clipper.class`) retain their C# _PascalCase_ names (for now...).
* Benchmarks can be run by appending `jmh:benchmark` to the chosen maven goal.
* `scanlineList` from `ClipperBase` uses Java `TreeSet` (variable renamed to `scanlineSet`).
