[![](https://jitpack.io/v/micycle1/Clipper2-java.svg)](https://jitpack.io/#micycle1/Clipper2-java)


# Clipper2-java
A Java port of _[Clipper2](https://github.com/AngusJohnson/Clipper2)_.

## Usage

### Overview

The interface of *Clipper2-java* is identical to the original C# version.

The `Clipper` class provides static methods for clipping, path-offsetting, minkowski-sums and path simplification.
For more complex clipping operations (e.g. when clipping open paths or when outputs are expected to include polygons nested within holes of others), use the `Clipper64` or `ClipperD` classes directly.

### Maven
*Clipper2-java* is available as Maven/Gradle artifact via [Jitpack](https://jitpack.io/#micycle1/Clipper2-java).

### Example

```java
Paths64 subj = new Paths64();
Paths64 clip = new Paths64();
subj.add(Clipper.MakePath(new int[] { 100, 50, 10, 79, 65, 2, 65, 98, 10, 21 }));
clip.add(Clipper.MakePath(new int[] { 98, 63, 4, 68, 77, 8, 52, 100, 19, 12 }));
Paths64 solution = Clipper.Union(subj, clip, FillRule.NonZero);
solution.get(0).forEach(p -> System.out.println(p.toString()));
```


## Port Info
* _tangiblesoftwaresolutions_' C# to Java Converter did the heavy lifting (but then a lot of manual work was required).
* Wrapper objects are used to replicate C# `ref` (pass-by-reference) behaviour. This isn't very Java-esque but avoids an unmanageable refactoring effort.
* Code passes all tests: polygon, line and polytree.
* Uses lower-case (x, y) for point coordinates.
* Private local variables have been renamed to their _camelCase_ variant but public methods (i.e. those of `Clipper.class`) retain their C# _PascalCase_ names (for now...).
* Benchmarks can be run by including `jmh:benchmark` to the chosen maven goal.
* `scanlineList` from `ClipperBase` uses Java `TreeSet` (variable renamed to `scanlineSet`).

## Benchmark
_lightbringer's_ Java [port](https://github.com/lightbringer/clipper-java) of Clipper1 is benchmarked against this project in the benchmarks. *Clipper2-java* is faster, which becomes more pronounced input size grows.
```
Benchmark               (edgeCount)  Mode  Cnt  Score   Error  Units
Clipper1.Intersection         1000  avgt    2  0.209           s/op
Clipper1.Intersection         2000  avgt    2  1.123           s/op
Clipper1.Intersection         4000  avgt    2  9.691           s/op
Clipper2.Intersection         1000  avgt    2  0.130           s/op
Clipper2.Intersection         2000  avgt    2  0.852           s/op
Clipper2.Intersection         4000  avgt    2  3.465           s/op
```
