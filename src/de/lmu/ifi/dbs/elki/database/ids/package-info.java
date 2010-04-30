/**
 * Database object identification and ID group handling.
 * 
 * Database IDs (short: DBID) in ELKI are based on the factory pattern, to allow replacing
 * the simple Integer-based DBIDs with more complex implementations, e.g. for use with external
 * databases or to add tracking for debugging purposes. This also allows adding of more efficient
 * implementations later on in a single place.
 * 
 * <h2>DBID interface:</h2>
 * 
 * The {@link DBID} object identifies a single object.
 * 
 * The {@link DBIDs} hierarchy contains classes for handling groups (sets, arrays) of IDs, that can
 * be seen as a two-dimensional matrix consisting 
 * <table style="border: 1px">
 * <tr>
 * <th></th>
 * <th style="border-bottom: 2px">{@link ArrayDBIDs}</th>
 * <th style="border-bottom: 2px">{@link HashSetDBIDs}</th>
 * <th style="border-bottom: 2px">{@link TreeSetDBIDs}</th>
 * </tr>
 * <tr>
 * <th style="border-right: 2px">{@link ModifiableDBIDs}</th>
 * <td>{@link ArrayModifiableDBIDs}</td>
 * <td>{@link HashSetModifiableDBIDs}</td>
 * <td>{@link TreeSetModifiableDBIDs}</td>
 * </tr>
 * <tr>
 * <th style="border-right: 2px">{@link StaticDBIDs}</th>
 * <td>{@link ArrayStaticDBIDs}</td>
 * <td>n/a</td>
 * <td>n/a</td>
 * </tr>
 * </table>
 * 
 * {@link StaticDBIDs} are structures that do not allow modifications,
 * but thus can be implemented more efficiently, for example as Interval. They are
 * mostly used by the data sources.
 * 
 * These interfaces cannot be instantiated, obviously. Instead, use the static
 * {@link DBIDFactory#FACTORY}, which is also wrapped in the {@link DBIDUtil} class.
 * 
 * <h2>Examples:</h2>
 * <pre>{@code
 * DBIDs allids = database.getIDs();
 * // preallocate an array of initial capacity 123 
 * ArrayModifiableDBIDs array = DBIDUtil.newArraySet(123);
 * // new DBID hash set with minimum initial capacity
 * ModifiableDBIDs hash = DBIDUtil.newHashSet();
 * // initialize a tree set with the IDs of the database.
 * ModifiableDBIDs tree = DBIDUtil.newTreeSet(database.getIDs());
 * 
 * // add all DBIDs from the hash
 * tree.addDBIDs(hash)
 * }</pre>
 * 
 * <h2>Generic utility classes:</h2>
 * <p>{@link de.lmu.ifi.dbs.elki.database.ids.generic.MergedDBIDs MergedDBIDs}
 * allows virtual concatenation of multiple DBIDs objects.</p>
 * 
 * <p>{@link de.lmu.ifi.dbs.elki.database.ids.generic.MaskedDBIDs MaskedDBIDs}
 * allows masking an ArrayDBIDs with a BitSet.</p>
 */
package de.lmu.ifi.dbs.elki.database.ids;