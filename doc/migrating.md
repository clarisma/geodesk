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
   `disjoint()`   | `disjointFrom()`
   `intersects()` | `intersecting()`
   `overlaps()`   | `overlapping()`
   `touches()`    | `touching()`

- Use the new type-check methods instead of `instanceof`:

   Old                           | New
   ------------------------------|---------------------
   `feature instanceof Node`     | `feature.isNode()`
   `feature instanceof Way`      | `feature.isWay()`
   `feature instanceof Relation` | `feature.isRelation()`

