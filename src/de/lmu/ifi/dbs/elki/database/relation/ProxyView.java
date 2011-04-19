package de.lmu.ifi.dbs.elki.database.relation;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.result.AbstractHierarchicalResult;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;

/**
 * A virtual partitioning of the database. For the accepted DBIDs, access is
 * passed on to the wrapped representation.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public class ProxyView<O> extends AbstractHierarchicalResult implements Relation<O> {
  /**
   * Our database
   */
  private final Database database;

  /**
   * The DBIDs we contain
   */
  private final DBIDs idview;

  /**
   * The wrapped representation where we get the IDs from.
   */
  private final Relation<O> inner;

  /**
   * Constructor.
   * 
   * @param idview Ids to expose
   * @param inner Inner representation
   */
  public ProxyView(Database database, DBIDs idview, Relation<O> inner) {
    super();
    this.database = database;
    this.idview = DBIDUtil.makeUnmodifiable(idview);
    this.inner = inner;
  }

  @Override
  public Database getDatabase() {
    return database;
  }

  /**
   * Constructor-like static method.
   * 
   * @param <O> Object type
   * @param idview Ids to expose
   * @param inner Inner representation
   * @return Instance
   */
  public static <O> ProxyView<O> wrap(Database database, DBIDs idview, Relation<O> inner) {
    return new ProxyView<O>(database, idview, inner);
  }

  @Override
  public O get(DBID id) {
    assert (idview.contains(id));
    return inner.get(id);
  }

  @Override
  public void set(DBID id, O val) {
    assert (idview.contains(id));
    inner.set(id, val);
  }

  @Override
  public void delete(@SuppressWarnings("unused") DBID id) {
    throw new UnsupportedOperationException("Semantics of 'delete-from-virtual-partition' are not uniquely defined. Delete from IDs or from underlying data, please!");
  }

  @Override
  public DBIDs getDBIDs() {
    return idview;
  }

  @Override
  public IterableIterator<DBID> iterDBIDs() {
    return IterableUtil.fromIterable(idview);
  }

  @Override
  public int size() {
    return idview.size();
  }

  @Override
  public SimpleTypeInformation<O> getDataTypeInformation() {
    return inner.getDataTypeInformation();
  }

  @Override
  public String getLongName() {
    return "Partition of " + inner.getLongName();
  }

  @Override
  public String getShortName() {
    return "partition";
  }
}