package com.sivavr.ehcache.cache;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import com.sivavr.ehcache.dao.SuperHeroDAO;
import com.sivavr.ehcache.model.SuperHero;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheWriterConfiguration;
import net.sf.ehcache.config.CacheConfiguration.CacheLoaderFactoryConfiguration;

@Repository("cacheDelegate")
//@DependsOn("hibernateTransactionManager")
public final class CacheDelegate {
	private static final Logger LOGGER = Logger.getLogger(CacheDelegate.class);
	private static final String CACHE_NAME = "herosCache";

	private CacheManager manager;
	private Ehcache cache;

	@Autowired
	@Qualifier("superHeroDaoImpl")
	private SuperHeroDAO superHeroDaoImpl;

	/**
	 * Default Constructor
	 */

	public CacheDelegate() {
		LOGGER.info("***Initializing Cache Delegate Class***");
		net.sf.ehcache.config.Configuration config = new net.sf.ehcache.config.Configuration();
		config.addCache(cacheConfig());
		manager = CacheManager.create(config);
		cache = manager.getCache(CACHE_NAME);
		SuperHeroCacheWriter writer = new SuperHeroCacheWriter(cache);
		cache.registerCacheWriter(writer);
		SuperHeroCacheLoader loader = new SuperHeroCacheLoader();
		cache.registerCacheLoader(loader);
	}

	@SuppressWarnings("deprecation")
	private CacheConfiguration cacheConfig() {
		CacheConfiguration config = new CacheConfiguration();
		config.setName("");
		config.setMaxEntriesLocalHeap(1000);
		config.setEternal(true);
		config.setOverflowToDisk(false);
		config.setMemoryStoreEvictionPolicy("LRU");
		config.cacheWriter(new CacheWriterConfiguration().writeMode(CacheWriterConfiguration.WriteMode.WRITE_BEHIND)
				.maxWriteDelay(3).rateLimitPerSecond(10).notifyListenersOnException(true).writeCoalescing(true)
				.writeBatching(true).writeBatchSize(2).retryAttempts(5).retryAttemptDelaySeconds(5)
				.cacheWriterFactory(new CacheWriterConfiguration.CacheWriterFactoryConfiguration()
						.className("com.sivavr.ehcache.cache.SuperHeroCacheWriterFactory")
						.properties("daoImpl=" + superHeroDaoImpl)));
		config.cacheLoaderFactory(
				new CacheLoaderFactoryConfiguration().className("com.sivavr.ehcache.cache.SuperHeroCacheLoaderFactory")
						.properties("type=int;startCounter=10").propertySeparator(";"));
		return config;
	}

	/**
	 * Method that removes a certain object (by key) from cache. Please note
	 * that this method will not log until the object gets evicted from data
	 * store.
	 * 
	 * @param key
	 *            The cache object key
	 */

	public void removeElementFormCache(Object key) {
		if (!cache.remove(key)) {
			throw new RuntimeException("Could Not remove key-value from Cache");
		}
	}

	/**
	 * Method that adds a new hero object in Cache
	 * 
	 * @param hero
	 *            model object instance
	 */
	public void addElementToCache(SuperHero hero) {
		cache.put(new Element(hero.getId(), hero));
	}

	/**
	 * Method that adds a new hero object in CacheWriter
	 * 
	 * @param hero
	 *            model object instance
	 */

	public void addElementToCacheWriter(SuperHero hero) {
		LOGGER.info("*** CacheDelegate.addElementToCacheWriter() ***");
		LOGGER.info("before::cache size = " + cache.getSize());
		// get key
		long key = superHeroDaoImpl.findAll().size() + 1;
		hero.setId(key);
		LOGGER.info("*** CacheDelegateputWithWriter >>> key:"+key+" >>> " + hero.getMovie() + "," + hero.getName() + " ***");
		// put caching
		cache.putWithWriter(new Element(hero.getId(), hero));
		LOGGER.info("after::cache size = " + cache.getSize());
	}

	/**
	 * Method that get a hero object in Cache
	 * 
	 * @param hero
	 *            model object instance
	 */
	@SuppressWarnings("unchecked")
	public List<SuperHero> getElementFromCache(Long key) {
		LOGGER.info("*** CacheDelegate.getElementFromCache() ***");
		LOGGER.info("cache size = " + cache.getSize());
		return (List<SuperHero>) cache.get(key).getObjectValue();
	}

	/**
	 * Method that get a hero object in CacheLoader
	 * 
	 * @param hero
	 *            model object instance
	 */

	public List<SuperHero> getElementFromCacheLoader(Long key) {
		LOGGER.info("*** CacheDelegate.getElementFromCacheLoader() key is:" + key + " ***");
		LOGGER.info("cache size = " + cache.getSize() + ",cache is:" + cache.toString() + cache);
		SuperHero hero = (SuperHero) cache.getWithLoader(key, null, null).getObjectValue();
		List<SuperHero> olist = new ArrayList<SuperHero>();
		olist.add(hero);
		return olist;
	}

	/**
	 * Method that removes all Elements in Cache
	 */
	public void removeAllElementsInCache() {
		cache.removeAll();
	}

	/**
	 * Find all the heros
	 * 
	 * @return The list
	 */
	public List<SuperHero> findAll() {
		LOGGER.info("*** CacheDelegate.findAll() ***");
		return superHeroDaoImpl.findAll();
	}

	/**
	 * Simulates an exception during a cache method execution. Invalid key.
	 */
	public void generateException() {
		cache.getCacheExceptionHandler().onException(cache, 100, new CacheException());
	}
}
