package de.lmu.ifi.dbs.elki.visualization.projector;
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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Produce one-dimensional projections.
 * 
 * @author Erich Schubert
 */
// TODO: re-add maxdim option
public class HistogramFactory implements ProjectorFactory {
  /**
   * Maximum dimensionality
   */
  private int maxdim = ScatterPlotFactory.MAX_DIMENSIONS_DEFAULT;

  /**
   * Constructor.
   * @param maxdim Maximum dimensionality
   */
  public HistogramFactory(int maxdim) {
    super();
    this.maxdim = maxdim;
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    Database db = ResultUtil.findDatabase(newResult);
    if(db != null) {
      for(Relation<?> rel : db.getRelations()) {
        if(TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
          @SuppressWarnings("unchecked")
          Relation<NumberVector<?, ?>> vrel = (Relation<NumberVector<?, ?>>) rel;
          final int dim = DatabaseUtil.dimensionality(vrel);
          HistogramProjector<NumberVector<?, ?>> proj = new HistogramProjector<NumberVector<?, ?>>(vrel, Math.min(dim, maxdim));
          baseResult.getHierarchy().add(vrel, proj);
        }
      }
    }
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
     * Stores the maximum number of dimensions to show.
     */
    private int maxdim = ScatterPlotFactory.MAX_DIMENSIONS_DEFAULT;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter maxdimP = new IntParameter(ScatterPlotFactory.Parameterizer.MAXDIM_ID, new GreaterEqualConstraint(1), ScatterPlotFactory.MAX_DIMENSIONS_DEFAULT);
      if(config.grab(maxdimP)) {
        maxdim = maxdimP.getValue();
      }
    }

    @Override
    protected HistogramFactory makeInstance() {
      return new HistogramFactory(maxdim);
    }
  }
}