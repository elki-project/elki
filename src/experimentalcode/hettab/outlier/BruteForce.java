package experimentalcode.hettab.outlier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Vector;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair;
import de.lmu.ifi.dbs.elki.utilities.pairs.SCPair;

/**
 * BruteForce provides a naive brute force algorithm in which all k-subsets of
 * dimensions are examined and calculates the sparsity coefficient to find
 * outliers
 * 
 * <p>
 * Reference: <br>
 * Outlier detection for high dimensional data Outlier detection for high
 * dimensional data <br>
 * International Conference on Management of Data Proceedings of the 2001 ACM
 * SIGMOD international conference on Management of data 2001, Santa Barbara,
 * California, United States
 * </p>
 * 
 * @author Ahmed Hettab
 * 
 */
@Title("BruteForce: Outlier detection for high dimensional data")
@Description("Examines all possible sets of k dimensional projections")
@Reference(authors = "C.C. Aggarwal, P. S. Yu", title = "Outlier detection for high dimensional data", booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD 2001), Santa Barbara, CA, 2001", url = "http://charuaggarwal.net/outl.pdf")
public class BruteForce<V extends DoubleVector> extends AbstractAlgorithm<V, OutlierResult> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(BruteForce.class);

  /**
   * OptionID for {@link #PHI_PARAM}
   */
  public static final OptionID PHI_ID = OptionID.getOrCreateOptionID("bf.phi", "the number of equi-depth ranges to use");

  /**
   * Parameter to specify the equi-depth ranges must be an integer greater than
   * 1.
   * <p>
   * Key: {@code -bf.phi}
   * </p>
   */
  private final IntParameter PHI_PARAM = new IntParameter(PHI_ID, new GreaterConstraint(1));

  /**
   * Holds the value of {@link #PHI_PARAM}.
   */
  private int phi;

  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("bf.k", "the dimensionality of projection");

  /**
   * Parameter to specify the dimensionality of projection must be an integer
   * greater than 1.
   * <p>
   * Key: {@code -eafod.k}
   * </p>
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(1));

  /**
   * Holds the value of {@link #K_PARAM}.
   */
  private int k;

  /**
   * The association id to associate the BF_SCORE of an object for the
   * BruteForce algorithm.
   */
  public static final AssociationID<Double> BF_SCORE = AssociationID.getOrCreateAssociationID("bf", Double.class);

  /**
   * Provides the BruteForce algorithm, adding parameters {@link #K_PARAM}
   * {@link #PHI_PARAM} to the option handler additionally to parameters of
   * super class.
   */
  public BruteForce(Parameterization config) {
    super();
    config = config.descend(this);
    if(config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }
    if(config.grab(PHI_PARAM)) {
      phi = PHI_PARAM.getValue();
    }
  }

  @Override
  protected OutlierResult runInTime(Database<V> database) throws IllegalStateException {
    final int dim = database.dimensionality();
    final int size = database.size();
    HashMap<Integer, HashMap<Integer, HashSetModifiableDBIDs>> ranges = new HashMap<Integer, HashMap<Integer, HashSetModifiableDBIDs>>();
    calculateDepth(database, ranges);

    //
    for(int i = 1; i <= dim; i++) {
      for(int j = 1; j <= phi; j++) {
        ArrayDBIDs list = DBIDUtil.newArray(ranges.get(i).get(j));
        MinMax<Double> minmax = new MinMax<Double>();
        for(int t = 0; t < list.size(); t++) {
          minmax.put(database.get(list.get(t)).getValue(i));
        }
        if(logger.isVerbose()) {
          logger.verbose("Dim : " + i + " depth : " + j);
          logger.verbose("Min :" + minmax.getMin());
          logger.verbose("Max :" + minmax.getMax());
        }
      }
    }
    HashMap<Integer, ArrayList<Vector<IntIntPair>>> subspaces = new HashMap<Integer, ArrayList<Vector<IntIntPair>>>();

    // Set of all dim*phi ranges
    ArrayList<Vector<IntIntPair>> q = new ArrayList<Vector<IntIntPair>>();

    for(int i = 1; i <= database.dimensionality(); i++) {
      for(int j = 1; j <= phi; j++) {
        Vector<IntIntPair> v = new Vector<IntIntPair>();
        v.add(new IntIntPair(i, j));
        q.add(v);
      }
    }
    subspaces.put(1, q);

    // calculate Ri
    for(int i = 2; i <= k; i++) {
      ArrayList<Vector<IntIntPair>> Ri = new ArrayList<Vector<IntIntPair>>();
      ArrayList<Vector<IntIntPair>> oldR = subspaces.get(i - 1);

      for(int j = 0; j < oldR.size(); j++) {
        Vector<IntIntPair> c = oldR.get(j);
        for(int l = 0; l < q.size(); l++) {
          int count = 0;
          Vector<IntIntPair> neu = new Vector<IntIntPair>(c);
          IntIntPair pair = q.get(l).get(0);
          for(int t = 0; t < neu.size(); t++) {
            if(neu.get(t).first == pair.first)
              count++;
          }
          if(count == 0) {
            neu.add(pair);
            Ri.add(neu);
          }
        }
      }
      subspaces.put(i, Ri);
    }

    WritableDataStore<Double> sparsity = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Double.class);
    MinMax<Double> minmax = new MinMax<Double>();
    // set Of all k-subspaces
    ArrayList<Vector<IntIntPair>> s = subspaces.get(k);

    // calculate the sparsity coefficient
    for(Vector<IntIntPair> sub : s) {
      double sparsityC = fitness(sub, size, ranges);
      DBIDs ids = getIDs(sub, ranges);

      for(DBID id : ids) {
        sparsity.put(id, sparsityC);
      }
      minmax.put(sparsityC);
    }

    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("BruteForce", "bruteforce-outlier", BF_SCORE, sparsity);
    OutlierScoreMeta meta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY);
    return new OutlierResult(meta, scoreResult);
  }

  /**
   * grid discretization of the data : <br>
   * each attribute of data is divided into phi equi-depth ranges . <br>
   * each range contains a fraction f=1/phi of the records .
   * 
   * @param database
   */
  public void calculateDepth(Database<V> database, HashMap<Integer, HashMap<Integer, HashSetModifiableDBIDs>> ranges) {
    int dim = database.dimensionality();
    int size = database.size();
    // sort dimension
    ArrayList<ArrayList<SCPair<DBID, Double>>> dbAxis = new ArrayList<ArrayList<SCPair<DBID, Double>>>(dim);

    HashSetModifiableDBIDs range = DBIDUtil.newHashSet();
    HashMap<Integer, HashSetModifiableDBIDs> rangesAt = new HashMap<Integer, HashSetModifiableDBIDs>();

    for(int i = 0; i < dim; i++) {
      ArrayList<SCPair<DBID, Double>> axis = new ArrayList<SCPair<DBID, Double>>(size);
      dbAxis.add(i, axis);
    }
    for(DBID id : database) {
      for(int d = 1; d <= database.dimensionality(); d++) {
        double value = database.get(id).getValue(d);
        SCPair<DBID, Double> point = new SCPair<DBID, Double>(id, value);
        dbAxis.get(d - 1).add(point);
      }
    }
    //
    for(int index = 0; index < database.dimensionality(); index++) {
      Collections.sort(dbAxis.get(index));
    }

    // equi-depth
    // if range = 0 => |range| = database.size();
    // if database.size()%phi == 0 |range|=database.size()/phi
    // if database.size()%phi == rest (1..rest => |range| =
    // database.size()/phi +1 , rest..phi => |range| = database.size()/phi
    int rest = database.size() % phi;
    int f = database.size() / phi;

    ModifiableDBIDs b = DBIDUtil.newHashSet();
    for(DBID id : database) {
      b.add(id);
    }
    // if range = 0 => |range| = database.size();
    for(int d = 1; d <= database.dimensionality(); d++) {
      rangesAt = new HashMap<Integer, HashSetModifiableDBIDs>();
      ranges.put(d, rangesAt);
    }

    for(int d = 1; d <= database.dimensionality(); d++) {
      ArrayList<SCPair<DBID, Double>> axis = dbAxis.get(d - 1);

      for(int i = 0; i < rest; i++) {
        // 1..rest => |range| = database.size()/phi +1
        range = DBIDUtil.newHashSet();
        for(int j = i * f + i; j < (i + 1) * f + i + 1; j++) {
          range.add(axis.get(j).getFirst());
        }
        ranges.get(d).put(i + 1, range);
      }

      // rest..phi => |range| = database.size()/phi
      for(int i = rest; i < phi; i++) {
        range = DBIDUtil.newHashSet();
        for(int j = i * f + rest; j < (i + 1) * f + rest; j++) {
          range.add(axis.get(j).getFirst());
        }
        ranges.get(d).put(i + 1, range);
      }

    }

  }

  /**
   * Method to calculate the sparsity coefficient of
   * 
   * @param subspace
   * @return sparsity coefficient
   */
  public double fitness(Vector<IntIntPair> subspace, int size, HashMap<Integer, HashMap<Integer, HashSetModifiableDBIDs>> ranges) {
    ModifiableDBIDs ids = DBIDUtil.newHashSet(ranges.get(subspace.get(0).getFirst()).get(subspace.get(0).getSecond()));

    // intersect
    for(int i = 1; i < subspace.size(); i++) {
      HashSetModifiableDBIDs current = ranges.get(subspace.get(i).getFirst()).get(subspace.get(i).getSecond());
      HashSetModifiableDBIDs result = EAFOD.retainAll(current, ids);
      ids.clear();
      ids.addDBIDs(result);
    }
    // calculate sparsity c
    double f = (double) 1 / phi;
    double nD = ids.size();
    double fK = Math.pow(f, k);
    double sC = (nD - (size * fK)) / Math.sqrt(size * fK * (1 - fK));
    return sC;
  }

  /**
   * Method to get the ids in the given subspace
   * 
   * @param subspace
   * @return ids
   */
  public DBIDs getIDs(Vector<IntIntPair> subspace, HashMap<Integer, HashMap<Integer, HashSetModifiableDBIDs>> ranges) {
    HashSetModifiableDBIDs ids = DBIDUtil.newHashSet(ranges.get(subspace.get(0).getFirst()).get(subspace.get(0).getSecond()));
    // intersect
    for(int i = 1; i < subspace.size(); i++) {
      HashSetModifiableDBIDs current = ranges.get(subspace.get(i).getFirst()).get(subspace.get(i).getSecond());
      HashSetModifiableDBIDs result = EAFOD.retainAll(current, ids);
      ids.clear();
      ids.addDBIDs(result);
    }
    return ids;
  }

  /**
   * return a presentation string of
   * 
   * @param subspace
   * @return presentation string
   */
  public String printSubspace(Vector<IntIntPair> subspace) {
    String s = "Subspace :";
    for(IntIntPair pair : subspace) {
      s = s + " Dim:" + pair.first + " phi:" + pair.second + " ";
    }
    return s;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}