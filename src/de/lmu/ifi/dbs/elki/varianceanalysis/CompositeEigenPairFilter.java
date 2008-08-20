package de.lmu.ifi.dbs.elki.varianceanalysis;

import java.util.List;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * The <code>CompositeEigenPairFilter</code> can be used to
 * build a chain of eigenpair filters.
 *
 * @author Elke Achtert 
 */
public class CompositeEigenPairFilter extends AbstractParameterizable implements EigenPairFilter {
  private final ClassListParameter<EigenPairFilter> FILTERS_PARAM = new ClassListParameter<EigenPairFilter>(
      OptionID.EIGENPAIR_FILTER_COMPOSITE_LIST,
      EigenPairFilter.class);
  
  /**
   * The filters to be applied.
   */
  private List<EigenPairFilter> filters;

  /**
   * Provides a new EigenPairFilter that builds a chain of user specified eigenpair filters.
   */
  public CompositeEigenPairFilter() {
    super();

    addOption(FILTERS_PARAM);
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
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#parameterDescription()
   */
  public String parameterDescription() {
    StringBuffer description = new StringBuffer();
    description.append(this.getClass().getName());
    description.append(" builds a chain of user specified eigen pair filters.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }

  /**
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    //filters
    filters = FILTERS_PARAM.instantiateClasses();
    // TODO: do we need to pass parameters manually?
    
    setParameters(args, remainingParameters);

    return remainingParameters;
  }
}