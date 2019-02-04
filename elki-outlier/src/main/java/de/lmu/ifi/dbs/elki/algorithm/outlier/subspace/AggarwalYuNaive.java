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
package de.lmu.ifi.dbs.elki.algorithm.outlier.subspace;

import java.util.ArrayList;
import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.InvertedOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair;

/**
 * BruteForce variant of the high-dimensional outlier detection algorithm by
 * Aggarwal and Yu.
 * <p>
 * The evolutionary approach is implemented as
 * {@link de.lmu.ifi.dbs.elki.algorithm.outlier.subspace.AggarwalYuEvolutionary}
 * <p>
 * Reference:
 * <p>
 * Outlier detection for high dimensional data<br>
 * C. C. Aggarwal, P. S. Yu<br>
 * Proc. 2001 ACM SIGMOD international conference on Management of data
 * 
 * @author Ahmed Hettab
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @param <V> Vector type
 */
@Title("BruteForce: Outlier detection for high dimensional data")
@Description("Examines all possible sets of k dimensional projections")
@Reference(authors = "C. C. Aggarwal, P. S. Yu", //
    title = "Outlier detection for high dimensional data", //
    booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD 2001)", //
    url = "https://doi.org/10.1145/375663.375668", //
    bibkey = "DBLP:conf/sigmod/AggarwalY01")
@Alias("de.lmu.ifi.dbs.elki.algorithm.outlier.AggarwalYuNaive")
public class AggarwalYuNaive<V extends NumberVector> extends AbstractAggarwalYuOutlier<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(AggarwalYuNaive.class);

  /**
   * Constructor.
   * 
   * @param k K
   * @param phi Phi
   */
  public AggarwalYuNaive(int k, int phi) {
    super(k, phi);
  }

  /**
   * Run the algorithm on the given relation.
   * 
   * @param relation Relation
   * @return Outlier detection result
   */
  public OutlierResult run(Relation<V> relation) {
    final int dim = RelationUtil.dimensionality(relation);
    final int size = relation.size();
    ArrayList<ArrayList<DBIDs>> ranges = buildRanges(relation);

    ArrayList<int[]> Rk;
    // Build a list of all subspaces
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Subspace size", k, LOG) : null;
    // R1 initial one-dimensional subspaces.
    Rk = new ArrayList<>();
    // Set of all dim*phi ranges
    ArrayList<IntIntPair> q = new ArrayList<>();
    for(int i = 0; i < dim; i++) {
      for(int j = 0; j < phi; j++) {
        q.add(new IntIntPair(i, j));
        Rk.add(new int[] { i, j });
      }
    }
    LOG.incrementProcessed(prog);

    // build Ri
    for(int i = 2; i <= k; i++) {
      ArrayList<int[]> Rnew = new ArrayList<>();

      for(int j = 0; j < Rk.size(); j++) {
        int[] c = Rk.get(j);
        for(IntIntPair pair : q) {
          boolean invalid = false;
          for(int t = 0; t < c.length; t += 2) {
            if(c[t] == pair.first) {
              invalid = true;
              break;
            }
          }
          if(!invalid) {
            int[] neu = Arrays.copyOf(c, c.length + 2);
            neu[c.length] = pair.first;
            neu[c.length + 1] = pair.second;
            Rnew.add(neu);
          }
        }
      }
      Rk = Rnew;
      LOG.incrementProcessed(prog);
    }
    if(prog != null) {
      prog.setProcessed(k, LOG);
    }
    LOG.ensureCompleted(prog);

    WritableDoubleDataStore sparsity = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC);
    // calculate the sparsity coefficient
    for(int[] sub : Rk) {
      DBIDs ids = computeSubspace(sub, ranges);
      final double sparsityC = sparsity(ids.size(), size, k, phi);

      if(sparsityC < 0) {
        for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
          double prev = sparsity.doubleValue(iter);
          if(Double.isNaN(prev) || sparsityC < prev) {
            sparsity.putDouble(iter, sparsityC);
          }
        }
      }
    }
    DoubleMinMax minmax = new DoubleMinMax();
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double val = sparsity.doubleValue(iditer);
      if(Double.isNaN(val)) {
        sparsity.putDouble(iditer, 0.0);
        val = 0.0;
      }
      minmax.put(val);
    }
    DoubleRelation scoreResult = new MaterializedDoubleRelation("AggarwalYuNaive", "aggarwal-yu-outlier", sparsity, relation.getDBIDs());
    OutlierScoreMeta meta = new InvertedOutlierScoreMeta(minmax.getMin(), minmax.getMax(), Double.NEGATIVE_INFINITY, 0.0);
    return new OutlierResult(meta, scoreResult);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractAggarwalYuOutlier.Parameterizer {
    @Override
    protected AggarwalYuNaive<V> makeInstance() {
      return new AggarwalYuNaive<>(k, phi);
    }
  }
}
