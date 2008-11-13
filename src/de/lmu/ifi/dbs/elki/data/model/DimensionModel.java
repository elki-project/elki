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
   * Accessor
   * @return
   */
  public int getDimension() {
    return dimension;
  }

  /**
   * Accessor
   * @param dimension
   */
  public void setDimension(int dimension) {
    this.dimension = dimension;
  }
  
  /**
   * Implementation of {@link TextWriteable} interface
   */
  @Override
  public void writeToText(TextWriterStream out) {
    super.writeToText(out);
    out.commentPrintLn("Dimension: "+dimension);
  }
  
  /**
   * Implementation of {@link Model} interface.
   */
  @Override
  public String getSuggestedLabel() {    
    return "Cluster dim("+dimension+")";
  }

}
