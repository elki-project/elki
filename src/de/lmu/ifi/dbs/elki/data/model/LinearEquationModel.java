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
   * Equation system
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
   * Get assigned Linear Equation System
   * 
   * @return linear equation system
   */
  public LinearEquationSystem getLes() {
    return les;
  }

  /**
   * Assign new Linear Equation System.
   * 
   * @param les new linear equation system
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
