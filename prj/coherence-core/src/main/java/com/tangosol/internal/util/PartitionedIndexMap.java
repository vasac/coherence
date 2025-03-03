/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.util;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.AbstractKeyBasedMap;
import com.tangosol.util.Base;
import com.tangosol.util.ChainedSet;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.MapIndex;
import com.tangosol.util.SafeSortedMap;
import com.tangosol.util.SegmentedHashMap;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.comparator.SafeComparator;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A composite view over partition indices for the specified partition set.
 *
 * @param <K>  the type of cache keys
 * @param <V>  the type of cache values
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class PartitionedIndexMap<K, V>
        extends AbstractKeyBasedMap<ValueExtractor<V, ?>, MapIndex<K, V, ?>>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code PartitionedIndexMap} instance.
     *
     * @param ctx             the {@link BackingMapContext context} associated with the indexed cache
     * @param mapPartitioned  the map of partition indices, keyed by partition number
     * @param partitions      the set of partitions to return an index map for, or {@code null}
     *                        to return an index for all owned partitions
     */
    public PartitionedIndexMap(BackingMapContext ctx,
                               Map<Integer, Map<ValueExtractor<V, ?>, MapIndex<K, V, ?>>> mapPartitioned,
                               PartitionSet partitions)
        {
        f_ctx            = ctx;
        f_mapPartitioned = mapPartitioned;
        f_partitions     = partitions;
        }

    // ---- public API ------------------------------------------------------

    /**
     * Return a {@link MapIndex} for the specified extractor.
     *
     * @param extractor  the value extractor to get the {@code MapIndex} for
     *
     * @return a {@link MapIndex} for the specified extractor
     */
    public <E> MapIndex<K, V, E> get(ValueExtractor<V, E> extractor)
        {
        int nPart = f_partitions == null
                    ? f_mapPartitioned.keySet().stream().findFirst().orElse(-1)
                    : f_partitions.rnd();

        MapIndex<K, V, E> mapIndex = getMapIndex(nPart, extractor);
        if (mapIndex != null)
            {
            return f_partitions != null && f_partitions.cardinality() == 1
                   ? mapIndex   // optimize for single partition
                   : new PartitionedIndex(extractor, mapIndex.isOrdered(), mapIndex.getComparator());
            }
        return null;
        }

    /**
     * Return the set of partitions this index view is for.
     *
     * @return the set of partitions this index view is for
     */
    public Iterable<Integer> getPartitions()
        {
        return f_partitions == null ? f_mapPartitioned.keySet() : f_partitions;
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Return {@link MapIndex} for the specified partition and extractor.
     *
     * @param nPart      the partition to get the index for
     * @param extractor  the extractor to get the index for
     *
     * @return a {@link MapIndex} for the specified partition and extractor, or
     *         {@code null} if this view does nopt contain specified partition
     *         or an index for the specified extractor
     */
    protected <E> MapIndex<K, V, E> getMapIndex(int nPart, ValueExtractor<V, E> extractor)
        {
        if (nPart >= 0 && (f_partitions == null ? f_mapPartitioned.containsKey(nPart) : f_partitions.contains(nPart)))
            {
            Map<ValueExtractor<V, ?>, MapIndex<K, V, ?>> mapPart = f_mapPartitioned.get(nPart);
            if (mapPart != null)
                {
                return (MapIndex<K, V, E>) mapPart.get(extractor);
                }
            }
        return null;
        }

    // ---- AbstractKeyBasedMap interface -----------------------------------

    @Override
    public MapIndex<K, V, ?> get(Object oKey)
        {
        return get((ValueExtractor<V, ?>) oKey);
        }

    @Override
    protected Iterator<ValueExtractor<V, ?>> iterateKeys()
        {
        return f_mapPartitioned.values().stream()
                .flatMap(map -> map.keySet().stream())
                .distinct()
                .iterator();
        }

    @Override
    public String toString()
        {
        return "PartitionedIndexMap{" +
               "f_ctx=" + f_ctx +
               ", f_mapPartitioned=" + f_mapPartitioned +
               ", f_partitions=" + f_partitions +
               '}';
        }

    // ---- inner class: PartitionedIndex -----------------------------------

    /**
     * Provides unified view over multiple partition-level MapIndex instances
     * for a specific index.
     *
     * @param <E>  the type of indexed attribute
     */
    public class PartitionedIndex<E>
            implements MapIndex<K, V, E>
        {
        // ---- constructors ------------------------------------------------

        /**
         * Construct {@link PartitionedIndex} instance.
         *
         * @param extractor  the {@code ValueExtractor} for this index
         *
         */
        PartitionedIndex(ValueExtractor<V, E> extractor, boolean fOrdered, Comparator<E> comparator)
            {
            f_extractor  = extractor;
            f_fOrdered   = fOrdered;
            f_comparator = fOrdered ? ensureSafeComparator(comparator) : null;
            }

        /**
         * Ensures that the comparator used for index sorting supports {@code null} values.
         *
         * @param comparator  the comparator to wrap, if necessary; can be {@code null}, in
         *                    which case a trivial {@code SafeComparator} that uses natural
         *                    ordering will be returned
         *
         * @return a {@link SafeComparator} instance
         *
         * @param <T>  the type of objects that may be compared by this comparator
         */
        private static <T> Comparator<T> ensureSafeComparator(Comparator<T> comparator)
            {
            return comparator == null
                           ? SafeComparator.INSTANCE()
                           : comparator instanceof SafeComparator
                             ? comparator
                             : new SafeComparator<>(comparator);
            }

        // ---- MapIndex interface ------------------------------------------

        @Override
        public ValueExtractor<V, E> getValueExtractor()
            {
            return f_extractor;
            }

        @Override
        public boolean isOrdered()
            {
            return f_fOrdered;
            }

        @Override
        public boolean isPartial()
            {
            for (Map<ValueExtractor<V, ?>, MapIndex<K, V, ?>> mapPart : f_mapPartitioned.values())
                {
                MapIndex<K, V, ?> mapIndex = mapPart.get(f_extractor);
                if (mapIndex != null && mapIndex.isPartial())
                    {
                    return true;
                    }
                }
            return false;
            }

        @Override
        public Map<E, Set<K>> getIndexContents()
            {
            return isOrdered()
                   ? new SortedIndexContents()
                   : new IndexContents();
            }

        @Override
        public Object get(K key)
            {
            int nPart = f_ctx.getManagerContext().getKeyPartition(key);

            MapIndex<K, V, E> mapIndex = getMapIndex(nPart, f_extractor);
            return mapIndex == null ? NO_VALUE : mapIndex.get(key);
            }

        @Override
        public Comparator<E> getComparator()
            {
            return f_comparator;
            }

        @Override
        public void insert(Entry<? extends K, ? extends V> entry)
            {
            throw new UnsupportedOperationException("PartitionedIndex is read-only");
            }

        @Override
        public void update(Entry<? extends K, ? extends V> entry)
            {
            throw new UnsupportedOperationException("PartitionedIndex is read-only");
            }

        @Override
        public void delete(Entry<? extends K, ? extends V> entry)
            {
            throw new UnsupportedOperationException("PartitionedIndex is read-only");
            }

        @Override
        public long getUnits()
            {
            long cUnits = 0;

            for (int nPart : getPartitions())
                {
                MapIndex<K, V, E> mapIndex = getMapIndex(nPart, f_extractor);
                cUnits += (mapIndex != null ? mapIndex.getUnits() : 0);
                }

            return Math.round(cUnits * 1.1);
            }

        // ---- Object methods ----------------------------------------------

        @Override
        public String toString()
            {
            return toString(false);
            }

        /**
         * Returns a string representation of this PartitionedIndex.  If called in
         * verbose mode, include the contents of the index (the inverse
         * index). Otherwise, just print the number of entries in the index.
         *
         * @param fVerbose  if true then print the content of the index otherwise
         *                  print the number of entries
         *
         * @return a String representation of this PartitionedIndex
         */
        public String toString(boolean fVerbose)
            {
            return ClassHelper.getSimpleName(getClass())
                   + ": Extractor=" + getValueExtractor()
                   + ", Ordered=" + isOrdered()
                   + ", Footprint=" + Base.toMemorySizeString(getUnits(), false)
                   + (fVerbose ? ", Content[" + getIndexContents().size() + "]=" + getIndexContents().keySet() : "");
            }

        // ---- inner class: IndexContents ----------------------------------

        /**
         * Provides composite view over the contents/inverse maps of unordered
         * partitioned indices.
         */
        private class IndexContents
                implements Map<E, Set<K>>
            {
            // ---- Map interface -------------------------------------------

            @Override
            public int size()
                {
                return keySet().size();
                }

            @Override
            public boolean isEmpty()
                {
                for (int nPart : getPartitions())
                    {
                    if (!getIndexContents(nPart).isEmpty())
                        {
                        return false;
                        }
                    }
                return true;
                }

            @Override
            public boolean containsKey(Object oKey)
                {
                for (int nPart : getPartitions())
                    {
                    if (getIndexContents(nPart).containsKey(oKey))
                        {
                        return true;
                        }
                    }
                return false;
                }

            @Override
            public boolean containsValue(Object oValue)
                {
                return values().contains(oValue);
                }

            @Override
            public Set<K> get(Object oKey)
                {
                int          cCapacity   = f_partitions == null
                                           ? f_mapPartitioned.size()
                                           : f_partitions.cardinality();
                List<Set<K>> listKeySets = new ArrayList(cCapacity);
                for (int nPart : getPartitions())
                    {
                    Set setKeys = getIndexContents(nPart).get(oKey);
                    if (setKeys != null && !setKeys.isEmpty())
                        {
                        listKeySets.add(setKeys);
                        }
                    }
                return new ChainedSet<>(listKeySets);
                }

            @Override
            public Set<K> put(E key, Set<K> value)
                {
                throw new UnsupportedOperationException("PartitionedIndex is read-only");
                }

            @Override
            public Set<K> remove(Object key)
                {
                throw new UnsupportedOperationException("PartitionedIndex is read-only");
                }

            @Override
            @SuppressWarnings("NullableProblems")
            public void putAll(Map<? extends E, ? extends Set<K>> m)
                {
                throw new UnsupportedOperationException("PartitionedIndex is read-only");
                }

            @Override
            public void clear()
                {
                throw new UnsupportedOperationException("PartitionedIndex is read-only");
                }

            @Override
            public Set<E> keySet()
                {
                Set<E> setKeys = instantiateKeySet();
                for (int nPart : getPartitions())
                    {
                    setKeys.addAll(getIndexContents(nPart).keySet());
                    }
                return setKeys;
                }

            @Override
            public Collection<Set<K>> values()
                {
                Set<E> setKeys = keySet();

                return new AbstractCollection<>()
                    {
                    public Iterator<Set<K>> iterator()
                        {
                        Iterator<E> itKeys = setKeys.iterator();

                        return new Iterator<>()
                            {
                            public boolean hasNext()
                                {
                                return itKeys.hasNext();
                                }

                            public Set<K> next()
                                {
                                return get(itKeys.next());
                                }
                            };
                        }

                    public int size()
                        {
                        return setKeys.size();
                        }
                    };
                }

            @Override
            public Set<Map.Entry<E, Set<K>>> entrySet()
                {
                Set<E> setKeys = keySet();

                return new AbstractSet<>()
                    {
                    public Iterator<Map.Entry<E, Set<K>>> iterator()
                        {
                        Iterator<E> itKeys = setKeys.iterator();

                        return new Iterator<>()
                            {
                            public boolean hasNext()
                                {
                                return itKeys.hasNext();
                                }

                            public Map.Entry<E, Set<K>> next()
                                {
                                return new Entry(itKeys.next());
                                }
                            };
                        }

                    public int size()
                        {
                        return setKeys.size();
                        }
                    };
                }

            // ---- helpers -------------------------------------------------

            /**
             * Returns an empty map (immutable).
             *
             * @return the empty map
             */
            protected Map<E, Set<K>> emptyMap()
                {
                return (Map<E, Set<K>>) SegmentedHashMap.EMPTY;
                }

            /**
             * Instantiate a set to collect the keys into.
             *
             * @return the set to collect the keys into
             */
            protected Set<E> instantiateKeySet()
                {
                return new HashSet<>();
                }

            /**
             * Return the index contents for the specified partition.
             *
             * @param nPart  the partition to get index contents for
             *
             * @return the index contents for the specified partition
             */
            protected Map<E, Set<K>> getIndexContents(int nPart)
                {
                MapIndex<K, V, E> mapIndex = getMapIndex(nPart, f_extractor);
                return mapIndex != null
                       ? mapIndex.getIndexContents()
                       : emptyMap();
                }

            // ---- inner class: Entry --------------------------------------

            /**
             * Virtual inverse index Entry.
             */
            class Entry implements Map.Entry<E, Set<K>>
                {
                /**
                 * Construct an {@code Entry}.
                 *
                 * @param key  the key for this entry
                 */
                Entry(E key)
                    {
                    f_key = key;
                    }

                @Override
                public E getKey()
                    {
                    return f_key;
                    }

                @Override
                public Set<K> getValue()
                    {
                    return get(f_key);
                    }

                @Override
                public Set<K> setValue(Set<K> value)
                    {
                    throw new UnsupportedOperationException("PartitionedIndex is read-only");
                    }

                // ---- data members ----------------------------------------

                /**
                 * The key for this Entry.
                 */
                private final E f_key;
                }
            }

        // ---- inner class: SortedIndexContents ----------------------------

        /**
         * Provides composite view over the contents/inverse maps of ordered
         * partitioned indices.
         */
        @SuppressWarnings("NullableProblems")
        class SortedIndexContents
                extends IndexContents
                implements SortedMap<E, Set<K>>
            {
            // ---- SortedMap interface -------------------------------------

            @Override
            public Comparator<? super E> comparator()
                {
                return f_comparator;
                }

            @Override
            public SortedMap<E, Set<K>> subMap(E fromKey, E toKey)
                {
                SafeSortedMap mapSorted = new SafeSortedMap(f_comparator);

                for (int nPart : getPartitions())
                    {
                    Map<E, Set<K>> subMap = getIndexContents(nPart).subMap(fromKey, toKey);
                    subMap.forEach((k, v) -> mapSorted.merge(k, v, (v1, v2) -> chainSets((Set<K>) v1, (Set<K>) v2)));
                    }

                return mapSorted;
                }

            @Override
            public SortedMap<E, Set<K>> headMap(E toKey)
                {
                SafeSortedMap mapSorted = new SafeSortedMap(f_comparator);

                for (int nPart : getPartitions())
                    {
                    Map<E, Set<K>> headMap = getIndexContents(nPart).headMap(toKey);
                    headMap.forEach((k, v) -> mapSorted.merge(k, v, (v1, v2) -> chainSets((Set<K>) v1, (Set<K>) v2)));
                    }

                return mapSorted;
                }

            @Override
            public SortedMap<E, Set<K>> tailMap(E fromKey)
                {
                SafeSortedMap mapSorted = new SafeSortedMap(f_comparator);

                for (int nPart : getPartitions())
                    {
                    Map<E, Set<K>> tailMap = getIndexContents(nPart).tailMap(fromKey);
                    tailMap.forEach((k, v) -> mapSorted.merge(k, v, (v1, v2) -> chainSets((Set<K>) v1, (Set<K>) v2)));
                    }

                return mapSorted;
                }

            @Override
            public SortedSet<E> keySet()
                {
                return (SortedSet<E>) super.keySet();
                }

            @Override
            public E firstKey()
                {
                return keySet().first();
                }

            @Override
            public E lastKey()
                {
                return keySet().last();
                }

            // ---- helpers -------------------------------------------------

            @Override
            protected Map<E, Set<K>> emptyMap()
                {
                return (Map<E, Set<K>>) SafeSortedMap.EMPTY;
                }

            @Override
            protected SortedSet<E> instantiateKeySet()
                {
                return new TreeSet<>(f_comparator);
                }

            @Override
            protected SortedMap<E, Set<K>> getIndexContents(int nPart)
                {
                return (SortedMap<E, Set<K>>) super.getIndexContents(nPart);
                }

            /**
             * Create {@link ChainedSet} from two {@code Set}s or an existing
             * {@code ChainedSet} and another {@code Set}.
             *
             * @param setFirst   the first Set; could be a ChainedSet
             * @param setSecond  the second Set
             *
             * @return a ChainedSet view over specified Sets
             */
            private Set<K> chainSets(Set<K> setFirst, Set<K> setSecond)
                {
                return setFirst instanceof ChainedSet<K>
                       ? new ChainedSet<>((ChainedSet<K>) setFirst, setSecond)
                       : new ChainedSet<>(setFirst, setSecond);
                }
            }

        // ---- data members ------------------------------------------------

        /**
         * The extractor for this index.
         */
        private final ValueExtractor<V, E> f_extractor;

        /**
         * Flag specifying whether this index is ordered.
         */
        private final boolean f_fOrdered;

        /**
         * The Comparator to use for sorting, or  {@code null} if natural
         * ordering should be used.
         */
        private final Comparator<E> f_comparator;
        }

    // ---- data members ----------------------------------------------------

    /**
     * The {@link BackingMapContext context} associated with the indexed cache.
     */
    protected final BackingMapContext f_ctx;

    /**
     * The map of partition indices, keyed by partition number.
     */
    protected final Map<Integer, Map<ValueExtractor<V, ?>, MapIndex<K, V, ?>>> f_mapPartitioned;

    /**
     * The set of partitions to create an index map for. Could be {@code null},
     * in which case all partitions owned by the local member should be included
     * into this index map.
     */
    protected final PartitionSet f_partitions;
    }
