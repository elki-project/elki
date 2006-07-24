package de.lmu.ifi.dbs.varianceanalysis;

import java.util.List;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * LocalPCA provides some methods valid for any extending class.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class LocalPCA extends AbstractParameterizable implements PCA {

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
  public static final String BIG_VALUE_D = "a constant big value (> 0, big > small) to reset eigenvalues "
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
  public static final String SMALL_VALUE_D = "a constant small value (>= 0, small < big) to reset eigenvalues "
                                             + "(default: " + DEFAULT_SMALL_VALUE + ").";

  /**
   * The default value for parameter eigenpair filter.
   */
  public static final String DEFAULT_EIGENPAIR_FILTER = PercentageEigenPairFilter.class.getName();

  /**
   * Parameter for eigenpair filter.
   */
  public static final String EIGENPAIR_FILTER_P = "filter";

  /**
   * Description for parameter eigenpair filter.
   */
  public static final String EIGENPAIR_FILTER_D = "the filter to determine the strong and weak eigenvectors " +
                                                  Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(EigenPairFilter.class) +
                                                  ". Default: " + DEFAULT_EIGENPAIR_FILTER;


  /**
   * Holds the big value.
   */
  double big;

  /**
   * Holds the small value.
   */
  double small;

  /**
   * The eigenpair filter to determine the strong and weak eigenvectors.
   */
  EigenPairFilter eigenPairFilter;

  /**
   * The correlation dimension (i.e. the number of strong eigenvectors) of the
   * object to which this PCA belongs to.
   */
  int correlationDimension = 0;

  /**
   * The eigenvalues in decreasing order.
   */
  double[] eigenvalues;

  /**
   * The eigenvectors in decreasing order to their corresponding eigenvalues.
   */
  Matrix eigenvectors;

  /**
   * The strong eigenvectors.
   */
  Matrix strongEigenvectors;

  /**
   * The selection matrix of the weak eigenvectors.
   */
  Matrix e_hat;

  /**
   * The selection matrix of the strong eigenvectors.
   */
  Matrix e_czech;

  /**
   * The similarity matrix.
   */
  Matrix m_hat;

  /**
   * The dissimilarity matrix.
   */
  Matrix m_czech;

  /**
   * Adds parameter for big and small value to parameter map.
   */
  public LocalPCA() {
    super();   
    optionHandler.put(BIG_VALUE_P, new Parameter(BIG_VALUE_P,BIG_VALUE_D,Parameter.Types.DOUBLE));
    optionHandler.put(SMALL_VALUE_P, new Parameter(SMALL_VALUE_P,SMALL_VALUE_D,Parameter.Types.DOUBLE));
    optionHandler.put(EIGENPAIR_FILTER_P, new Parameter(EIGENPAIR_FILTER_P,EIGENPAIR_FILTER_D,Parameter.Types.CLASS));
  }

  /**
   * Performs a LocalPCA for the object with the specified ids stored in the given
   * database.
   *
   * @param ids      the ids of the objects for which the PCA should be performed
   * @param database the database containing the objects
   */
  public void run(List<Integer> ids, Database<RealVector> database) {
    // logging
    StringBuffer msg = new StringBuffer();
    if (this.debug) {
      RealVector o = database.get(ids.get(0));
      String label = (String) database.getAssociation(
          AssociationID.LABEL, ids.get(0));
      msg.append("\nobject ").append(o).append(" ").append(label);
    }

    // sorted eigenpairs, eigenvectors, eigenvalues
    SortedEigenPairs eigenPairs = sortedEigenPairs(database, ids);
    eigenvectors = eigenPairs.eigenVectors();
    eigenvalues = eigenPairs.eigenValues();

    // filter into strong and weak eigenpairs
    FilteredEigenPairs filteredEigenPairs = eigenPairFilter.filter(eigenPairs);
    List<EigenPair> strongEigenPairs = filteredEigenPairs.getStrongEigenPairs();

    // correlationDimension = #strong EV
    correlationDimension = strongEigenPairs.size();

    // selection Matrix for weak EVs
    e_hat = new Matrix(eigenvalues.length, eigenvalues.length);
    // selection Matrix for strong EVs
    e_czech = new Matrix(eigenvalues.length, eigenvalues.length);
    for (int i = 0; i < eigenvalues.length; i++) {
      if (i < correlationDimension) {
        e_czech.set(i, i, big);
        e_hat.set(i, i, small);
      }
      else {
        e_czech.set(i, i, small);
        e_hat.set(i, i, big);
      }
    }
    strongEigenvectors = eigenvectors.times(e_czech);

    m_hat = eigenvectors.times(e_hat).times(eigenvectors.transpose());
    m_czech = eigenvectors.times(e_czech).times(eigenvectors.transpose());

    if (this.debug) {
      msg.append("\n ids =");
      for (Integer id : ids) {
        msg.append(database.getAssociation(AssociationID.LABEL, id) + ", ");
      }

      msg.append("\n  E = ");
      msg.append(Util.format(eigenvalues, ",", 6));

      msg.append("\n  V = ");
      msg.append(eigenvectors);

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
   * Sets the parameters big value and small value. Both big value and small
   * value are optional parameters. If they are not specified, default values
   * are used.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.getParameters();

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

    // eigenpair filter
    String className;
    if (optionHandler.isSet(EIGENPAIR_FILTER_P)) {
      className = optionHandler.getOptionValue(EIGENPAIR_FILTER_P);
    }
    else {
      className = DEFAULT_EIGENPAIR_FILTER;
    }
    try {
      eigenPairFilter = Util.instantiate(EigenPairFilter.class, className);
    }
    catch (UnableToComplyException e) {
      throw new WrongParameterValueException(EIGENPAIR_FILTER_P, className, EIGENPAIR_FILTER_D, e);
    }

    remainingParameters = eigenPairFilter.setParameters(remainingParameters);
    setParameters(args, remainingParameters);

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
    mySettings.addSetting(EIGENPAIR_FILTER_P, eigenPairFilter.getClass().getName());

    attributeSettings.addAll(eigenPairFilter.getAttributeSettings());
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
   * Returns a copy of the matrix of eigenvectors of the object to which this
   * PCA belongs to.
   *
   * @return the matrix of eigenvectors
   */
  public Matrix getEigenvectors() {
    return eigenvectors.copy();
  }

  /**
   * Returns a copy of the eigenvalues of the object to which this PCA belongs
   * to in decreasing order.
   *
   * @return the eigenvalues
   */
  public double[] getEigenvalues() {
    return Util.copy(eigenvalues);
  }

  /**
   * Returns a copy of the selection matrix of the weak eigenvectors (E_hat)
   * of the object to which this PCA belongs to.
   *
   * @return the selection matrix of the weak eigenvectors E_hat
   */
  public Matrix getSelectionMatrixOfWeakEigenvectors() {
    return e_hat.copy();
  }

  /**
   * Returns a copy of the selection matrix of the strong eigenvectors
   * (E_czech) of the object to which this PCA belongs to.
   *
   * @return the selection matrix of the weak eigenvectors E_czech
   */
  public Matrix getSelectionMatrixOfStrongEigenvectors() {
    return e_czech.copy();
  }

  /**
   * Returns a copy of the similarity matrix (M_hat) of the object to which
   * this PCA belongs to.
   *
   * @return the similarity matrix M_hat
   */
  public Matrix getSimilarityMatrix() {
    return m_hat.copy();
  }

  /**
   * Returns a copy of the dissimilarity matrix (M_czech) of the object to
   * which this PCA belongs to.
   *
   * @return the dissimilarity matrix M_hat
   */
  public Matrix getDissimilarityMatrix() {
    return m_czech.copy();
  }

  /**
   * Returns a copy of the strong eigenvectors of the object to which this PCA
   * belongs to.
   *
   * @return the matrix of eigenvectors
   */
  public Matrix strongEigenVectors() {
    return strongEigenvectors.copy();
  }

  /**
   * Determines and returns the eigenpairs (i.e. the eigenvectors and their
   * corresponding eigenvalues) sorted in descending order of their eigenvalues
   *
   * @param database the database holding the objects
   * @param ids      the list of the object ids for which the eigenpairs
   *                 should be determined
   * @return the eigenpairs (i.e. the eigenvectors and their
   *         corresponding eigenvalues) sorted in descending order of their eigenvalues
   */
  protected abstract SortedEigenPairs sortedEigenPairs(Database<RealVector> database, List<Integer> ids);
}
