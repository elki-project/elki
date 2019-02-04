/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.algorithm.statistics;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.ScalesResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ListSizeConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleListParameter;

/**
 * Pseudo "algorithm" that computes the global min/max for a relation across all
 * attributes.
 *
 * FIXME: this should become part of relation metadata.
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
@Description("Setup a scaling so that all dimensions are scaled equally in visualization.")
public class AddSingleScale implements Algorithm {
  /**
   * Minimum and maximum to use.
   */
  double[] minmax = null;

  /**
   * Constructor.
   *
   * @param minmax Minimum and maximum values
   */
  public AddSingleScale(double[] minmax) {
    super();
    this.minmax = minmax;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Result run(Database database) {
    for(Relation<?> rel : database.getRelations()) {
      if(TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        ScalesResult res = run((Relation<? extends NumberVector>) rel);
        ResultUtil.addChildResult(rel, res);
      }
    }
    return null;
  }

  /**
   * Add scales to a single vector relation.
   *
   * @param rel Relation
   * @return Scales
   */
  private ScalesResult run(Relation<? extends NumberVector> rel) {
    final int dim = RelationUtil.dimensionality(rel);
    LinearScale[] scales = new LinearScale[dim];
    if(minmax == null) {
      DoubleMinMax mm = new DoubleMinMax();
      for(DBIDIter iditer = rel.iterDBIDs(); iditer.valid(); iditer.advance()) {
        NumberVector vec = rel.get(iditer);
        for(int d = 0; d < dim; d++) {
          final double val = vec.doubleValue(d);
          if(val != val) {
            continue; // NaN
          }
          mm.put(val);
        }
      }
      LinearScale scale = new LinearScale(mm.getMin(), mm.getMax());
      for(int i = 0; i < dim; i++) {
        scales[i] = scale;
      }
    }
    else {
      // Use predefined.
      LinearScale scale = new LinearScale(minmax[0], minmax[1]);
      for(int i = 0; i < dim; i++) {
        scales[i] = scale;
      }
    }
    ScalesResult res = new ScalesResult(scales);
    return res;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Minimum and maximum values.
     */
    public static final OptionID MINMAX_ID = new OptionID("scales.minmax", "Forcibly set the scales to the given range.");

    /**
     * Minimum and maximum to use.
     */
    double[] minmax = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleListParameter minmaxP = new DoubleListParameter(MINMAX_ID) //
          .setOptional(true) //
          .addConstraint(new ListSizeConstraint(2));
      if(config.grab(minmaxP)) {
        minmax = minmaxP.getValue().clone();
      }
    }

    @Override
    protected AddSingleScale makeInstance() {
      return new AddSingleScale(minmax);
    }
  }
}