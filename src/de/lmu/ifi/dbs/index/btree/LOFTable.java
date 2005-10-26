package de.lmu.ifi.dbs.index.btree;

import java.io.Serializable;

/**
 * TODO: comment
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class LOFTable extends BTree {

  /**
   * Creates a new LOFTable with the specified parameters.
   * The LOFTable will be hold in main memory.
   *
   * @param m         the order of the BTree
   * @param pageSize  the size of a page in Bytes
   * @param cacheSize the size of the cache in Bytes
   */
  public LOFTable(int m, int pageSize, int cacheSize) {
    super(m, pageSize, cacheSize);
  }

  /**
   * Creates a LOFTable with the specified parameters from an existing persistent file.
   *
   * @param cacheSize the size of the cache in Bytes
   * @param fileName  the name of the file storing this BTree.
   */
  public LOFTable(int cacheSize, String fileName) {
    super(cacheSize, fileName);
  }

  /**
   * Creates a new LOFTable with the specified parameters.
   * The LOFTable will be saved persistently in the specified file.
   *
   * @param m         the order of the BTree
   * @param pageSize  the size of a page in Bytes
   * @param cacheSize the size of the cache in Bytes
   * @param fileName  the name of the file storing this BTree.
   */
//  public LOFTable(int m, int pageSize, int cacheSize, String fileName) {
//    super(m, pageSize, cacheSize, fileName);
//  }

//  public void insert(LOFTableEntry entry) {
//    Integer key = new Integer(entry.getObject().getObjectID());
//    insert(key, entry, true);
//  }

//  public void delete(DBObject key) {
//    lof.remove(new Integer(key.getObjectID()));
//  }

//  public LOFTableEntry getLOF(DBObject key) {
//    return (LOFTableEntry) lof.find(new Integer(key.getObjectID()));
//  }
}
