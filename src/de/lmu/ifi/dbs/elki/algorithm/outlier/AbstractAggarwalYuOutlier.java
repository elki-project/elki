package de.lmu.ifi.dbs.elki.algorithm.outlier;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
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
public abstract class AbstractAggarwalYuOutlier<V extends NumberVector<?, ?>> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * OptionID for the grid size
   */
  public static final OptionID PHI_ID = OptionID.getOrCreateOptionID("ay.phi", "The number of equi-depth grid ranges to use in each dimension.");

  /**
   * OptionID for the target dimensionality
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("ay.k", "Subspace dimensionality to search for.");

  /**
   * Symbolic value for subspaces not in use.
   * 
   * Note: in some places, the implementations may rely on this having the value
   * 0 currently!
   */
  public static final int DONT_CARE = 0;

  /**
   * The number of partitions for each dimension
   */
  protected int phi;

  /**
   * The target dimensionality.
   */
  protected int k;

  /**
   * Constructor.
   * 
   * @param k K parameter
   * @param phi Phi parameter
   */
  public AbstractAggarwalYuOutlier(int k, int phi) {
    super();
    this.k = k;
    this.phi = phi;
  }

  /**
   * Grid discretization of the data:<br />
   * Each attribute of data is divided into phi equi-depth ranges.<br />
   * Each range contains a fraction f=1/phi of the records.
   * 
   * @param database
   * @return range map
   */
  protected ArrayList<ArrayList<DBIDs>> buildRanges(Relation<V> database) {
    final int dim = DatabaseUtil.dimensionality(database);
    final int size = database.size();
    final DBIDs allids = database.getDBIDs();
    final ArrayList<ArrayList<DBIDs>> ranges = new ArrayList<ArrayList<DBIDs>>();

    // Temporary projection storage of the database
    final ArrayList<ArrayList<FCPair<Double, DBID>>> dbAxis = new ArrayList<ArrayList<FCPair<Double, DBID>>>(dim);
    for(int i = 0; i < dim; i++) {
      ArrayList<FCPair<Double, DBID>> axis = new ArrayList<FCPair<Double, DBID>>(size);
      dbAxis.add(i, axis);
    }
    // Project
    for(DBID id : allids) {
      final V obj = database.get(id);
      for(int d = 1; d <= dim; d++) {
        dbAxis.get(d - 1).add(new FCPair<Double, DBID>(obj.doubleValue(d), id));
      }
    }
    // Split into cells
    final double part = size * 1.0 / phi;
    for(int d = 1; d <= dim; d++) {
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
        ArrayModifiableDBIDs currange = DBIDUtil.newArray(phi + 1);
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
    HashSetModifiableDBIDs ids = DBIDUtil.newHashSet(ranges.get(subspace.get(0).first - 1).get(subspace.get(0).second));
    // intersect all selected dimensions
    for(int i = 1; i < subspace.size(); i++) {
      DBIDs current = ranges.get(subspace.get(i).first - 1).get(subspace.get(i).second);
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

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static abstract class Parameterizer extends AbstractParameterizer {
    protected Integer phi;

    protected Integer k;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter kP = new IntParameter(K_ID, new GreaterEqualConstraint(2));
      if(config.grab(kP)) {
        k = kP.getValue();
      }
      final IntParameter phiP = new IntParameter(PHI_ID, new GreaterEqualConstraint(2));
      if(config.grab(phiP)) {
        phi = phiP.getValue();
      }
    }
  }
}