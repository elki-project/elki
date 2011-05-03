package de.lmu.ifi.dbs.elki.data.model;

import java.util.Collections;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * This code was factored out of the Bicluster class, since not all biclusters
 * have inverted rows. TODO: shouldn't that be inverted columns?
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
public class BiclusterWithInverted<V extends FeatureVector<V, ?>> extends Bicluster<V> {
  /**
   * The ids of inverted rows.
   */
  private ArrayModifiableDBIDs invertedRows;

  /**
   * @param rowIDs Row IDs
   * @param colIDs Col IDs
   * @param database Database
   */
  public BiclusterWithInverted(ArrayDBIDs rowIDs, int[] colIDs, Relation<V> database) {
    super(rowIDs, colIDs, database);
  }

  /**
   * @param rowIDs Row IDs
   * @param colIDs Col IDs
   * @param database Database
   * 
   * @deprecated Use DBIDs, not integer indexes!
   */
  @SuppressWarnings("deprecation")
  @Deprecated
  public BiclusterWithInverted(int[] rowIDs, int[] colIDs, Relation<V> database) {
    super(rowIDs, colIDs, database);
  }

  /**
   * Sets the ids of the inverted rows.
   * 
   * @param invertedRows the ids of the inverted rows
   */
  public void setInvertedRows(DBIDs invertedRows) {
    this.invertedRows = DBIDUtil.newArray(invertedRows);
  }

  /**
   * Provides a copy of the inverted column IDs.
   * 
   * @return a copy of the inverted column IDs.
   */
  public DBIDs getInvertedRows() {
    return DBIDUtil.makeUnmodifiable(invertedRows);
  }

  /**
   * Sorts the row and column ids (and - if applicable - the ids of inverted
   * rows) in ascending order.
   */
  @Override
  public void sortIDs() {
    super.sortIDs();
    if(this.invertedRows != null) {
      Collections.sort(this.invertedRows);
    }
  }

  /**
   * Implementation of {@link TextWriteable} interface.
   */
  @Override
  public void writeToText(TextWriterStream out, String label) {
    super.writeToText(out, label);
    if(this.invertedRows != null) {
      out.commentPrintLn("inverted rows (row IDs): " + this.invertedRows.toString());
    }
  }
}