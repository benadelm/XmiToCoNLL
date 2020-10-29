/* This Source Code Form is subject to the terms of the hermA Licence.
 * If a copy of the licence was not distributed with this file, You have
 * received this Source Code Form in a manner that does not comply with
 * the terms of the licence.
 */
package herma.xmi2conll;

import java.util.HashMap;
import java.util.Map;

public class CollectionUtil {
	
	private static final Long LONG_ONE = Long.valueOf(1L);
	
	/**
	 * Returns the {@link HashMap}
	 * associated with the specified key
	 * in the given {@link Map}.
	 * If there is no mapping for the key yet,
	 * inserts a newly created {@link HashMap}
	 * and returns that newly created {@link HashMap}.
	 * <p>
	 * As a consequence, this method never returns {@code null}
	 * and when this method returns,
	 * the given {@link Map} maps the specified key
	 * to the {@link HashMap} instance
	 * returned by this method.
	 * </p>
	 * 
	 * @param <K1>
	 * the key type of the outer {@link Map}
	 * 
	 * @param <K2>
	 * the key type of the inner {@link HashMap}
	 * 
	 * @param <V>
	 * the value type of the inner {@link HashMap}
	 * 
	 * @param map
	 * the outer {@link Map};
	 * not {@code null}
	 * 
	 * @param key
	 * the key;
	 * {@code null} may be permitted depending on the outer {@link Map}
	 * 
	 * @return
	 * a {@link HashMap} instance, not {@code null};
	 * the given {@link Map} maps the specified key
	 * to the returned {@link HashMap} instance
	 */
	public static <K1, K2, V> HashMap<K2, V> getSubmap(final Map<K1, HashMap<K2, V>> map, final K1 key) {
		return map.computeIfAbsent(key, id -> new HashMap<>());
	}
	
	/**
	 * Increments the {@link Long} value
	 * associated with the specified key
	 * in the given {@link Map}.
	 * If there is no mapping for the key yet,
	 * inserts a {@link Long} with a value of {@code 1L}.
	 * 
	 * @param <K>
	 * the key type of the {@link Map}
	 * 
	 * @param map
	 * the {@link Map};
	 * not {@code null}
	 * 
	 * @param key
	 * the key;
	 * {@code null} may be permitted depending on the {@link Map}
	 */
	public static <K> void increment(final Map<K, Long> map, final K key) {
		map.merge(key, LONG_ONE, CollectionUtil::incrementLong);
	}
	
	private static Long incrementLong(final Long value, final Long unused) {
		return Long.valueOf(Math.incrementExact(value.longValue()));
	}
	
}
