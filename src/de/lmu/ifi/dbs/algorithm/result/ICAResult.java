package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.varianceanalysis.ica.FastICA;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;

import java.io.PrintStream;
import java.util.List;

/**
 * TODO: comment
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ICAResult extends AbstractResult<RealVector> {
  /**
   * The independent component analysis.
   */
  private FastICA ica;

  /**
   * todo
   * @param db
   */
  public ICAResult(Database<RealVector> db, FastICA ica) {
    super(db);
    this.ica = ica;
  }

  /**
   * Writes the clustering result to the given stream.
   * todo
   *
   * @param outStream     the stream to write to
   * @param normalization Normalization to restore original values according to, if this action is supported
   *                      - may remain null.
   * @param settings      the settings to be written into the header, if this parameter is <code>null</code>,
   *                      no header will be written
   * @throws de.lmu.ifi.dbs.utilities.UnableToComplyException
   *          if any feature vector is not compatible with values initialized during normalization
   */
  public void output(PrintStream outStream, Normalization normalization, List settings) throws UnableToComplyException {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  public String toString() {
    StringBuffer msg = new StringBuffer();

    Matrix mix = ica.getMixingMatrix().copy();
    mix.normalizeColumns();

     Matrix sep = ica.getSeparatingMatrix().copy();
    sep.normalizeColumns();


    msg.append("\nMixing matrix\n" +  ica.getMixingMatrix());
    msg.append("\nSeparating matrix\n" + ica.getSeparatingMatrix());
    msg.append("\nWeight matrix\n" + ica.getWeightMatrix());
     msg.append("\nNormalo Mixing matrix\n" + mix);
     msg.append("\nNormalo Sep matrix\n" + sep);

    return msg.toString();
  }
}
