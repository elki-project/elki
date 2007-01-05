package de.lmu.ifi.dbs.distance.distancefunction;

import static de.lmu.ifi.dbs.distance.distancefunction.ERiCDistanceFunction.DEFAULT_DELTA;
import static de.lmu.ifi.dbs.distance.distancefunction.ERiCDistanceFunction.DELTA_D;
import static de.lmu.ifi.dbs.distance.distancefunction.ERiCDistanceFunction.DELTA_P;

import de.lmu.ifi.dbs.data.Bit;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.distance.BitDistance;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.GreaterEqualConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.varianceanalysis.LocalPCA;

import java.util.List;

/**
 * Provides a distance function for the ERiC algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ERiCDistanceFunction<O extends RealVector> extends AbstractDistanceFunction<O, BitDistance> {
  /**
   * The default value for delta.
   */
  public static final double DEFAULT_DELTA = 0.25;

  /**
   * Option string for parameter delta.
   */
  public static final String DELTA_P = "delta";

  /**
   * Description for parameter delta.
   */
  public static final String DELTA_D = "a double specifying the threshold of a distance between a vector q and a given space that indicates that q adds a new dimension to the space (default is delta = " + DEFAULT_DELTA + ")";

  /**
   * The threshold of a distance between a vector q and a given space that
   * indicates that q adds a new dimension to the space.
   */
  private double delta;

  /**
   * Provides a distance function for the ERiC algorithm.
   */
  public ERiCDistanceFunction() {
    super(Bit.BIT_PATTERN);
    DoubleParameter delta = new DoubleParameter(DELTA_P, DELTA_D, new GreaterEqualConstraint(0));
    delta.setDefaultValue(DEFAULT_DELTA);
    optionHandler.put(DELTA_P, delta);
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // delta
    delta = (Double) optionHandler.getOptionValue(DELTA_P);

    return remainingParameters;
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = super.getAttributeSettings();
    AttributeSettings attributeSettings = result.get(0);
    attributeSettings.addSetting(DELTA_P, Double.toString(delta));
    return result;
  }

  /**
   * @see de.lmu.ifi.dbs.distance.MeasurementFunction#valueOf(String)
   */
  public BitDistance valueOf(String pattern) throws IllegalArgumentException {
    if (pattern.equals(INFINITY_PATTERN)) {
      return infiniteDistance();
    }
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  /**
   * @see de.lmu.ifi.dbs.distance.MeasurementFunction#infiniteDistance()
   */
  public BitDistance infiniteDistance() {
    throw new UnsupportedOperationException("Infinite distance not supported!");
  }

  /**
   * @see de.lmu.ifi.dbs.distance.MeasurementFunction#nullDistance()
   */
  public BitDistance nullDistance() {
    return new BitDistance(false);
  }

  /**
   * @see de.lmu.ifi.dbs.distance.MeasurementFunction#undefinedDistance()
   */
  public BitDistance undefinedDistance() {
    throw new UnsupportedOperationException("Undefinded distance not supported!");
  }

  /**
   * @see DistanceFunction#distance(de.lmu.ifi.dbs.data.DatabaseObject, de.lmu.ifi.dbs.data.DatabaseObject)
   */
  public BitDistance distance(O o1, O o2) {
    LocalPCA pca1 = (LocalPCA) getDatabase().getAssociation(AssociationID.LOCAL_PCA, o1.getID());
    LocalPCA pca2 = (LocalPCA) getDatabase().getAssociation(AssociationID.LOCAL_PCA, o2.getID());

    if (!equalsSoft(pca1, pca2)) {
      return new BitDistance(true);
    }
    else {
      return new BitDistance(false);
    }
  }

  /**
   * Returns true, if the strong eigenvectors of the two specified
   * pcas span up the same space.
   *
   * @param pca1 first PCA
   * @param pca2 second PCA
   * @return true, if the strong eigenvectors of the two specified
   *         pcas span up the same space
   */
  private boolean equalsSoft(LocalPCA pca1, LocalPCA pca2) {
    Matrix m1_czech = pca1.dissimilarityMatrix();
    Matrix v2_strong = pca2.adapatedStrongEigenvalues();
    for (int i = 0; i < v2_strong.getColumnDimensionality(); i++) {
      Matrix v2_i = v2_strong.getColumn(i);
      // check, if distance of v2_i to the space of pca_1 > delta
      // (i.e., if v2_i spans up a new dimension)
      double dist = Math.sqrt(v2_i.transpose().times(v2_i).get(0, 0) - v2_i.transpose().times(m1_czech).times(v2_i).get(0, 0));

      // if so, return false
      if (dist > delta) {
        return false;
      }
    }

    return true;
  }
}
