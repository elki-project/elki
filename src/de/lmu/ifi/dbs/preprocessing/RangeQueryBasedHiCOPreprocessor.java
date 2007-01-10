package de.lmu.ifi.dbs.preprocessing;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

/**
 * Computes the HiCO correlation dimension of objects of a certain database.
 * The PCA is based on epsilon range queries.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class RangeQueryBasedHiCOPreprocessor extends HiCOPreprocessor {

  /**
   * Parameter for epsilon.
   */
  public static final String EPSILON_P = "preprocessorEpsilon";

  /**
   * Description for parameter epsilon.
   */
  public static final String EPSILON_D = "an epsilon value suitable to the specified distance function";

  /**
   * Epsilon.
   */
  protected String epsilon;

  /**
   * Provides a new Preprocessor that computes the correlation dimension of
   * objects of a certain database based on a range query.
   */
  public RangeQueryBasedHiCOPreprocessor() {
    super();
    ParameterConstraint<String> con = new DistanceFunctionPatternConstraint(pcaDistanceFunction);
    optionHandler.put(EPSILON_P, new PatternParameter(EPSILON_P,EPSILON_D,con));
  }

  /**
   * @see HiCOPreprocessor#objectIDsForPCA(Integer, de.lmu.ifi.dbs.database.Database, boolean, boolean)
   */
  protected List<Integer> objectIDsForPCA(Integer id, Database<RealVector> database, boolean verbose, boolean time) {
    pcaDistanceFunction.setDatabase(database, verbose, time);

    List<QueryResult<DoubleDistance>> knns = database.rangeQuery(id, epsilon, pcaDistanceFunction);

    List<Integer> ids = new ArrayList<Integer>(knns.size());
    for (QueryResult knn : knns) {
      ids.add(knn.getID());
    }

    return ids;
  }

  /**
   * Sets the value for the required parameter k.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    epsilon = (String)optionHandler.getOptionValue(EPSILON_P);
   
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
    mySettings.addSetting(EPSILON_P, epsilon);

    return attributeSettings;
  }

  /**
   * Returns a description of the class and the required parameters.
   * <p/>
   * This description should be suitable for a usage description.
   *
   * @return String a description of the class and the required parameters
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(RangeQueryBasedHiCOPreprocessor.class.getName());
    description.append(" computes the correlation dimension of objects of a certain database.\n");
    description.append("The PCA is based on epsilon range queries.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }
}
