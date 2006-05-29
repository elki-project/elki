package de.lmu.ifi.dbs.algorithm.outlier;

import de.lmu.ifi.dbs.index.btree.BTree;
import de.lmu.ifi.dbs.index.btree.DefaultKey;
import de.lmu.ifi.dbs.utilities.output.ObjectPrinter;

import java.io.*;
import java.util.logging.Logger;

/**
 * Holds the lof values of the lof algorithm in a B-Tree structure.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class LOFTable {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"UNUSED_SYMBOL"})
//  private static final boolean DEBUG = LoggingConfiguration.DEBUG;
  private static final boolean DEBUG = true;

  /**
   * The logger of this class.
   */
  @SuppressWarnings({"UNUSED_SYMBOL"})
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * The BTree containing the lof values.
   */
  private BTree<DefaultKey, LOFEntry> lof;

  /**
   * Creates a new LOFTable with the specified parameters.
   *
   * @param pageSize  the size of a page in Bytes
   * @param cacheSize the size of the cache in Bytes
   * @param minpts    number of nearest neighbors of an object to be considered for computing its LOF
   */
  public LOFTable(int pageSize, int cacheSize, int minpts) {
    int keySize = 4;
    int valueSize = 8 + minpts * 8;
    this.lof = new BTree<DefaultKey, LOFEntry>(keySize, valueSize, pageSize, cacheSize);
//    this.lof = new BTree<DefaultKey, LOFEntry>(keySize, valueSize, pageSize, cacheSize, "lofelki.txt");
  }

  public LOFTable(String fileName, int pageSize, int cacheSize, int minpts) throws IOException {
    this(pageSize, cacheSize, minpts);

//    InputStream in = in = new FileInputStream(fileName);
//    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
//    int lineNumber = 0;
//    for (String line; (line = reader.readLine()) != null; lineNumber++) {
//      if (lineNumber >= 2) {
//        LOFEntry lofEntry = new LOFEntry();
//        lofEntry.readExternal(in);
//      }
//    }

  }

  /**
   * Inserts the lof value of the object with the specified id into this table.
   *
   * @param id    the object id
   * @param entry the lof value
   */
  public void insert(Integer id, LOFEntry entry) {
    lof.insert(new DefaultKey(id), entry);
  }

  /**
   * Returns the lof value of the object with the specified id.
   *
   * @param id the object id
   * @return the lof value of the object with the specified id
   */
  public LOFEntry getLOFEntry(Integer id) {
    LOFEntry e = lof.search(new DefaultKey(id));
    if (e != null) return e;
    System.out.println(lof.printStructure());
    return null;
  }

  /**
   * Returns the lof value of the object with the specified id
   * for update.
   *
   * @param id the object id
   * @return the lof value of the object with the specified id
   */
  public LOFEntry getLOFEntryForUpdate(Integer id) {
    return lof.searchForUpdate(new DefaultKey(id));
  }

  /**
   * Writes this table to the specified stream.
   *
   * @param outStream the stream to write into
   */
  public void write(PrintStream outStream) {
    outStream.println("################################################################################");
    outStream.println("### object-ID sum1 sum2_1 ... sum2_k");
    outStream.println("################################################################################");

//    ObjectPrinter printer = new ObjectPrinter() {
//      public String getPrintData(Object o) {
//        return o.toString();
//      }
//    };
//
//    lof.writeData(outStream, printer);
  }

  /**
   * Returns the physical read access of this table.
   */
  public long getPhysicalReadAccess() {
    return lof.getPhysicalReadAccess();
  }

  /**
   * Returns the physical write access of this table.
   */
  public long getPhysicalWriteAccess() {
    return lof.getPhysicalWriteAccess();
  }

  /**
   * Returns the logical read access of this table.
   */
  public long getLogicalPageAccess() {
    return lof.getLogicalPageAccess();
  }

  private class LOFEntryPrinter implements ObjectPrinter {
    /**
     * Get the object's print data.
     *
     * @param o the object to be printed
     * @return result  a string containing the ouput
     */
    public String getPrintData(Object o) {
      LOFEntry lofEntry = (LOFEntry) o;
      StringBuffer result = new StringBuffer();
      result.append(lofEntry.getSum1());
      int n = lofEntry.getSum2Array().length;
      for (int i = 0; i < n; i++) {
        if (i < n - 1)
          result.append(" ").append(lofEntry.getSum2(i));
      }
      return result.toString();
    }

    /**
     * Restores the object which is specified by the given String.
     *
     * @param s the string that specifies the object to be restored
     * @return the restored object
     */
    public Object restoreObject(String s) {

      return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
  }

}
