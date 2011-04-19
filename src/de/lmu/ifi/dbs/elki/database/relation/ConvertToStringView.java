package de.lmu.ifi.dbs.elki.database.relation;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.result.AbstractHierarchicalResult;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;

/**
 * Representation adapter that uses toString() to produce a string
 * representation.
 * 
 * @author Erich Schubert
 */
public class ConvertToStringView extends AbstractHierarchicalResult implements Relation<String> {
  /**
   * The database we use
   */
  final Relation<?> existing;

  /**
   * Constructor.
   * 
   * @param existing Existing representation
   */
  public ConvertToStringView(Relation<?> existing) {
    super();
    this.existing = existing;
  }

  @Override
  public Database getDatabase() {
    return existing.getDatabase();
  }

  @Override
  public String get(DBID id) {
    return existing.get(id).toString();
  }

  @SuppressWarnings("unused")
  @Override
  public void set(DBID id, String val) {
    throw new UnsupportedOperationException("Covnersion representations are not writable!");
  }

  @SuppressWarnings("unused")
  @Override
  public void delete(DBID id) {
    throw new UnsupportedOperationException("Covnersion representations are not writable!");
  }

  @Override
  public DBIDs getDBIDs() {
    return existing.getDBIDs();
  }

  @Override
  public IterableIterator<DBID> iterDBIDs() {
    return existing.iterDBIDs();
  }

  @Override
  public int size() {
    return existing.size();
  }

  @Override
  public SimpleTypeInformation<String> getDataTypeInformation() {
    return TypeUtil.STRING;
  }

  @Override
  public String getLongName() {
    return "toString(" + existing.getLongName() + ")";
  }

  @Override
  public String getShortName() {
    return "tostring-" + existing.getShortName();
  }
}