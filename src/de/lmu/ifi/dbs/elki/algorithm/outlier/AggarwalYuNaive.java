package de.lmu.ifi.dbs.elki.algorithm.outlier;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.ArrayList;
import java.util.Vector;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.InvertedOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair;

/**
 * BruteForce provides a naive brute force algorithm in which all k-subsets of
 * dimensions are examined and calculates the sparsity coefficient to find
 * outliers.
 * 
 * The evolutionary approach is implemented as
 * {@link de.lmu.ifi.dbs.elki.algorithm.outlier.AggarwalYuEvolutionary}.
 * 
 * <p>
 * Reference: <br />
 * Outlier detection for high dimensional data<br />
 * C.C. Aggarwal, P. S. Yu<br />
 * International Conference on Management of Data Proceedings of the 2001 ACM
 * SIGMOD international conference on Management of data 2001, Santa Barbara,
 * California, United States
 * </p>
 * 
 * @author Ahmed Hettab
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
// TODO: progress logging!
@Title("BruteForce: Outlier detection for high dimensional data")
@Description("Examines all possible sets of k dimensional projections")
@Reference(authors = "C.C. Aggarwal, P. S. Yu", title = "Outlier detection for high dimensional data", booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD 2001), Santa Barbara, CA, 2001", url = "http://dx.doi.org/10.1145/375663.375668")
public class AggarwalYuNaive<V extends NumberVector<?, ?>> extends AbstractAggarwalYuOutlier<V> {
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
    final int dimensionality = DatabaseUtil.dimensionality(relation);
    final int size = relation.size();
    ArrayList<ArrayList<DBIDs>> ranges = buildRanges(relation);

    ArrayList<Vector<IntIntPair>> Rk;
    // Build a list of all subspaces
    {
      // R1 initial one-dimensional subspaces.
      Rk = new ArrayList<Vector<IntIntPair>>();
      // Set of all dim*phi ranges
      ArrayList<IntIntPair> q = new ArrayList<IntIntPair>();
      for(int i = 1; i <= dimensionality; i++) {
        for(int j = 1; j <= phi; j++) {
          IntIntPair s = new IntIntPair(i, j);
          q.add(s);
          // Add to first Rk
          Vector<IntIntPair> v = new Vector<IntIntPair>();
          v.add(s);
          Rk.add(v);
        }
      }

      // build Ri
      for(int i = 2; i <= k; i++) {
        ArrayList<Vector<IntIntPair>> Rnew = new ArrayList<Vector<IntIntPair>>();

        for(int j = 0; j < Rk.size(); j++) {
          Vector<IntIntPair> c = Rk.get(j);
          for(IntIntPair pair : q) {
            boolean invalid = false;
            for(int t = 0; t < c.size(); t++) {
              if(c.get(t).first == pair.first) {
                invalid = true;
                break;
              }
            }
            if(!invalid) {
              Vector<IntIntPair> neu = new Vector<IntIntPair>(c);
              neu.add(pair);
              Rnew.add(neu);
            }
          }
        }
        Rk = Rnew;
      }
    }

    WritableDoubleDataStore sparsity = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC);
    // calculate the sparsity coefficient
    for(Vector<IntIntPair> sub : Rk) {
      DBIDs ids = computeSubspace(sub, ranges);
      final double sparsityC = sparsity(ids.size(), size, k, phi);

      if(sparsityC < 0) {
        for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
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
    Relation<Double> scoreResult = new MaterializedRelation<Double>("AggarwalYuNaive", "aggarwal-yu-outlier", TypeUtil.DOUBLE, sparsity, relation.getDBIDs());
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
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<?, ?>> extends AbstractAggarwalYuOutlier.Parameterizer {
    @Override
    protected AggarwalYuNaive<V> makeInstance() {
      return new AggarwalYuNaive<V>(k, phi);
    }
  }
}