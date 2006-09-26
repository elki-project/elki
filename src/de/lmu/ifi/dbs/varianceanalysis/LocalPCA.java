package de.lmu.ifi.dbs.varianceanalysis;

import java.util.List;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.GreaterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.GreaterEqualConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.LessGlobalConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * LocalPCA is a super calss for PCA-algorithms considering only a local neighborhood.
 * LocalPCA provides some methods valid for any extending class.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class LocalPCA extends AbstractPCA {

  /**
   * The default value for the big value.
   */
  public static final double DEFAULT_BIG_VALUE = 1;

  /**
   * Option string for parameter big value.
   */
  public static final String BIG_VALUE_P = "big";

  /**
   * Description for parameter big value.
   */
  public static final String BIG_VALUE_D = "a constant big value (> 0, big > small) to reset high eigenvalues "
                                           + "(default: " + DEFAULT_BIG_VALUE + "). ";

  /**
   * The default value for the small value.
   */
  public static final double DEFAULT_SMALL_VALUE = 0;

  /**
   * Option string for parameter small value.
   */
  public static final String SMALL_VALUE_P = "small";

  /**
   * Description for parameter small value.
   */
  public static final String SMALL_VALUE_D = "a constant small value (>= 0, small < big) to reset low eigenvalues "
                                             + "(default: " + DEFAULT_SMALL_VALUE + ").";

  /**
   * Holds the big value.
   */
  private double big;

  /**
   * Holds the small value.
   */
  private double small;

  /**
   * The correlation dimension (i.e. the number of strong eigenvectors) of the
   * object to which this PCA belongs to.
   */
  private int correlationDimension = 0;

  /**
   * The selection matrix of the weak eigenvectors.
   */
  private Matrix e_hat;

  /**
   * The selection matrix of the strong eigenvectors.
   */
  private Matrix e_czech;

  /**
   * The similarity matrix.
   */
  private Matrix m_hat;

  /**
   * The dissimilarity matrix.
   */
  private Matrix m_czech;

  /**
   * The diagonal matrix of adapted strong eigenvalues: eigenvectors * e_czech.
   */
  private Matrix adapatedStrongEigenvalues;

  /**
   * Adds parameter for big and small value to parameter map.
   */
  public LocalPCA() {
    super();
    
    // parameter big value
    DoubleParameter big = new DoubleParameter(BIG_VALUE_P, BIG_VALUE_D, new GreaterConstraint(0));
    big.setDefaultValue(DEFAULT_BIG_VALUE);
    optionHandler.put(BIG_VALUE_P, big);
    
    // parameter small value
    DoubleParameter small = new DoubleParameter(SMALL_VALUE_P, SMALL_VALUE_D, new GreaterEqualConstraint(0));
    small.setDefaultValue(DEFAULT_SMALL_VALUE);
    optionHandler.put(SMALL_VALUE_P, small);
    
    // global constraint
    optionHandler.setGlobalParameterConstraint(new LessGlobalConstraint(small,big));
  }

  /**
   * Performs a LocalPCA for the object with the specified ids stored in the given
   * database.
   *
   * @param ids      the ids of the objects for which the PCA should be performed
   * @param database the database containing the objects
   */
  public final void run(List<Integer> ids, Database<RealVector> database) {
    // logging
    StringBuffer msg = new StringBuffer();
    if (this.debug) {
      RealVector o = database.get(ids.get(0));
      String label = (String) database.getAssociation(AssociationID.LABEL, ids.get(0));
      msg.append("\nobject ").append(o).append(" ").append(label);
    }

    // sorted eigenpairs, eigenvectors, eigenvalues
    Matrix pcaMatrix = pcaMatrix(database, ids);
    determineEigenPairs(pcaMatrix);

    // correlationDimension = #strong EV
    correlationDimension = getStrongEigenvalues().length;
    int dim = getEigenvalues().length;

    // selection Matrix for weak and strong EVs
    e_hat = new Matrix(dim, dim);
    e_czech = new Matrix(dim, dim);
    for (int d = 0; d < dim; d++) {
      if (d < correlationDimension) {
        e_czech.set(d, d, big);
        e_hat.set(d, d, small);
      }
      else {
        e_czech.set(d, d, small);
        e_hat.set(d, d, big);
      }
    }

    Matrix V = getEigenvectors();
    adapatedStrongEigenvalues = V.times(e_czech);
    m_hat = V.times(e_hat).times(V.transpose());
    m_czech = V.times(e_czech).times(V.transpose());

    if (this.debug) {
      msg.append("\n ids =");
      for (Integer id : ids) {
        msg.append(database.getAssociation(AssociationID.LABEL, id) + ", ");
      }

      msg.append("\n  E = ");
      msg.append(Util.format(getEigenvalues(), ",", 6));

      msg.append("\n  V = ");
      msg.append(V);

      msg.append("\n  E_hat = ");
      msg.append(e_hat);

      msg.append("\n  E_czech = ");
      msg.append(e_czech);

      msg.append("\n  corrDim = ");
      msg.append(correlationDimension);

      debugFine(msg.toString() + "\n");
    }
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // big value
    if (optionHandler.isSet(BIG_VALUE_P)) {
      String bigValueString = optionHandler.getOptionValue(BIG_VALUE_P);
      try {
        big = Double.parseDouble(bigValueString);
        if (big <= 0)
          throw new WrongParameterValueException(BIG_VALUE_P, bigValueString, BIG_VALUE_D);
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(BIG_VALUE_P, bigValueString, BIG_VALUE_D, e);
      }
    }
    else {
      big = DEFAULT_BIG_VALUE;
    }

    // small value
    if (optionHandler.isSet(SMALL_VALUE_P)) {
      String smallValueString = optionHandler.getOptionValue(SMALL_VALUE_P);
      try {
        small = Double.parseDouble(smallValueString);
        if (small < 0)
          throw new WrongParameterValueException(SMALL_VALUE_P, smallValueString, SMALL_VALUE_D);
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(SMALL_VALUE_P, smallValueString, SMALL_VALUE_D, e);
      }
    }
    else {
      small = DEFAULT_SMALL_VALUE;
    }

    if (big <= small) {
      throw new WrongParameterValueException("big value has to be greater than small value" +
                                             "(big = " + big + " <= " + small + " = small)");
    }

    return remainingParameters;
  }

  /**
   * Returns the parameter setting of this PCA.
   *
   * @return the parameter setting of this PCA
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();

    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(BIG_VALUE_P, Double.toString(big));
    mySettings.addSetting(SMALL_VALUE_P, Double.toString(small));

    return attributeSettings;
  }

  /**
   * Returns the correlation dimension (i.e. the number of strong
   * eigenvectors) of the object to which this PCA belongs to.
   *
   * @return the correlation dimension
   */
  public int getCorrelationDimension() {
    return correlationDimension;
  }

  /**
   * Returns a copy of the selection matrix of the weak eigenvectors (E_hat)
   * of the object to which this PCA belongs to.
   *
   * @return the selection matrix of the weak eigenvectors E_hat
   */
  public Matrix selectionMatrixOfWeakEigenvectors() {
    return e_hat.copy();
  }

  /**
   * Returns a copy of the selection matrix of the strong eigenvectors
   * (E_czech) of this LocalPCA.
   *
   * @return the selection matrix of the weak eigenvectors E_czech
   */
  public Matrix selectionMatrixOfStrongEigenvectors() {
    return e_czech.copy();
  }

  /**
   * Returns a copy of the similarity matrix (M_hat) of this LocalPCA.
   *
   * @return the similarity matrix M_hat
   */
  public Matrix similarityMatrix() {
    return m_hat.copy();
  }

  /**
   * Returns a copy of the dissimilarity matrix (M_czech) of this LocalPCA.
   *
   * @return the dissimilarity matrix M_hat
   */
  public Matrix dissimilarityMatrix() {
    return m_czech.copy();
  }

  /**
   * Returns a copy of the adapted strong eigenvalues.
   *
   * @return the adapted strong eigenvalues
   */
  public Matrix adapatedStrongEigenvalues() {
    return adapatedStrongEigenvalues.copy();
  }

  /**
   * Determines and returns the matrix that is used for
   * performaing the pca.
   *
   * @param database the database holding the objects
   * @param ids      the list of the object ids for which the matrix
   *                 should be determined
   * @return he matrix that is used for performaing a pca
   */
  protected abstract Matrix pcaMatrix(Database<RealVector> database, List<Integer> ids);
}
