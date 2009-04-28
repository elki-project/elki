package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

import java.util.List;

import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * The <code>CompositeEigenPairFilter</code> can be used to build a chain of
 * eigenpair filters.
 * 
 * @author Elke Achtert
 */
// todo parameter comments
public class CompositeEigenPairFilter extends AbstractParameterizable implements EigenPairFilter {
  /**
   * OptionID for
   * {@link #FILTERS_PARAM}
   */
  public static final OptionID EIGENPAIR_FILTER_COMPOSITE_LIST =
    OptionID.getOrCreateOptionID("pca.filter.composite.list",
        "A comma separated list of the class names of the filters to be used. "
        + "The specified filters will be applied sequentially in the given order.");

  private final ClassListParameter<EigenPairFilter> FILTERS_PARAM =
    new ClassListParameter<EigenPairFilter>(EIGENPAIR_FILTER_COMPOSITE_LIST, EigenPairFilter.class);

  /**
   * The filters to be applied.
   */
  private List<EigenPairFilter> filters;

  /**
   * Provides a new EigenPairFilter that builds a chain of user specified
   * eigenpair filters.
   */
  public CompositeEigenPairFilter() {
    super();

    addOption(FILTERS_PARAM);
  }

  /**
   * Filters the specified eigenpairs into strong and weak eigenpairs, where
   * strong eigenpairs having high variances and weak eigenpairs having small
   * variances.
   * 
   * @param eigenPairs the eigenPairs (i.e. the eigenvectors and
   * @return the filtered eigenpairs
   */
  public FilteredEigenPairs filter(SortedEigenPairs eigenPairs) {
    FilteredEigenPairs result = null;
    for(EigenPairFilter f : filters) {
      result = f.filter(eigenPairs);
      eigenPairs = new SortedEigenPairs(result.getStrongEigenPairs());
    }
    return result;
  }

  @Override
  public String parameterDescription() {
    StringBuffer description = new StringBuffer();
    description.append(this.getClass().getName());
    description.append(" builds a chain of user specified eigen pair filters.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }

  /**
   * Calls the super method and instantiates {@link #filters} according to the
   * value of parameter {@link #FILTERS_PARAM}. The remaining parameters are
   * passed to each instance of {@link #filters}.
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // filters
    filters = FILTERS_PARAM.instantiateClasses();
    for(EigenPairFilter filter : filters) {
      remainingParameters = filter.setParameters(remainingParameters);
      addParameterizable(filter);
    }

    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }
}