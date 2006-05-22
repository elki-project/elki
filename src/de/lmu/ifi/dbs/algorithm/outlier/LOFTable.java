package de.lmu.ifi.dbs.algorithm.outlier;

import de.lmu.ifi.dbs.index.btree.BTree;
import de.lmu.ifi.dbs.utilities.output.ObjectPrinter;

import java.io.PrintStream;
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
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * The BTree containing the lof values.
   */
  private BTree<Integer, LOFEntry> lof;

  /**
   * Creates a new LOFTable with the specified parameters.
   *
   * @param pageSize  the size of a page in Bytes
   * @param cacheSize the size of the cache in Bytes
   * @param minpts    number of nearest neighbors of an object to be considered for computing its LOF
   */
  public LOFTable(int pageSize, int cacheSize, int minpts) {
    double m = ((double) pageSize - 16) / (4 + minpts * minpts * 8);
    if (DEBUG) {
      logger.fine("m = " + m);
    }

    this.lof = new BTree<Integer, LOFEntry>((int) m, pageSize, cacheSize);
  }

  /**
   * Inserts the lof value of the object with the specified id into this table.
   *
   * @param id    the object id
   * @param entry the lof value
   */
  public void insert(Integer id, LOFEntry entry) {
    lof.insert(id, entry);
  }

  /**
   * Returns the lof value of the object with the specified id.
   *
   * @param id the object id
   * @return the lof value of the object with the specified id
   */
  public LOFEntry getLOFEntry(Integer id) {
    return lof.search(id);
  }

  /**
   * Returns the lof value of the object with the specified id
   * for update.
   *
   * @param id the object id
   * @return the lof value of the object with the specified id
   */
  public LOFEntry getLOFEntryForUpdate(Integer id) {
    return lof.searchForUpdate(id);
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

    ObjectPrinter printer = new ObjectPrinter() {
      public String getPrintData(Object o) {
        return o.toString();
      }
    };

    lof.writeData(outStream, printer);
  }
}
