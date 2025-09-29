# Migrating from 1.x to 2.x

Version 2.0 uses the new GOL file format, which has been redesigned to allow incremental updates. The query API remains largely identical to previous versions.

## Necessary Steps

- Create new GOL files from OSM data using GOL Tool 2.0 ([download`](https://www.geodesk.com/download) / [repo](https://github.com/clarisma/geodesk-gol))

- Remove all calls to `Feature.isPlaceholder()` -- *placeholder features* no longer exist in GOL 2.0

- Replace all calls to filter factory methods in the `Filters` class with their equivalent methods in `Features` (e.g. `Features.select(Filters.intersecting(...))` becomes `Features.intersecting(...)`)

## Recommended Steps

- Opening a GOL by creating a new `FeatureLibrary` instance has been deprecated; use `Features.open()` instead 


# Migrating from 0.1.x to 0.2

Version 0.2 uses a simpler, more intuitive API. It does, however, introduce some breaking changes. Follow these steps for a smooth upgrade: 

## Necessary Steps

- The package `com.geodesk.core` has been consolidated into `com.geodesk.geom`. Most notably, this affects the `Box` class. Change all `import` statements accordingly.

- The methods of `Node`, `Way` and `Relation` have been moved to `Feature` (their supertype). The subtypes continue to exist as marker interfaces, but you should change all uses of `Node`, `Way` and `Relation` to `Feature`.

- The `Features` interface is no longer generic. Change all uses of `Features<T>` to plain `Features`.

- Change:
    
   Old                           | New
   ------------------------------|-----------------------------
   `Relation.memberNodes()`      | `Feature.members().nodes()`
   `Relation.memberWays()`       |  `Feature.members().ways()`
   `Relation.memberRelations()`  |  `Feature.members().relations()`
   `Feature.parentWays()`        |  `Feature.parents().ways()`
   `Feature.parentRelations()`   | `Feature.parents().relations()`

  Note that in many cases, the type filter can be omitted. For example, in 

  `Feature.members("n[public_transport=stop_position]").nodes()`

  the call to `.nodes()` can be omitted since `n` already restricts the
  members to nodes. 

## Recommended Steps

- The `Filters` class has been deprecated and may be removed in future releases. Instead of creating filters via its factory methods and passing them to `Features.select()`, you can now call filter methods directly on `Features`. So, instead of

   `hotels.select(Filters.within(berlin))`

  you can simply use:

   `hotels.within(berlin)`

  Note that the names of several filter methods have changed to better reflect their purpose:

   Old            | New
   ---------------|---------------------
   `contains()`   | `containing()`
   `crosses()`    | `crossing()`
   `intersects()` | `intersecting()`
   `overlaps()`   | `overlapping()`
   `touches()`    | `touching()`

- Use the new type-check methods instead of `instanceof`:

   Old                           | New
   ------------------------------|---------------------
   `feature instanceof Node`     | `feature.isNode()`
   `feature instanceof Way`      | `feature.isWay()`
   `feature instanceof Relation` | `feature.isRelation()`

