<img src="https://docs.geodesk.com/img/github-header.png">


GeoDesk is a fast and storage-efficient geospatial database for OpenStreetMap data. Also available [for C++](https://github.com/clarisma/libgeodesk) and [for Python](https://github.com/clarisma/geodesk-py).

## Why GeoDesk?

- **Small storage footprint** &mdash; GeoDesk's GOL files are only 20% to 50% larger than the original OSM data in PBF format &mdash; that's less than a tenth of the storage consumed by a traditional SQL-based database.

- **Fast queries** &mdash; typically 50 times faster than SQL. 

- **Fast to get started** &mdash; Converting `.osm.pbf` data to a GOL is 20 times faster than an import into an SQL database. Alternatively, download pre-made data tiles for just the regions you need and automatically assemble them into a GOL.

- **Intuitive API** &mdash; No need for object-relational mapping; GeoDesk queries return `Node`, `Way` and `Relation` objects. Quickly discover tags, way-nodes and relation members. Get a feature's `Geometry`, measure its length/area. 
 
- **Proper handling of relations** &mdash; (Traditional geospatial databases deal with geometric shapes and require workarounds to support this unique and powerful aspect of OSM data.)

- **Seamless integration with the Java Topology Suite (JTS)** for advanced geometric operations, such as buffer, union, simplify, convex and concave hulls, Voronoi diagrams, and much more.

- **Modest hardware requirements** &mdash; If it can run a 64-bit JVM, it'll run GeoDesk.
 
## Get Started

### Maven

Include this dependency in your project's `pom.xml`:

```xml
<dependency>
    <groupId>com.geodesk</groupId>
    <artifactId>geodesk</artifactId>
    <version>1.0.2</version>
</dependency>
```

Alternatively, to build the latest version from source:

```
git clone https://github.com/clarisma/geodesk.git
cd geodesk
mvn install
```

### Example Application

```java
import com.geodesk.feature.*;
import com.geodesk.geom.*;

public class PubsExample
{
    public static void main(String[] args)
    {
        FeatureLibrary library = new FeatureLibrary(
            "switzerland.gol");                          // 1
        
        for(Feature pub: library                         // 2
            .select("na[amenity=pub]")                   // 3
            .in(Box.ofWSEN(8.53,47.36,8.55,47.38)))      // 4
        {
            System.out.println(pub.stringValue("name")); // 5
        }
        
        library.close();                                 // 6
    }
}
```

What's going on here?

1. We're opening a feature library (`switzerland.gol`)

2. We iterate through all the features ...

3. ... that are pubs ([GeoDesk query language](https://docs.geodesk.com/goql) &mdash; *similar to MapCSS*)

4. ... in downtown Zurich (*bounding box with West/South/East/North coordinates*).

5. We print the name of each pub.

6. We close the library.

That's it, you've created your first GeoDesk application! 

### More Examples

Find all movie theaters within 500 meters from a given point:

```java
Features movieTheaters = library
    .select("na[amenity=cinema]")
    .maxMetersFromLonLat(500, myLon, myLat);
```

*Remember, OSM uses British English for its terminology.*

Discover the bus routes that traverse a given street:

```java
for(Feature route: street.parents("[route=bus]"))
{
    System.out.format("- %s from %s to %s",
        route.stringValue("ref"),
        route.stringValue("from"),
        route.stringValue("To"));
}
```

Count the number of entrances of a building:

```java
int numberOfEntrances = building.nodes("[entrance]").size();
```

## Documentation

- [GeoDesk Developer's Guide](https://docs.geodesk.com/java)
- [API Reference](https://apidocs.geodesk.com/0.2)

## Related Repositories

- [gol-tool](http://www.github.com/clarisma/gol-tool) &mdash; command-line utility for building, maintaining and querying GOL files
- [geodesk-examples](http://www.github.com/clarisma/geodesk-examples) &mdash; example applications
- [geodesk-tests](http://www.github.com/clarisma/geodesk-tests) &mdash; integration tests
- [libgeodesk](https://github.com/clarisma/libgeodesk) &mdash; GeoDesk for C++ 
- [geodesk-py](https://github.com/clarisma/geodesk-py) &mdash; GeoDesk for Python
