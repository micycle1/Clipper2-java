[![](https://jitpack.io/v/micycle1/Clipper2-java.svg)](https://jitpack.io/#micycle1/Clipper2-java)


# Clipper2-java
A Java port of _[Clipper2](https://github.com/AngusJohnson/Clipper2)_.

### Port Info
* _tangiblesoftwaresolutions_' C# to Java Converter did the heavy lifting (but then a lot of manual work was required).
* Renamed `Area(List<Point64>)` to `AreaPath`.
* `Clipper.class` methods taking in `PointD` types are disabled for now (due to Java type erasure problems).
* Currently passes 118 of the first 120 polygon tests; some of the test cases thereafter seem to get stuck in while loop.
