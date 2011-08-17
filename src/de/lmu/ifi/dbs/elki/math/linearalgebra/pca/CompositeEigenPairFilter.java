package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
Ludwig-Maximilians-Universität München
Lehr- und Forschungseinheit für Datenbanksysteme
ELKI Development Team

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.util.List;

import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;

/**
 * The <code>CompositeEigenPairFilter</code> can be used to build a chain of
 * eigenpair filters.
 * 
 * @author Elke Achtert
 */
// todo parameter comments
public class CompositeEigenPairFilter implements EigenPairFilter {
  /**
   * The list of filters to use.
   */
  public static final OptionID EIGENPAIR_FILTER_COMPOSITE_LIST = OptionID.getOrCreateOptionID("pca.filter.composite.list", "A comma separated list of the class names of the filters to be used. " + "The specified filters will be applied sequentially in the given order.");

  /**
   * The filters to be applied.
   */
  private List<EigenPairFilter> filters;

  /**
   * Constructor.
   * 
   * @param filters Filters to use.
   */
  public CompositeEigenPairFilter(List<EigenPairFilter> filters) {
    super();
    this.filters = filters;
  }

  /**
   * Filters the specified eigenpairs into strong and weak eigenpairs, where
   * strong eigenpairs having high variances and weak eigenpairs having small
   * variances.
   * 
   * @param eigenPairs the eigenPairs (i.e. the eigenvectors and
   * @return the filtered eigenpairs
   */
  @Override
  public FilteredEigenPairs filter(SortedEigenPairs eigenPairs) {
    FilteredEigenPairs result = null;
    for(EigenPairFilter f : filters) {
      result = f.filter(eigenPairs);
      eigenPairs = new SortedEigenPairs(result.getStrongEigenPairs());
    }
    return result;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * The filters to be applied.
     */
    private List<EigenPairFilter> filters = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectListParameter<EigenPairFilter> filtersP = new ObjectListParameter<EigenPairFilter>(EIGENPAIR_FILTER_COMPOSITE_LIST, EigenPairFilter.class);

      if(config.grab(filtersP)) {
        filters = filtersP.instantiateClasses(config);
      }
    }

    @Override
    protected CompositeEigenPairFilter makeInstance() {
      return new CompositeEigenPairFilter(filters);
    }
  }
}