/**
 * General data store layer (along the lines of {@code Map<DBID, T>} - use everywhere!) for ELKI.
 * 
 * <h2>When to use:</h2>
 * 
 * <p>Every time you need to associate a larger number of objects (in form of
 * {@link de.lmu.ifi.dbs.elki.database.ids.DBID DBID}s) with any kind of value.
 * This can be temporary values such as KNN lists, but also result values such as
 * outlier scores.</p>
 * 
 * <p>Basically, {@code Storage<T> == Map<DBID, T>}. But this API will allow extensions that
 * can do on-disk maps.</p>
 * 
 * <h2>How to use:</h2>
 * <pre>{@code
 * // Storage for the outlier score of each ID. 
 * final WritableStorage<Double> scores = StorageFactory.FACTORY.makeStorage(ids, StorageFactory.HINT_STATIC, Double.class);
 * }</pre>
 */
package de.lmu.ifi.dbs.elki.database.datastore;