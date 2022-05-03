package com.geodesk.feature.query;

/**
 * Support for bitsets that precisely describe which features to match:
 * - by primitive type
 * - by area flag
 * - by relation membership
 * - whether way has feature nodes, or a feature node belongs to a way
 *
 * We can obtain these bits from the feature flags like this:
 *   (flags >> 1) & 0x1F
 *
 *   0   = area flag
 *   1   = relmember flag
 *   2/3 = type
 *   4   = way-node flag
 *
 * This gives us 32 combinations, which neatly fit in an int.
 * Not all flag combinations are valid:
 * - There is no primitive type 3 (0=node, 1=way, 2=relation)
 * - A node cannot be an area
 * - The waynode flag is invalid for relations
 *   (but may used to indicate special kind of relations in the future)
 *
 * 0   node
 * 1   n/a (node cannot be area)
 * 2   member node
 * 3   n/a
 * 4   way
 * 5   area way
 * 6   member way
 * 7   member area way
 * 8   relation
 * 9   area relation
 * 10  member relation
 * 11  member area relation
 * 12  n/a (invalid type)
 * 13  n/a (invalid type)
 * 14  n/a (invalid type)
 * 15  n/a (invalid type)
 * 16  way-node
 * 17  n/a
 * 18  member way-node
 * 19  n/a
 * 20  way with feature nodes
 * 21  area way with feature nodes
 * 22  member way with feature nodes
 * 23  member area way with feature nodes
 * 24  relation (extra flag)
 * 25  area relation (extra flag)
 * 26  member relation (extra flag)
 * 27  member area relation (extra flag)
 * 28  n/a (invalid type)
 * 29  n/a (invalid type)
 * 30  n/a (invalid type)
 * 31  n/a (invalid type)
 *
 *
 */
public class TypeBits
{
    public static final int NODES        = 0b00000000_00000101_00000000_00000101;
    public static final int WAYS         = 0b00000000_11110000_00000000_11110000;
    public static final int RELATIONS    = 0b00001111_00000000_00001111_00000000;
    public static final int AREAS        = 0b00001010_10100000_00001010_10100000;
    public static final int NONAREA_WAYS        = WAYS & (~AREAS);
    public static final int NONAREA_RELATIONS   = RELATIONS & (~AREAS);
    public static final int ALL                 = NODES | WAYS | RELATIONS;

    public static int fromFeatureFlags(int flags)
    {
        assert (1 << 31) == (1 << 0xffff_ffff);
        return 1 << (flags >> 1);       // Don't need & 0x1F, Java's shift only considers lowest 5 bits
    }
}
