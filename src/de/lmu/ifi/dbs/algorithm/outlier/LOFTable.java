package de.lmu.ifi.dbs.algorithm.outlier;

import de.lmu.ifi.dbs.index.btree.BTree;
import de.lmu.ifi.dbs.index.btree.BTreeData;
import de.lmu.ifi.dbs.index.btree.DefaultKey;
import de.lmu.ifi.dbs.utilities.output.ObjectPrinter;

import java.io.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Holds the lof values of the lof algorithm in a B-Tree structure.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class LOFTable {
  /**
   * The printer for output.
   */
  private static ObjectPrinter<BTreeData<DefaultKey, LOFEntry>> printer = new LOFEntryPrinter();

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

  /**
   * Creates a new LOFTable from a given file with the specified parameters.
   *
   * @param fileName  the name of the file containing the entries
   * @param pageSize  the size of a page in Bytes
   * @param cacheSize the size of the cache in Bytes
   * @param minpts    number of nearest neighbors of an object to be considered for computing its LOF
   */
  public LOFTable(String fileName, int pageSize, int cacheSize, int minpts) throws IOException {
    this(pageSize, cacheSize, minpts);

    InputStream in = new FileInputStream(fileName);
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 0;
    for (String line; (line = reader.readLine()) != null; lineNumber++) {
      if (lineNumber >= 2) {
        BTreeData<DefaultKey, LOFEntry> data = printer.restoreObject(line);
        lof.insert(data);
      }
    }

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

    lof.writeData(outStream, printer);
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

  private static class LOFEntryPrinter implements ObjectPrinter<BTreeData<DefaultKey, LOFEntry>> {
    Pattern split = Pattern.compile(" ");

    /**
     * Get the object's print data.
     *
     * @param o the object to be printed
     * @return result  a string containing the ouput
     */
    public String getPrintData(BTreeData<DefaultKey, LOFEntry> o) {
      // object-ID sum1 sum2_1 ... sum2_k
      StringBuffer result = new StringBuffer();
      result.append(o.getKey().value());
      result.append(" ");

      LOFEntry lofEntry = o.getValue();
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
    public BTreeData<DefaultKey, LOFEntry> restoreObject(String s) {
      String[] parameters = split.split(s);

      DefaultKey key = new DefaultKey(Integer.parseInt(parameters[0]));

      double sum1 = Double.parseDouble(parameters[1]);

      double[] sum2Array = new double[parameters.length - 2];
      for (int i = 2; i < parameters.length; i++) {
        sum2Array[i - 2] = Double.parseDouble(parameters[i]);
      }
      LOFEntry lofEntry = new LOFEntry(sum1, sum2Array);

      return new BTreeData<DefaultKey, LOFEntry>(key, lofEntry);
    }
  }

}
