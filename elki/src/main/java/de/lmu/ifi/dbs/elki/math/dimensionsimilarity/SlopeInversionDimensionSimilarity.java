package de.lmu.ifi.dbs.elki.math.dimensionsimilarity;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Arrange dimensions based on the entropy of the slope spectrum. In contrast to
 * {@link SlopeDimensionSimilarity}, we also take the option of inverting an
 * axis into account.
 *
 * Reference:
 * <p>
 * Elke Achtert, Hans-Peter Kriegel, Erich Schubert, Arthur Zimek:<br />
 * Interactive Data Mining with 3D-Parallel-Coordinate-Trees.<br />
 * Proceedings of the 2013 ACM International Conference on Management of Data
 * (SIGMOD), New York City, NY, 2013.
 * </p>
 *
 * TODO: shouldn't this be normalized by the single-dimension entropies or so?
 *
 * @author Erich Schubert
 * @author Robert Rödler
 * @since 0.5.5
 */
@Reference(authors = "Elke Achtert, Hans-Peter Kriegel, Erich Schubert, Arthur Zimek", //
title = "Interactive Data Mining with 3D-Parallel-Coordinate-Trees", //
booktitle = "Proc. of the 2013 ACM International Conference on Management of Data (SIGMOD)", //
url = "http://dx.doi.org/10.1145/2463676.2463696")
public class SlopeInversionDimensionSimilarity extends SlopeDimensionSimilarity {
  /**
   * Static instance.
   */
  public static final SlopeInversionDimensionSimilarity STATIC = new SlopeInversionDimensionSimilarity();

  /**
   * Constructor. Use static instance instead!
   */
  protected SlopeInversionDimensionSimilarity() {
    super();
  }

  @Override
  public void computeDimensionSimilarites(Relation<? extends NumberVector> relation, DBIDs subset, DimensionSimilarityMatrix matrix) {
    final int dim = matrix.size();
    final int size = subset.size();

    // Collect angular histograms.
    // Note, we only fill half of the matrix
    int[][][] angles = new int[dim][dim][PRECISION];
    int[][][] angleI = new int[dim][dim][PRECISION];

    // FIXME: Get/keep these statistics in the relation, or compute for the
    // sample only.
    double[] off, scale;
    {
      double[][] mm = RelationUtil.computeMinMax(relation);
      off = mm[0];
      scale = mm[1];
      for(int d = 0; d < dim; d++) {
        scale[d] -= off[d];
        scale[d] = (scale[d] > 0.) ? 1. / scale[d] : 1.;
      }
    }

    // Scratch buffer
    double[] vec = new double[dim];
    for(DBIDIter id = subset.iter(); id.valid(); id.advance()) {
      final NumberVector obj = relation.get(id);
      // Map values to 0..1
      for(int d = 0; d < dim; d++) {
        vec[d] = (obj.doubleValue(matrix.dim(d)) - off[d]) * scale[d];
      }
      for(int i = 0; i < dim - 1; i++) {
        for(int j = i + 1; j < dim; j++) {
          {
            // This will be on a scale of 0 .. 2:
            final double delta = vec[j] - vec[i] + 1;
            int div = (int) Math.round(delta * RESCALE);
            // TODO: do we really need this check?
            div = (div < 0) ? 0 : (div >= PRECISION) ? PRECISION - 1 : div;
            angles[i][j][div] += 1;
          }
          {
            // This will be on a scale of 0 .. 2:
            final double delta = vec[j] + vec[i];
            int div = (int) Math.round(delta * RESCALE);
            // TODO: do we really need this check?
            div = (div < 0) ? 0 : (div >= PRECISION) ? PRECISION - 1 : div;
            angleI[i][j][div] += 1;
          }
        }
      }
    }

    // Compute entropy in each combination:
    for(int x = 0; x < dim; x++) {
      for(int y = x + 1; y < dim; y++) {
        double entropy = 0., entropyI = 0;
        {
          int[] as = angles[x][y];
          for(int l = 0; l < PRECISION; l++) {
            if(as[l] > 0) {
              final double p = as[l] / (double) size;
              entropy += p * Math.log(p);
            }
          }
        }
        {
          int[] as = angleI[x][y];
          for(int l = 0; l < PRECISION; l++) {
            if(as[l] > 0) {
              final double p = as[l] / (double) size;
              entropyI += p * Math.log(p);
            }
          }
        }
        if(entropy >= entropyI) {
          entropy = 1 + entropy / LOG_PRECISION;
          matrix.set(x, y, entropy);
        }
        else {
          entropyI = 1 + entropyI / LOG_PRECISION;
          // Negative sign to indicate the axes might be inversely related
          matrix.set(x, y, -entropyI);
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
    @Override
    protected SlopeInversionDimensionSimilarity makeInstance() {
      return STATIC;
    }
  }
}
