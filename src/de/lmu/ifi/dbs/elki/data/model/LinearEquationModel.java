package de.lmu.ifi.dbs.elki.data.model;

import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Cluster model containing a linear equation system for the cluster.
 * 
 * @author Erich Schubert
 *
 */
public class LinearEquationModel extends BaseModel implements TextWriteable {
  /**
   * equation system
   */
  private LinearEquationSystem les;

  /**
   * Constructor
   * @param les
   */
  public LinearEquationModel(LinearEquationSystem les) {
    super();
    this.les = les;
  }

  /**
   * Accessor.
   * 
   * @return
   */
  public LinearEquationSystem getLes() {
    return les;
  }

  /**
   * Accessor.
   * 
   * @param les
   */
  public void setLes(LinearEquationSystem les) {
    this.les = les;
  }
  
  /**
   * Implementation of {@link TextWriteable} interface
   */
  @Override
  public void writeToText(TextWriterStream out, String label) {
    super.writeToText(out, label);
    out.commentPrintLn(les.equationsToString(6));
  }

}
