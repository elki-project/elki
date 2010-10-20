package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.FCPair;
import de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair;

/**
 * Abstract base class for the sparse-grid-cell based outlier detection of
 * Aggarwal and Yu.
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
@Reference(authors = "C.C. Aggarwal, P. S. Yu", title = "Outlier detection for high dimensional data", booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD 2001), Santa Barbara, CA, 2001", url = "http://dx.doi.org/10.1145/375663.375668")
public abstract class AbstractAggarwalYuOutlier<V extends NumberVector<?, ?>> extends AbstractAlgorithm<V, OutlierResult> {
  /**
   * The number of partitions for each dimension
   */
  protected int phi;

  /**
   * Holds the value of {@link #K_PARAM}.
   */
  protected int k;

  /**
   * OptionID for {@link #PHI_PARAM}
   */
  public static final OptionID PHI_ID = OptionID.getOrCreateOptionID("ay.phi", "The number of equi-depth grid ranges to use in each dimension.");

  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("ay.k", "Subspace dimensionality to search for.");

  /**
   * Symbolic value for subspaces not in use.
   * 
   * Note: in some places, the implementations may rely on this having the value 0 currently!
   */
  public static final int DONT_CARE = 0;

  /**
   * The association id to associate the AGGARWAL_YU_SCORE of an object for the
   * BruteForce algorithm.
   */
  public static final AssociationID<Double> AGGARWAL_YU_SCORE = AssociationID.getOrCreateAssociationID("aggarwal-yu", Double.class);

  /**
   * Constructor, Parameterizable style.
   * 
   * @param config Parameterization
   */
  public AbstractAggarwalYuOutlier(Parameterization config) {
    super();
    config = config.descend(this);
    final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterEqualConstraint(2));
    if(config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }
    final IntParameter PHI_PARAM = new IntParameter(PHI_ID, new GreaterEqualConstraint(2));
    if(config.grab(PHI_PARAM)) {
      phi = PHI_PARAM.getValue();
    }
  }

  /**
   * Grid discretization of the data:<br />
   * Each attribute of data is divided into phi equi-depth ranges.<br />
   * Each range contains a fraction f=1/phi of the records.
   * 
   * @param database
   * @return range map
   */
  protected ArrayList<ArrayList<DBIDs>> buildRanges(Database<V> database) {
    final int dim = database.dimensionality();
    final int size = database.size();
    final DBIDs allids = database.getIDs();
    final ArrayList<ArrayList<DBIDs>> ranges = new ArrayList<ArrayList<DBIDs>>();

    // Temporary projection storage of the database
    final ArrayList<ArrayList<FCPair<Double, DBID>>> dbAxis = new ArrayList<ArrayList<FCPair<Double, DBID>>>(dim);
    for(int i = 0; i < dim; i++) {
      ArrayList<FCPair<Double, DBID>> axis = new ArrayList<FCPair<Double, DBID>>(size);
      dbAxis.add(i, axis);
    }
    // Project
    for(DBID id : allids) {
      for(int d = 1; d <= database.dimensionality(); d++) {
        double value = database.get(id).doubleValue(d);
        FCPair<Double, DBID> point = new FCPair<Double, DBID>(value, id);
        dbAxis.get(d - 1).add(point);
      }
    }
    // Split into cells
    final double part = size * 1.0 / phi;
    for(int d = 1; d <= database.dimensionality(); d++) {
      ArrayList<FCPair<Double, DBID>> axis = dbAxis.get(d - 1);
      Collections.sort(axis);
      ArrayList<DBIDs> dimranges = new ArrayList<DBIDs>(phi + 1);
      dimranges.add(allids);
      int start = 0;
      for(int r = 0; r < phi; r++) {
        int end = (int) (part * r);
        if(r == phi - 1) {
          end = size;
        }
        ArrayDBIDs currange = DBIDUtil.newArray(phi + 1);
        for(int i = start; i < end; i++) {
          currange.add(axis.get(i).second);
        }
        start = end;
        dimranges.add(currange);
      }
      ranges.add(dimranges);
    }
    return ranges;
  }

  /**
   * Method to calculate the sparsity coefficient of
   * 
   * @param setsize Size of subset
   * @param dbsize Size of database
   * @param k Dimensionality
   * @return sparsity coefficient
   */
  protected double sparsity(final int setsize, final int dbsize, final int k) {
    // calculate sparsity c
    final double f = 1. / phi;
    final double fK = Math.pow(f, k);
    final double sC = (setsize - (dbsize * fK)) / Math.sqrt(dbsize * fK * (1 - fK));
    return sC;
  }

  /**
   * Method to get the ids in the given subspace
   * 
   * @param subspace
   * @return ids
   */
  protected DBIDs computeSubspace(Vector<IntIntPair> subspace, ArrayList<ArrayList<DBIDs>> ranges) {
    HashSetModifiableDBIDs ids = DBIDUtil.newHashSet(ranges.get(subspace.get(0).getFirst() - 1).get(subspace.get(0).getSecond()));
    // intersect all selected dimensions
    for(int i = 1; i < subspace.size(); i++) {
      DBIDs current = ranges.get(subspace.get(i).getFirst() - 1).get(subspace.get(i).getSecond());
      ids.retainAll(current);
      if(ids.size() == 0) {
        break;
      }
    }
    return ids;
  }

  /**
   * Get the DBIDs in the current subspace.
   * 
   * @param gene gene data
   * @param ranges Database ranges
   * @return resulting DBIDs
   */
  protected DBIDs computeSubspaceForGene(int[] gene, ArrayList<ArrayList<DBIDs>> ranges) {
    HashSetModifiableDBIDs m = DBIDUtil.newHashSet(ranges.get(0).get(gene[0]));
    // intersect
    for(int i = 1; i < gene.length; i++) {
      if(gene[i] != DONT_CARE) {
        DBIDs current = ranges.get(i).get(gene[i]);
        m.retainAll(current);
      }
    }
    return m;
  }

}