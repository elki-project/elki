package de.lmu.ifi.dbs.elki.data.model;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;

/**
 * This code was factored out of the Bicluster class, since not all biclusters have inverted rows.
 * TODO: shouldn't that be inverted columns?
 * 
 * @author Erich Schubert
 *
 * @param <V>
 */
public class BiclusterWithInverted<V extends RealVector<V, Double>> extends Bicluster<V> {
  /**
   * The ids of inverted rows.
   */
  private int[] invertedRows;

  public BiclusterWithInverted(int[] rowIDs, int[] colIDs, Database<V> database) {
    super(rowIDs, colIDs, database);
  }

  /**
   * Sets the ids of the inverted rows.
   *
   * @param invertedRows the ids of the inverted rows
   */
  public void setInvertedRows(int[] invertedRows) {
      this.invertedRows = new int[invertedRows.length];
      System.arraycopy(invertedRows, 0, this.invertedRows, 0, invertedRows.length);
  }

  /**
   * Provides a copy of the inverted column IDs.
   * 
   * @return a copy of the inverted column IDs.
   */
  public int[] getInvertedRows() {
    int[] columnIDs = new int[invertedRows.length];
    System.arraycopy(invertedRows, 0, columnIDs, 0, invertedRows.length);
    return columnIDs;
  }


  /**
   * Sorts the row and column ids (and - if applicable - the ids of inverted rows)
   * in ascending order.
   */
  @Override
  public void sortIDs() {
      super.sortIDs();
      if (this.invertedRows != null) {
          Arrays.sort(this.invertedRows);
      }
  }

  /**
   * Implementation of {@link TextWriteable} interface.
   */
  @Override
  public void writeToText(TextWriterStream out, String label) {
    super.writeToText(out, label);
    if (this.invertedRows != null) {
      out.commentPrintLn("inverted rows (row IDs): " + FormatUtil.format(this.invertedRows));
    }      
  }
}
