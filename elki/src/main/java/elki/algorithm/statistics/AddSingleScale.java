/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.algorithm.statistics;

import elki.Algorithm;
import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.ids.DBIDIter;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.math.DoubleMinMax;
import elki.math.scales.LinearScale;
import elki.result.ResultUtil;
import elki.result.ScalesResult;
import elki.utilities.documentation.Description;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.ListSizeConstraint;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleListParameter;

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

  /**
   * Run the algorithm on all vector relations of a database.
   * 
   * @param database Database
   * @return Empty (scales are attached to the relations)
   */
  public Void run(Database database) {
    for(Relation<?> rel : database.getRelations()) {
      if(TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        @SuppressWarnings("unchecked")
        Relation<? extends NumberVector> vrel = (Relation<? extends NumberVector>) rel;
        ResultUtil.addChildResult(rel, run(vrel));
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
  public static class Par implements Parameterizer {
    /**
     * Minimum and maximum values.
     */
    public static final OptionID MINMAX_ID = new OptionID("scales.minmax", "Forcibly set the scales to the given range.");

    /**
     * Minimum and maximum to use.
     */
    double[] minmax = null;

    @Override
    public void configure(Parameterization config) {
      new DoubleListParameter(MINMAX_ID) //
          .setOptional(true) //
          .addConstraint(new ListSizeConstraint(2)) //
          .grab(config, x -> minmax = x.clone());
    }

    @Override
    public AddSingleScale make() {
      return new AddSingleScale(minmax);
    }
  }
}