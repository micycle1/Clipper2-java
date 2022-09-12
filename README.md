# Clipper2-java
A Java port of _[Clipper2](https://github.com/AngusJohnson/Clipper2)_.

### Port Info
* _tangiblesoftwaresolutions_' C# to Java Converter did the heavy lifting.
* Renamed `Area(List<Point64>)` to `AreaPath`.
* `Clipper.class` methods taking in `PointD` types are disabled for now (due to Java type erasure problems).
