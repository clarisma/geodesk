package com.geodesk.feature.old;

import com.geodesk.feature.*;

import java.util.Collection;

// TODO: remove
// TODO: Should this be "Features"? But I'd like to use that name for the API endpoint!
public interface FeatureCollection<T extends Feature> extends Collection<T>
{
    // TODO: resolve ambiguity: is this a typed ID, or regular ID?
    boolean contains(long id);
    boolean contains(FeatureType type, long id);

    default boolean contains(Class<?> type, long id)
    {
        if(type == Node.class) return contains(FeatureType.NODE, id);
        if(type == Way.class) return contains(FeatureType.WAY, id);
        if(type == Relation.class) return contains(FeatureType.RELATION, id);
        throw new FeatureException(String.format(
            "Type (%s) must be Node, Way, or Relation", type.toString()));
    }

    long[] toIdArray();

    private static boolean immutable()
    {
        throw new UnsupportedOperationException("FeatureCollection is immutable.");
    }

    @Override default boolean add(T e) { return immutable(); }
    @Override default boolean remove(Object o) { return immutable(); }
    @Override default boolean addAll(Collection<? extends T> c) { return immutable(); }
    @Override default boolean removeAll(Collection<?> c) { return immutable(); }
    @Override default boolean retainAll(Collection<?> c) { return immutable(); }
    @Override default void clear() { immutable(); }
}
