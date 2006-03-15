package de.lmu.ifi.dbs.pca;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.linearalgebra.Matrix;
import de.lmu.ifi.dbs.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AbstractCorrelationPCA provides some methods valid for any extending class.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractLocalPCA implements LocalPCA {
  /**
   * The debug flag.
   */
  protected static boolean DEBUG = false;

  /**
   * Logger object for logging messages.
   */
  protected static Logger logger;

  /**
   * The loggerLevel for logging messages.
   */
  protected static Level loggerLevel = Level.INFO;

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
  public static final String BIG_VALUE_D = "<double>a constant big value (> 0, big > small) to reset eigenvalues "
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
  public static final String SMALL_VALUE_D = "<double>a constant small value (>= 0, small < big) to reset eigenvalues " + "(default: " + DEFAULT_SMALL_VALUE + ").";

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
   * OptionHandler to handler options. The option handler should be
   * initialized using parameterToDescription in any non-abstract class
   * extending this class.
   */
  protected OptionHandler optionHandler;
  
  /**
   * Holds the currently set parameter array.
   */
  private String[] currentParameterArray = new String[0];

  /**
   * Adds parameter for big and small value to parameter map.
   */
  public AbstractLocalPCA() {
    initLogger();
    parameterToDescription.put(BIG_VALUE_P + OptionHandler.EXPECTS_VALUE, BIG_VALUE_D);
    parameterToDescription.put(SMALL_VALUE_P + OptionHandler.EXPECTS_VALUE, SMALL_VALUE_D);
  }

  /**
   * Performs a PCA for the object with the specified ids stored in the given
   * database.
   *
   * @param ids      the ids of the objects for which the PCA should be performed
   * @param database the database containing the objects
   * @param alpha    the threshold for strong eigenvectors: the strong eigenvectors
   */
  public void run(List<Integer> ids, Database<RealVector> database, double alpha) {
    // logging
    StringBuffer msg = new StringBuffer();
    if (DEBUG) {
      RealVector o = database.get(ids.get(0));
      String label = (String) database.getAssociation(AssociationID.LABEL, ids.get(0));
      msg.append("object ");
      msg.append(o);
      msg.append(" ");
      msg.append(label);
    }

    // eigenvalues (and eigenvectors) in descending order
    EigenvalueDecomposition evd = eigenValueDecomposition(database, ids);
    SortedEigenPairs eigenPairs = new SortedEigenPairs(evd, false);
    eigenvalues = eigenPairs.eigenValues();
    eigenvectors = eigenPairs.eigenVectors();

    computeSelectionMatrices(alpha);

    if (DEBUG) {
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
    }
  }

  /**
   * Performs a PCA for the object with the specified ids stored in the given
   * database.
   *
   * @param ids      the ids of the objects for which the PCA should be performed
   * @param database the database containing the objects
   * @param delta    the threshold for sufficiently small Eigenvalues after normalization
   */
  public void run4CPCA(List<Integer> ids, Database<RealVector> database, double delta) {
    if (ids.size() == 0) {
      throw new IllegalArgumentException("empty list of objects");
    }
    // logging
    StringBuffer msg = new StringBuffer();
    RealVector o = database.get(ids.get(0));
    String label = (String) database.getAssociation(AssociationID.LABEL, ids.get(0));
    if (DEBUG) {
      msg.append("object ");
      msg.append(o);
      msg.append(" ");
      msg.append(label);
    }

    // eigenvalues (and eigenvectors) in descending order
    EigenvalueDecomposition evd = eigenValueDecomposition(database, ids);
    SortedEigenPairs eigenPairs = new SortedEigenPairs(evd, false);
    eigenvalues = eigenPairs.eigenValues();
    eigenvectors = eigenPairs.eigenVectors();

    computeSelectionMatrices4C(delta);

    if (DEBUG) {
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
    }
  }

  /**
   * Performs a PCA for the object with the specified ids stored in the given
   * database.
   *
   * @param ids       the ids of the objects for which the PCA should be performed
   * @param database  the database containing the objects
   * @param strongEVs the number of strong eigenvectors
   */
  public void run(List<Integer> ids, Database<RealVector> database, int strongEVs) {
    // logging
    StringBuffer msg = new StringBuffer();
    RealVector o = database.get(ids.get(0));
    String label = (String) database.getAssociation(AssociationID.LABEL, ids.get(0));
    if (DEBUG) {
      msg.append("object ");
      msg.append(o);
      msg.append(" ");
      msg.append(label);
    }

    // eigenvalues (and eigenvectors) in descending order
    EigenvalueDecomposition evd = eigenValueDecomposition(database, ids);
    SortedEigenPairs eigenPairs = new SortedEigenPairs(evd, false);
    eigenvalues = eigenPairs.eigenValues();
    eigenvectors = eigenPairs.eigenVectors();

    computeSelectionMatrices(strongEVs);

    if (DEBUG) {
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
    String[] remainingParameters = optionHandler.grabOptions(args);

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
    setParameters(args, remainingParameters);
    return remainingParameters;
  }
  
  /**
   * Sets the difference of the first array minus the second array
   * as the currently set parameter array.
   * 
   * 
   * @param complete the complete array
   * @param part an array that contains only elements of the first array
   */
  protected void setParameters(String[] complete, String[] part)
  {
      currentParameterArray = Util.difference(complete, part);
  }
  
  /**
   * 
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getParameters()
   */
  public String[] getParameters()
  {
      String[] param = new String[currentParameterArray.length];
      System.arraycopy(currentParameterArray, 0, param, 0, currentParameterArray.length);
      return param;
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
   * Performs the actual eigenvalue decomposition on the specified object ids
   * stored in the given database.
   *
   * @param database the database holding the objects
   * @param ids      the list of the object ids for which the eigenvalue
   *                 decomposition should be performed
   * @return the actual eigenvalue decomposition on the specified object ids
   *         stored in the given database
   */
  protected abstract EigenvalueDecomposition eigenValueDecomposition(Database<RealVector> database, List<Integer> ids);

  /**
   * Computes the selection matrices of the weak and strong eigenvectors, the
   * similarity matrix and the correlation dimension.
   *
   * @param alpha the threshold for strong eigenvectors
   */
  private void computeSelectionMatrices(double alpha) {
    StringBuffer msg = new StringBuffer();

    if (DEBUG) {
      msg.append("alpha = ");
      msg.append(alpha);
    }

    // weak EVs
    e_hat = new Matrix(eigenvalues.length, eigenvalues.length);
    // strong EVs
    e_czech = new Matrix(eigenvalues.length, eigenvalues.length);

    double totalSum = 0;
    for (double eigenvalue : eigenvalues) {
      totalSum += eigenvalue;
    }

    if (DEBUG) {
      msg.append("\n totalSum = ");
      msg.append(totalSum);
    }

    double currSum = 0;
    boolean found = false;
    for (int i = 0; i < eigenvalues.length; i++) {
      currSum += eigenvalues[i];
      // strong EV -> set EW in E_czech to big value, set EW in E_hat to
      // small value
      // weak EV -> set EW in E_czech to small value, set EW in E_hat to
      // big value
      if (currSum / totalSum >= alpha) {
        if (!found) {
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

    if (DEBUG) {
      logger.info(msg.toString());
    }
  }

  /**
   * Computes the selection matrices of the weak and strong eigenvectors, the
   * similarity matrix and the correlation dimension.
   *
   * @param delta the threshold for small eigenvalues
   */
  private void computeSelectionMatrices4C(double delta) {
    StringBuffer msg = new StringBuffer();

    if (DEBUG) {
      msg.append("delta = ");
      msg.append(delta);
    }

    // weak EVs
    e_hat = new Matrix(eigenvalues.length, eigenvalues.length);
    // strong EVs
    e_czech = new Matrix(eigenvalues.length, eigenvalues.length);

    double biggestEigenvalue = eigenvalues[0];

    if (DEBUG) {
      msg.append("\n biggest Eigenvalue = ");
      msg.append(biggestEigenvalue);
    }

    for (int i = 0; i < eigenvalues.length; i++) {
      // strong EV -> set EW in E_czech to big value, set EW in E_hat to
      // small value
      // weak EV -> set EW in E_czech to small value, set EW in E_hat to
      // big value
      if (eigenvalues[i] / biggestEigenvalue <= delta) {
        e_czech.set(i, i, small);
        e_hat.set(i, i, big);
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

    if (DEBUG) {
      logger.info(msg.toString());
    }
  }

  /**
   * Computes the selection matrices of the weak and strong eigenvectors, the
   * similarity matrix and the correlation dimension.
   *
   * @param strongEVs the number of strong eigenvectors
   */
  private void computeSelectionMatrices(int strongEVs) {
    StringBuffer msg = new StringBuffer();

    if (DEBUG) {
      msg.append("strongEVs = ");
      msg.append(strongEVs);
    }

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

    if (DEBUG) {
      logger.info(msg.toString());
    }
  }

  /**
   * Initializes the logger object.
   */
  private void initLogger() {
    logger = Logger.getLogger(AbstractLocalPCA.class.toString());
    logger.setLevel(loggerLevel);
  }
}
