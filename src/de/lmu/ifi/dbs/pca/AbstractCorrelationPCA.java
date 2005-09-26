package de.lmu.ifi.dbs.pca;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.AbstractDatabase;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.linearalgebra.Matrix;
import de.lmu.ifi.dbs.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AbstractCorrelationPCA provides some methods valid for any extending class.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractCorrelationPCA implements CorrelationPCA {
  /**
   * Logger object for logging messages.
   */
  protected static Logger logger;

  /**
   * The loggerLevel for logging messages.
   */
  protected static Level loggerLevel = Level.OFF;

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
  public static final String BIG_VALUE__D = "<double>a constant big value to reset eigenvalues " +
    "(default: " + DEFAULT_BIG_VALUE + ").";

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
  public static final String SMALL_VALUE_D = "<double>a constant small value to reset eigenvalues " +
    "(default: " + DEFAULT_SMALL_VALUE + ").";

  /**
   * Holds the big value.
   */
  private double big;

  /**
   * Holds the small value.
   */
  private double small;

  /**
   * The correlation dimension (i.e. the number of strong eigenvectors)
   * of the object to which this PCA belongs to.
   */
  private int correlationDimension = 0;

  /**
   * The eigenvalues in decreasing order.
   */
  private double[] eigenvalues;

  /**
   * The eigenvectors in decreasing order to their corresponding eigenvalues.
   */
  private Matrix eigenvectors;

  /**
   * The strong eigenvectors.
   */
  private Matrix strongEigenvectors;

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
   * Map providing a mapping of parameters to their descriptions.
   */
  protected Map<String, String> parameterToDescription = new Hashtable<String, String>();

  /**
   * OptionHandler to handler options. The option handler should be initialized using
   * parameterToDescription in any non-abstract class extending this class.
   */
  protected OptionHandler optionHandler;

  /**
   * Adds parameter for big and small value to parameter map.
   */
  public AbstractCorrelationPCA() {
    initLogger();
    parameterToDescription.put(BIG_VALUE_P + OptionHandler.EXPECTS_VALUE, BIG_VALUE__D);
    parameterToDescription.put(SMALL_VALUE_P + OptionHandler.EXPECTS_VALUE, SMALL_VALUE_D);
  }

  /**
   * Performs a PCA for the object with the specified ids stored in the given database.
   *
   * @param ids      the ids of the objects for which the PCA should be performed
   * @param database the database containing the objects
   * @param alpha    the threshold for strong eigenvectors: the strong eigenvectors
   */
  public void run(List<Integer> ids, Database<DoubleVector> database, double alpha) {
    // logging
    StringBuffer msg = new StringBuffer();
    DoubleVector o = database.get(ids.get(0));
    String label = (String) database.getAssociation(AbstractDatabase.ASSOCIATION_ID_LABEL,
      ids.get(0));
    msg.append("object ");
    msg.append(o);
    msg.append(" ");
    msg.append(label);

    // eigenvalues (and eigenvectors) in descending order
    EigenvalueDecomposition evd = eigenValueDecomposition(database, ids);
    SortedEigenPairs eigenPairs = new SortedEigenPairs(evd, false);
    eigenvalues = eigenPairs.eigenValues();
    eigenvectors = eigenPairs.eigenVectors();

    computeSelectionMatrices(alpha);

    msg.append("\n  E = ");
    msg.append(Util.format(eigenvalues));

    msg.append("\n  V = ");
    msg.append(eigenvectors);

    msg.append("\n  E_hat = ");
    msg.append(e_hat);

    msg.append("\n  E_czech = ");
    msg.append(e_czech);

    msg.append("\n  corrDim = ");
    msg.append(correlationDimension);

    logger.info(msg.toString() + "\n");

//    System.out.println("  corrDim = " + correlationDimension);
//    System.out.println("  E = " + Util.format(eigenvalues, ", ", 6));
//    System.out.println("");
  }

  /**
   * Performs a PCA for the object with the specified ids stored in the given database.
   *
   * @param ids       the ids of the objects for which the PCA should be performed
   * @param database  the database containing the objects
   * @param strongEVs the number of strong eigenvectors
   */
  public void run(List<Integer> ids, Database<DoubleVector> database, int strongEVs) {
    // logging
    StringBuffer msg = new StringBuffer();
    DoubleVector o = database.get(ids.get(0));
    String label = (String) database.getAssociation(AbstractDatabase.ASSOCIATION_ID_LABEL,
      ids.get(0));
    msg.append("object ");
    msg.append(o);
    msg.append(" ");
    msg.append(label);

    // eigenvalues (and eigenvectors) in descending order
    EigenvalueDecomposition evd = eigenValueDecomposition(database, ids);
    SortedEigenPairs eigenPairs = new SortedEigenPairs(evd, false);
    eigenvalues = eigenPairs.eigenValues();
    eigenvectors = eigenPairs.eigenVectors();

    computeSelectionMatrices(strongEVs);

    msg.append("\n  E = ");
    msg.append(Util.format(eigenvalues));

    msg.append("\n  V = ");
    msg.append(eigenvectors);

    msg.append("\n  E_hat = ");
    msg.append(e_hat);

    msg.append("\n  E_czech = ");
    msg.append(e_czech);

    msg.append("\n  corrDim = ");
    msg.append(correlationDimension);

    logger.info(msg.toString() + "\n");

    System.out.println("  corrDim = " + correlationDimension);
    System.out.println("  E = " + Util.format(eigenvalues, ", ", 6));
    System.out.println("");
  }

  /**
   * Sets the parameters big value and small value. Both big value and small value are optional
   * parameters. If they are not specified, default values are used.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = optionHandler.grabOptions(args);

    if (optionHandler.isSet(BIG_VALUE_P)) {
      try {
        big = Double.parseDouble(optionHandler.getOptionValue(BIG_VALUE_P));
      }
      catch (UnusedParameterException e) {
        throw new IllegalArgumentException(e);
      }
      catch (NoParameterValueException e) {
        throw new IllegalArgumentException(e);
      }
    }
    else {
      big = DEFAULT_BIG_VALUE;
    }

    if (optionHandler.isSet(SMALL_VALUE_P)) {
      try {
        small = Double.parseDouble(optionHandler.getOptionValue(SMALL_VALUE_P));
      }
      catch (UnusedParameterException e) {
        throw new IllegalArgumentException(e);
      }
      catch (NoParameterValueException e) {
        throw new IllegalArgumentException(e);
      }
    }
    else {
      small = DEFAULT_SMALL_VALUE;
    }

    if (big <= small) {
      throw new IllegalArgumentException(getClass().getSimpleName() + ": big value has to be greater than small value" +
        "(big = " + big + " <= " + small + " = small)");
    }

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    return optionHandler.usage("", false);
  }

  /**
   * Returns the parameter setting of this PCA.
   *
   * @return the parameter setting of this PCA
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = new ArrayList<AttributeSettings>();

    AttributeSettings attributeSettings = new AttributeSettings(this);
    attributeSettings.addSetting(BIG_VALUE_P, Double.toString(big));
    attributeSettings.addSetting(SMALL_VALUE_P, Double.toString(small));

    result.add(attributeSettings);
    return result;
  }

  /**
   * Returns the correlation dimension (i.e. the number of strong eigenvectors)
   * of the object to which this PCA belongs to.
   *
   * @return the correlation dimension
   */
  public int getCorrelationDimension() {
    return correlationDimension;
  }

  /**
   * Returns a copy of the matrix of eigenvectors
   * of the object to which this PCA belongs to.
   *
   * @return the matrix of eigenvectors
   */
  public Matrix getEigenvectors() {
    return eigenvectors.copy();
  }

  /**
   * Returns a copy of the eigenvalues of the object to which this PCA belongs to
   * in decreasing order.
   *
   * @return the eigenvalues
   */
  public double[] getEigenvalues() {
    return Util.copy(eigenvalues);
  }

  /**
   * Returns a copy of the selection matrix of the weak eigenvectors (E_hat) of the object
   * to which this PCA belongs to.
   *
   * @return the selection matrix of the weak eigenvectors E_hat
   */
  public Matrix getSelectionMatrixOfWeakEigenvectors() {
    return e_hat.copy();
  }

  /**
   * Returns a copy of the selection matrix of the strong eigenvectors (E_czech) of the object
   * to which this PCA belongs to.
   *
   * @return the selection matrix of the weak eigenvectors E_czech
   */
  public Matrix getSelectionMatrixOfStrongEigenvectors() {
    return e_czech.copy();
  }

  /**
   * Returns a copy of the similarity matrix (M_hat) of the object
   * to which this PCA belongs to.
   *
   * @return the similarity matrix M_hat
   */
  public Matrix getSimilarityMatrix() {
    return m_hat.copy();
  }

  /**
   * Returns a copy of the dissimilarity matrix (M_czech) of the object
   * to which this PCA belongs to.
   *
   * @return the dissimilarity matrix M_hat
   */
  public Matrix getDissimilarityMatrix() {
    return m_czech.copy();
  }

  /**
   * Returns a copy of the strong eigenvectors of the object to which this PCA belongs to.
   *
   * @return the matrix of eigenvectors
   */
  public Matrix strongEigenVectors() {
    return strongEigenvectors.copy();
  }

  /**
   * Performs the actual eigenvalue decomposition on the specified object ids
   * stored in the given database.
   *
   * @param database the database holding the objects
   * @param ids      the list of the object ids for which the eigenvalue decomposition
   *                 should be performed
   * @return the actual eigenvalue decomposition on the specified object ids
   *         stored in the given database
   */
  protected abstract EigenvalueDecomposition eigenValueDecomposition(Database<DoubleVector> database,
                                                                     List<Integer> ids);

  /**
   * Computes the selection matrices of the weak and strong eigenvectors,
   * the similarity matrix and the correlation dimension.
   *
   * @param alpha the threshold for strong eigenvectors
   */
  private void computeSelectionMatrices(double alpha) {
    StringBuffer msg = new StringBuffer();

    msg.append("alpha = ");
    msg.append(alpha);

    // weak EVs
    e_hat = new Matrix(eigenvalues.length, eigenvalues.length);
    // strong EVs
    e_czech = new Matrix(eigenvalues.length, eigenvalues.length);

    double totalSum = 0;
    for (double eigenvalue : eigenvalues) {
      totalSum += eigenvalue;
    }
    msg.append("\n totalSum = ");
    msg.append(totalSum);

    double currSum = 0;
    boolean found = false;
    for (int i = 0; i < eigenvalues.length; i++) {
      currSum += eigenvalues[i];
      // strong EV -> set EW in E_czech to big value, set EW in E_hat to small value
      // weak EV -> set EW in E_czech to small value, set EW in E_hat to big value
      if (currSum / totalSum >= alpha) {
        if (! found) {
          found = true;
          correlationDimension++;
          e_czech.set(i, i, big);
          e_hat.set(i, i, small);
        }
        else {
          e_czech.set(i, i, small);
          e_hat.set(i, i, big);
        }
      }
      else {
        correlationDimension++;
        e_czech.set(i, i, big);
        e_hat.set(i, i, small);
      }
    }
    strongEigenvectors = eigenvectors.times(e_czech);

    m_hat = eigenvectors.times(e_hat).times(eigenvectors.transpose());
    m_czech = eigenvectors.times(e_czech).times(eigenvectors.transpose());

    logger.info(msg.toString());
  }

  /**
   * Computes the selection matrices of the weak and strong eigenvectors,
   * the similarity matrix and the correlation dimension.
   *
   * @param strongEVs the number of strong eigenvectors
   */
  private void computeSelectionMatrices(int strongEVs) {
    StringBuffer msg = new StringBuffer();

    msg.append("strongEVs = ");
    msg.append(strongEVs);

    e_hat = new Matrix(eigenvalues.length, eigenvalues.length);
    e_czech = new Matrix(eigenvalues.length, eigenvalues.length);

    for (int i = 0; i < eigenvalues.length; i++) {
      if (i < strongEVs) {
        correlationDimension++;
        e_czech.set(i, i, 1);
      }
      else {
        e_hat.set(i, i, 1);
      }
    }
    strongEigenvectors = eigenvectors.times(e_czech);

    m_hat = eigenvectors.times(e_hat).times(eigenvectors.transpose());
    m_czech = eigenvectors.times(e_czech).times(eigenvectors.transpose());

    logger.info(msg.toString());
  }

  /**
   * Initializes the logger object.
   */
  private void initLogger() {
    logger = Logger.getLogger(AbstractCorrelationPCA.class.toString());
    logger.setLevel(loggerLevel);
  }
}
