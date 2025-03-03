/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.client;

import com.oracle.coherence.client.NamedCacheGrpcClient;
import com.oracle.coherence.grpc.Requests;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.OperationalContext;

import com.tangosol.net.grpc.GrpcDependencies;
import com.tangosol.util.Base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.extension.RegisterExtension;

import org.junit.jupiter.params.ParameterizedTest;

import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration tests for {@link NamedCacheGrpcClient} values() methods.
 *
 * @author Jonathan Knight  2019.11.12
 * @since 20.06
 */
class NamedCacheServiceValuesIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setupBaseTest() throws Exception
        {
        s_realCache = s_serverHelper.getSession().getCache(CACHE_NAME);
        }

    @BeforeEach
    void beforeEach()
        {
        s_realCache.clear();
        }

    private <K, V> NamedCache<K, V> createClient(String serializerName, Serializer serializer)
        {
        return s_serverHelper.createClient(GrpcDependencies.DEFAULT_SCOPE, CACHE_NAME, serializerName, serializer);
        }

    // ----- test methods ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetValuesOfEmptyCache(String serializerName, Serializer serializer)
        {
        s_realCache.clear();
        NamedCache<String, String> cache = createClient(serializerName, serializer);

        Collection<String> values = cache.values();
        assertThat(values, is(notNullValue()));
        assertThat(values.isEmpty(), is(true));
        assertThat(values.size(), is(0));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetValuesIteratorOfEmptyCache(String serializerName, Serializer serializer)
        {
        s_realCache.clear();
        NamedCache<String, String> cache = createClient(serializerName, serializer);

        Collection<String> values = cache.values();
        assertThat(values, is(notNullValue()));

        Iterator<String> iterator = values.iterator();
        assertThat(iterator, is(notNullValue()));
        assertThat(iterator.hasNext(), is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetValuesOfPopulatedCache(String serializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(serializerName, serializer);

        Collection<String> values = cache.values();
        assertThat(values, is(notNullValue()));
        assertThat(values.isEmpty(), is(false));
        assertThat(values.size(), is(s_realCache.size()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetValuesIteratorOfPopulatedCache(String serializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(serializerName, serializer);

        Collection<String> values = cache.values();
        assertThat(values, is(notNullValue()));

        Iterator<String> iterator = values.iterator();
        assertThat(iterator, is(notNullValue()));
        assertThat(iterator.hasNext(), is(true));

        Set<String> col   = new HashSet<>();
        int         count = 0;
        while (iterator.hasNext())
            {
            col.add(iterator.next());
            count++;
            }

        assertThat(count, is(s_realCache.size()));

        HashSet<String> expected = new HashSet<>(s_realCache.values());
        assertThat(col, is(expected));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldRemoveFromValuesIteratorOfPopulatedCache(String serializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(serializerName, serializer);

        Collection<String> values = cache.values();
        assertThat(values, is(notNullValue()));

        Iterator<String> iterator = values.iterator();
        assertThat(iterator, is(notNullValue()));
        assertThat(iterator.hasNext(), is(true));

        while (iterator.hasNext())
            {
            iterator.next();
            iterator.remove();
            }

        assertThat(s_realCache.isEmpty(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldClearValues(String serializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(serializerName, serializer);

        Collection<String> values = cache.values();
        values.clear();
        assertThat(s_realCache.isEmpty(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldContainAll(String serializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(serializerName, serializer);

        Collection<String> values = cache.values();
        boolean            result = values.containsAll(Arrays.asList("value-1", "value-2", "value-3"));
        assertThat(result, is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldNotContainAll(String serializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(serializerName, serializer);

        Collection<String> values = cache.values();
        boolean            result = values.containsAll(Arrays.asList("value-1", "value-B", "value-3"));
        assertThat(result, is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldContain(String serializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(serializerName, serializer);

        Collection<String> values = cache.values();

        for (String s : s_realCache.values())
            {
            boolean result = values.contains(s);
            assertThat("Values should contain key " + s, result, is(true));
            }
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldNotContainKey(String serializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(serializerName, serializer);

        Collection<String> values = cache.values();
        boolean            result = values.contains("value-A");
        assertThat(result, is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldBeEqual(String serializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(serializerName, serializer);
        Set<String>                set   = new HashSet<>(s_realCache.values());

        Collection<String> values = cache.values();
        assertThat(values.equals(set), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldNotBeEqual(String serializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(serializerName, serializer);
        Set<String>                set   = new HashSet<>(Arrays.asList("value-1", "value-2", "value-3"));

        Collection<String> values = cache.values();
        assertThat(values.equals(set), is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldHaveSameHashCode(String serializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(serializerName, serializer);
        Set<String>                set   = new HashSet<>(s_realCache.values());

        Collection<String> values = cache.values();
        assertThat(values.hashCode(), is(set.hashCode()));
        }

    // ToDo: enable when cache.stream(Filter) is implemented
    //@ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldRemoveAll(String serializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(serializerName, serializer);
        Set<String>                set   = new HashSet<>(Arrays.asList("value-1", "value-2", "value-3", "value-A",
                                                                       "value-B"));

        Collection<String> values = cache.values();
        boolean            result = values.removeAll(set);
        assertThat(result, is(true));
        assertThat(s_realCache.size(), is(7));
        assertThat(s_realCache.containsValue("value-1"), is(false));
        assertThat(s_realCache.containsValue("value-2"), is(false));
        assertThat(s_realCache.containsValue("value-3"), is(false));
        }

    // ToDo: enable when cache.stream(Filter) is implemented
    //@ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldNotRemoveAll(String serializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(serializerName, serializer);
        Set<String>                set   = new HashSet<>(Arrays.asList("value-A", "value-B"));

        Collection<String> values = cache.values();
        boolean            result = values.removeAll(set);
        assertThat(result, is(false));
        assertThat(s_realCache.size(), is(10));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldRetainAll(String serializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(serializerName, serializer);
        Set<String>                set   = new HashSet<>(Arrays.asList("value-1", "value-2", "value-3", "value-A",
                                                                       "value-B"));

        Collection<String> values = cache.values();
        boolean            result = values.retainAll(set);
        assertThat(result, is(true));
        assertThat(s_realCache.size(), is(3));
        assertThat(s_realCache.containsValue("value-1"), is(true));
        assertThat(s_realCache.containsValue("value-2"), is(true));
        assertThat(s_realCache.containsValue("value-3"), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldRetainAllWithNoneMatchingAndAllRemoved(String serializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(serializerName, serializer);
        Set<String>                set   = new HashSet<>(Arrays.asList("value-A", "value-B"));

        Collection<String> values = cache.values();
        boolean            result = values.retainAll(set);
        assertThat(result, is(true));
        assertThat(s_realCache.size(), is(0));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldRetainAllWhereAllMatchAndNoneRemoved(String serializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(serializerName, serializer);

        Collection<String> values = cache.values();
        boolean            result = values.retainAll(s_realCache.values());
        assertThat(result, is(false));
        assertThat(s_realCache.size(), is(10));
        }


    // ToDo: enable this when entrySet(Filter) is implemented
    //@ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldConvertToObjectArray(String serializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(serializerName, serializer);

        Collection<String> entries  = cache.values();
        Object[]           result   = entries.toArray();
        Object[]           expected = s_realCache.entrySet().toArray();
        assertThat(result, is(expected));
        }

    // ToDo: enable this when entrySet(Filter) is implemented
    //@ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldConvertToArray(String serializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(serializerName, serializer);

        Collection<String> keys     = cache.values();
        String[]           result   = keys.toArray(new String[0]);
        String[]           expected = s_realCache.values().toArray(new String[0]);
        assertThat(result, is(expected));
        }

    // ----- helper methods -------------------------------------------------

    protected void clearAndPopulateRealCache(int count)
        {
        s_realCache.clear();
        for (int i = 0; i < count; i++)
            {
            s_realCache.put("value-" + i, "value-" + i);
            }
        }

    /**
     * Obtain the {@link com.tangosol.io.Serializer} instances to use for parameterized
     * test {@link org.junit.jupiter.params.provider.Arguments}.
     *
     * @return the {@link com.tangosol.io.Serializer} instances to use for test
     * {@link org.junit.jupiter.params.provider.Arguments}
     */
    protected static Stream<Arguments> serializers()
        {
        List<Arguments> args   = new ArrayList<>();
        ClassLoader     loader = Base.getContextClassLoader();

        args.add(Arguments.of("", new DefaultSerializer()));

        OperationalContext ctx = (OperationalContext) CacheFactory.getCluster();
        for (Map.Entry<String, SerializerFactory> entry : ctx.getSerializerMap().entrySet())
            {
            args.add(Arguments.of(entry.getKey(), entry.getValue().createSerializer(loader)));
            }

        return args.stream();
        }

    // ----- constants ------------------------------------------------------

    protected static final String CACHE_NAME = "testCache";

    // ----- data members ---------------------------------------------------

    protected static NamedCache<String, String> s_realCache;

    @RegisterExtension
    protected static ServerHelper s_serverHelper = new ServerHelper()
            .setProperty("coherence.ttl", "0")
            .setProperty("coherence.wka", "127.0.0.1")
            .setProperty("coherence.localhost", "127.0.0.1")
            .setProperty("coherence.clustername", "NamedCacheServiceValuesIT")
            .setProperty("coherence.override", "coherence-json-override.xml")
            .setProperty("coherence.pof.config", "test-pof-config.xml")
            .setProperty("coherence.cacheconfig", "coherence-config.xml");
    }
