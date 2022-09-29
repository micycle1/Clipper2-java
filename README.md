[![](https://jitpack.io/v/micycle1/Clipper2-java.svg)](https://jitpack.io/#micycle1/Clipper2-java)


# Clipper2-java
A Java port of _[Clipper2](https://github.com/AngusJohnson/Clipper2)_.

### Port Info
* _tangiblesoftwaresolutions_' C# to Java Converter did the heavy lifting (but then a lot of manual work was required).
* Code passes all 191 polygon tests.
* Uses lower-case (x,y) for point coordinates
* Benchmarks can be run by appending `jmh:benchmark` to the maven goal.