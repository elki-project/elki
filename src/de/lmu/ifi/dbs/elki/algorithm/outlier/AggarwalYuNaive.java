package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.ArrayList;
import java.util.Vector;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
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
 * Outlier detection for high dimensional data Outlier detection for high
 * dimensional data <br />
 * C.C. Aggarwal, P. S. Yu<br />
 * International Conference on Management of Data Proceedings of the 2001 ACM
 * SIGMOD international conference on Management of data 2001, Santa Barbara,
 * California, United States
 * </p>
 * 
 * @author Ahmed Hettab
 * @author Erich Schubert
 */
// TODO: progress logging!
@Title("BruteForce: Outlier detection for high dimensional data")
@Description("Examines all possible sets of k dimensional projections")
@Reference(authors = "C.C. Aggarwal, P. S. Yu", title = "Outlier detection for high dimensional data", booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD 2001), Santa Barbara, CA, 2001", url = "http://dx.doi.org/10.1145/375663.375668")
public class AggarwalYuNaive<V extends NumberVector<?, ?>> extends AbstractAggarwalYuOutlier<V> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(AggarwalYuNaive.class);

  /**
   * Constructor.
   * 
   * @param k K
   * @param phi Phi
   */
  public AggarwalYuNaive(int k, int phi) {
    super(k, phi);
  }

  @Override
  public OutlierResult run(Database database) throws IllegalStateException {
    Relation<V> dataQuery = getRelation(database);
    final int size = dataQuery.size();
    ArrayList<ArrayList<DBIDs>> ranges = buildRanges(dataQuery);

    ArrayList<Vector<IntIntPair>> Rk;
    // Build a list of all subspaces
    {
      // R1 initial one-dimensional subspaces.
      Rk = new ArrayList<Vector<IntIntPair>>();
      // Set of all dim*phi ranges
      ArrayList<IntIntPair> q = new ArrayList<IntIntPair>();
      for(int i = 1; i <= DatabaseUtil.dimensionality(dataQuery); i++) {
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

    WritableDataStore<Double> sparsity = DataStoreUtil.makeStorage(database.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Double.class);
    // calculate the sparsity coefficient
    for(Vector<IntIntPair> sub : Rk) {
      DBIDs ids = computeSubspace(sub, ranges);
      final double sparsityC = sparsity(ids.size(), size, k);

      if(sparsityC < 0) {
        for(DBID id : ids) {
          Double prev = sparsity.get(id);
          if(prev == null || sparsityC < prev) {
            sparsity.put(id, sparsityC);
          }
        }
      }
    }
    DoubleMinMax minmax = new DoubleMinMax();
    for(DBID id : dataQuery.iterDBIDs()) {
      Double val = sparsity.get(id);
      if(val == null) {
        sparsity.put(id, 0.0);
        val = 0.0;
      }
      minmax.put(val);
    }
    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("AggarwalYuNaive", "aggarwal-yu-outlier", AGGARWAL_YU_SCORE, sparsity);
    OutlierScoreMeta meta = new InvertedOutlierScoreMeta(minmax.getMin(), minmax.getMax(), Double.NEGATIVE_INFINITY, 0.0);
    return new OutlierResult(meta, scoreResult);
  }

  @Override
  protected Logging getLogger() {
    return logger;
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