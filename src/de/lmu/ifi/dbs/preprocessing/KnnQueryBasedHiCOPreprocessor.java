package de.lmu.ifi.dbs.preprocessing;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Computes the HiCO correlation dimension of objects of a certain database. The PCA
 * is based on k nearest neighbor queries.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class KnnQueryBasedHiCOPreprocessor extends HiCOPreprocessor {

  /**
   * Undefined value for k.
   */
  public static final int UNDEFINED_K = -1;

  /**
   * Option string for parameter k.
   */
  public static final String K_P = "k";

  /**
   * Description for parameter k.
   */
  public static final String K_D = "a positive integer specifying the number of "
                                   + "nearest neighbors considered in the PCA. "
                                   + "If this value is not defined, k ist set to three "
                                   + "times of the dimensionality of the database objects.";

  /**
   * The number of nearest neighbors considered in the PCA.
   */
  private int k;

  /**
   * Provides a new Preprocessor that computes the correlation dimension of
   * objects of a certain database based on a k nearest neighbor query.
   */
  public KnnQueryBasedHiCOPreprocessor() {
    super();
    optionHandler.put(K_P, new Parameter(K_P,K_D,Parameter.Types.INT));
  }

  /**
   * @see HiCOPreprocessor#objectIDsForPCA(Integer, Database,boolean, boolean)
   */
  protected List<Integer> objectIDsForPCA(Integer id, Database<RealVector> database, boolean verbose, boolean time) {
    if (k == UNDEFINED_K) {
      RealVector obj = database.get(id);
      k = 3 * obj.getDimensionality();
    }

    pcaDistanceFunction.setDatabase(database, verbose, time);
    List<QueryResult<DoubleDistance>> knns = database.kNNQueryForID(id, k, pcaDistanceFunction);

    List<Integer> ids = new ArrayList<Integer>(knns.size());
    for (QueryResult knn : knns) {
      ids.add(knn.getID());
    }

    return ids;
  }

  /**
   * Sets the value for the parameter k. If k is not specified, the default
   * value is used.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    if (optionHandler.isSet(K_P)) {
      String kString = optionHandler.getOptionValue(K_P);
      try {
        k = Integer.parseInt(kString);
        if (k <= 0) {
          throw new WrongParameterValueException(K_P, kString, K_D);
        }
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(K_P, kString, K_D, e);
      }
    }
    else {
      k = UNDEFINED_K;
    }
    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();

    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(K_P, Integer.toString(k));

    return attributeSettings;
  }

  /**
   * Returns a description of the class and the required parameters. <p/> This
   * description should be suitable for a usage description.
   *
   * @return String a description of the class and the required parameters
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(KnnQueryBasedHiCOPreprocessor.class
    .getName());
    description
    .append(" computes the correlation dimension of objects of a certain database.\n");
    description.append("The PCA is based on k nearest neighbor queries.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }
}
