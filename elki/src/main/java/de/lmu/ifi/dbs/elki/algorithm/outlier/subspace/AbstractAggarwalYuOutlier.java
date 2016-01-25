package de.lmu.ifi.dbs.elki.algorithm.outlier.subspace;

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

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.VectorUtil.SortDBIDsBySingleDimension;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair;

/**
 * Abstract base class for the sparse-grid-cell based outlier detection of
 * Aggarwal and Yu.
 * 
 * Reference:
 * <p>
 * Outlier detection for high dimensional data<br />
 * C.C. Aggarwal, P. S. Yu<br />
 * International Conference on Management of Data Proceedings of the 2001 ACM
 * SIGMOD international conference on Management of data 2001, Santa Barbara,
 * California, United States
 * </p>
 * 
 * @author Ahmed Hettab
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @param <V> Vector type
 */
@Reference(authors = "C.C. Aggarwal, P. S. Yu", //
title = "Outlier detection for high dimensional data", //
booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD 2001), Santa Barbara, CA, 2001", //
url = "http://dx.doi.org/10.1145/375663.375668")
public abstract class AbstractAggarwalYuOutlier<V extends NumberVector> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * Symbolic value for subspaces not in use.
   */
  public static final short DONT_CARE = -1;

  /**
   * The first bucket.
   */
  public static final short GENE_OFFSET = DONT_CARE + 1;

  /**
   * The number of partitions for each dimension.
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
   * @param relation Relation to process
   * @return range map
   */
  protected ArrayList<ArrayList<DBIDs>> buildRanges(Relation<V> relation) {
    final int dim = RelationUtil.dimensionality(relation);
    final int size = relation.size();
    final ArrayList<ArrayList<DBIDs>> ranges = new ArrayList<>();

    ArrayModifiableDBIDs ids = DBIDUtil.newArray(relation.getDBIDs());
    SortDBIDsBySingleDimension sorter = new SortDBIDsBySingleDimension(relation);
    // Split into cells
    final double part = size * 1.0 / phi;
    for(int d = 0; d < dim; d++) {
      sorter.setDimension(d);
      ids.sort(sorter);
      ArrayList<DBIDs> dimranges = new ArrayList<>(phi + 1);
      int start = 0;
      DBIDArrayIter iter = ids.iter();
      for(int r = 1; r <= phi; r++) {
        int end = (r < phi) ? (int) (part * r) : size;
        ArrayModifiableDBIDs currange = DBIDUtil.newArray(end - start);
        for(iter.seek(start); iter.getOffset() < end; iter.advance()) {
          currange.add(iter);
        }
        start = end;
        dimranges.add(currange);
      }
      ranges.add(dimranges);
    }
    return ranges;
  }

  /**
   * Method to calculate the sparsity coefficient of.
   * 
   * @param setsize Size of subset
   * @param dbsize Size of database
   * @param k Dimensionality
   * @param phi Phi parameter
   * @return sparsity coefficient
   */
  protected static double sparsity(final int setsize, final int dbsize, final int k, final double phi) {
    // calculate sparsity c
    final double f = 1. / phi;
    final double fK = MathUtil.powi(f, k);
    final double sC = (setsize - (dbsize * fK)) / Math.sqrt(dbsize * fK * (1 - fK));
    return sC;
  }

  /**
   * Method to get the ids in the given subspace.
   * 
   * @param subspace Subspace to process
   * @param ranges List of DBID ranges
   * @return ids
   */
  protected DBIDs computeSubspace(ArrayList<IntIntPair> subspace, ArrayList<ArrayList<DBIDs>> ranges) {
    HashSetModifiableDBIDs ids = DBIDUtil.newHashSet(ranges.get(subspace.get(0).first).get(subspace.get(0).second));
    // intersect all selected dimensions
    for(int i = 1; i < subspace.size(); i++) {
      DBIDs current = ranges.get(subspace.get(i).first).get(subspace.get(i).second - GENE_OFFSET);
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
  protected DBIDs computeSubspaceForGene(short[] gene, ArrayList<ArrayList<DBIDs>> ranges) {
    HashSetModifiableDBIDs m = null;
    // intersect all present restrictions
    for(int i = 0; i < gene.length; i++) {
      if(gene[i] != DONT_CARE) {
        DBIDs current = ranges.get(i).get(gene[i] - GENE_OFFSET);
        if(m == null) {
          m = DBIDUtil.newHashSet(current);
        }
        else {
          m.retainAll(current);
        }
      }
    }
    assert (m != null) : "All genes set to '*', should not happen!";
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
  public abstract static class Parameterizer extends AbstractParameterizer {
    /**
     * OptionID for the grid size.
     */
    public static final OptionID PHI_ID = new OptionID("ay.phi", "The number of equi-depth grid ranges to use in each dimension.");

    /**
     * OptionID for the target dimensionality.
     */
    public static final OptionID K_ID = new OptionID("ay.k", "Subspace dimensionality to search for.");

    /**
     * Phi parameter.
     */
    protected int phi;

    /**
     * k Parameter.
     */
    protected int k;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter kP = new IntParameter(K_ID)//
      .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if(config.grab(kP)) {
        k = kP.getValue();
      }
      final IntParameter phiP = new IntParameter(PHI_ID)//
      .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if(config.grab(phiP)) {
        phi = phiP.getValue();
      }
    }
  }
}