package de.lmu.ifi.dbs.varianceanalysis;

import de.lmu.ifi.dbs.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.ClassListParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The <code>CompositeEigenPairFilter</code> can be used to
 * build a chain of eigenpair filters.
 *
 * @author Elke Achtert 
 */
public class CompositeEigenPairFilter extends AbstractParameterizable implements EigenPairFilter {
  /**
   * A pattern defining a comma.
   */
  public static final Pattern COMMA_SPLIT = Pattern.compile(",");

  /**
   * Parameter for filters.
   */
  public static final String FILTERS_P = "filters";

  /**
   * Description for parameter filters.
   */
  public static final String FILTERS_D = "A comma separated list of the class names of " +
                                         "the filters to be used. The specified filters will be applied " +
                                         "sequentially in the given order. " +
                                         Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(EigenPairFilter.class);

  /**
   * The filters to be applied.
   */
  private List<EigenPairFilter> filters;

  /**
   * Provides a new EigenPairFilter that builds a chain of user specified eigenpair filters.
   */
  public CompositeEigenPairFilter() {
    super();

    optionHandler.put(new ClassListParameter(FILTERS_P, FILTERS_D,EigenPairFilter.class));
  }

  /**
   * Filters the specified eigenpairs into strong and weak eigenpairs,
   * where strong eigenpairs having high variances
   * and weak eigenpairs having small variances.
   *
   * @param eigenPairs the eigenPairs (i.e. the eigenvectors and
   * @return the filtered eigenpairs
   */
  public FilteredEigenPairs filter(SortedEigenPairs eigenPairs) {
    FilteredEigenPairs result = null;
    for (EigenPairFilter f : filters) {
      result = f.filter(eigenPairs);
      eigenPairs = new SortedEigenPairs(result.getStrongEigenPairs());
    }
    return result;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(this.getClass().getName());
    description.append(" builds a chain of user specified eigen pair filters.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    //filters
    List<String> filtersString = (List<String>)optionHandler.getOptionValue(FILTERS_P);
    filters = new ArrayList<EigenPairFilter>(filtersString.size());

    for (String filterClass : filtersString) {
      try {
        EigenPairFilter f = Util.instantiate(EigenPairFilter.class, filterClass);
        remainingParameters = f.setParameters(remainingParameters);
        filters.add(f);
      }
      catch (UnableToComplyException e) {
        throw new WrongParameterValueException(FILTERS_P, filterClass, FILTERS_D, e);
      }
    }

    setParameters(args, remainingParameters);
    return remainingParameters;
  }
}