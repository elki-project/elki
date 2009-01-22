package de.lmu.ifi.dbs.elki.data.model;

import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Cluster model just providing a cluster dimensionality.
 * 
 * @author Erich Schubert
 *
 */
public class DimensionModel extends BaseModel implements TextWriteable {
  /**
   * Number of dimensions
   */
  private int dimension;

  /**
   * Constructor
   * @param dimension number of dimensions
   */
  public DimensionModel(Integer dimension) {
    super();
    this.dimension = dimension;
  }

  /**
   * Get cluster dimensionality
   * 
   * @return dimensionality
   */
  public int getDimension() {
    return dimension;
  }

  /**
   * Set cluster dimensionality
   *  
   * @param dimension new dimensionality
   */
  public void setDimension(int dimension) {
    this.dimension = dimension;
  }
  
  /**
   * Implementation of {@link TextWriteable} interface
   */
  @Override
  public void writeToText(TextWriterStream out, String label) {
    super.writeToText(out, label);
    out.commentPrintLn("Dimension: "+dimension);
  }
  
}
