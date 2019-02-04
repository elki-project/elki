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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.xtree;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.overflow.LimitedReinsertOverflowTreatment;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.reinsert.CloseReinsert;
import de.lmu.ifi.dbs.elki.persistent.PageFileFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;

/**
 * Factory class for XTree.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 * 
 * @param <O> object type
 * @param <N> node type
 */
public abstract class AbstractXTreeFactory<O extends NumberVector, N extends AbstractXTreeNode<N>> extends AbstractRStarTreeFactory<O, N, SpatialEntry, XTreeSettings> {
  /**
   * Constructor.
   * 
   * @param pageFileFactory Data storage
   * @param settings Tree settings
   */
  public AbstractXTreeFactory(PageFileFactory<?> pageFileFactory, XTreeSettings settings) {
    super(pageFileFactory, settings);
  }

  /**
   * Parameterizable API.
   * 
   * @author Erich Schubert
   * 
   * @param <O> object type
   */
  public abstract static class Parameterizer<O extends NumberVector> extends AbstractRStarTreeFactory.Parameterizer<O, XTreeSettings> {
    /**
     * Parameter for minimum number of entries per directory page when going for
     * a minimum overlap split; defaults to <code>.3</code> times the number of
     * maximum entries.
     */
    public static final OptionID MIN_FANOUT_ID = new OptionID("xtree.min_fanout_fraction", "The fraction (in [0,1]) of maximally allowed directory page entries which is to be tolerated as minimum number of directory page entries for minimum overlap splits");

    /**
     * Parameter for the number of re-insertions to be performed instead of
     * doing a split; defaults to <code>.3</code> times the number of maximum
     * entries.
     */
    public static final OptionID REINSERT_ID = new OptionID("xtree.reinsert_fraction", "The fraction (in [0,1]) of entries to be reinserted instead of performing a split");

    /**
     * Parameter for the maximally allowed overlap. Defaults to <code>.2</code>.
     */
    public static final OptionID MAX_OVERLAP_ID = new OptionID("xtree.max_overlap_fraction", "The fraction (in [0,1]) of allowed entry overlaps. Overlap type specified in xtree.overlap_type");

    /**
     * Parameter for defining the overlap type to be used for the maximum
     * overlap test. Available options:
     * <dl>
     * <dt><code>DataOverlap</code></dt>
     * <dd>The overlap is the ratio of total data objects in the overlapping
     * region.</dd>
     * <dt><code>VolumeOverlap</code></dt>
     * <dd>The overlap is the fraction of the overlapping region of the two
     * original mbrs:<br>
     * <code>(overlap volume of mbr 1 and mbr 2) / (volume of mbr 1 + volume of mbr 2)</code>
     * <br>
     * This option is faster than <code>DataOverlap</code>, however, it may
     * result in a tree structure which is not optimally adapted to the indexed
     * data.</dd>
     * </dl>
     * Defaults to <code>VolumeOverlap</code>.
     */
    public static final OptionID OVERLAP_TYPE_ID = new OptionID("xtree.overlap_type", "How to calculate the maximum overlap? Options: \"DataOverlap\" = {ratio of data objects in the overlapping region}, \"VolumeOverlap\" = {(overlap volume) / (volume 1 + volume 2)}");

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Bulk loads are not supported yet:
      // super.configBulkLoad(config);
      final DoubleParameter MIN_FANOUT_PARAMETER = new DoubleParameter(MIN_FANOUT_ID, 0.3);
      MIN_FANOUT_PARAMETER.addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      MIN_FANOUT_PARAMETER.addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE);
      if(config.grab(MIN_FANOUT_PARAMETER)) {
        settings.relativeMinFanout = MIN_FANOUT_PARAMETER.getValue();
      }
      final DoubleParameter REINSERT_PARAMETER = new DoubleParameter(REINSERT_ID, 0.3);
      REINSERT_PARAMETER.addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      REINSERT_PARAMETER.addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE);
      if(config.grab(REINSERT_PARAMETER)) {
        float reinsert_fraction = REINSERT_PARAMETER.getValue().floatValue();
        settings.setOverflowTreatment(new LimitedReinsertOverflowTreatment(new CloseReinsert(reinsert_fraction, SquaredEuclideanDistanceFunction.STATIC)));
      }
      final DoubleParameter MAX_OVERLAP_PARAMETER = new DoubleParameter(MAX_OVERLAP_ID, 0.2);
      MAX_OVERLAP_PARAMETER.addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      MAX_OVERLAP_PARAMETER.addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE);
      if(config.grab(MAX_OVERLAP_PARAMETER)) {
        settings.max_overlap = MAX_OVERLAP_PARAMETER.getValue().floatValue();
      }
      final EnumParameter<XTreeSettings.Overlap> OVERLAP_TYPE_PARAMETER = new EnumParameter<>(OVERLAP_TYPE_ID, XTreeSettings.Overlap.class, XTreeSettings.Overlap.VOLUME_OVERLAP);
      if(config.grab(OVERLAP_TYPE_PARAMETER)) {
        settings.overlap_type = OVERLAP_TYPE_PARAMETER.getValue();
      }
    }

    @Override
    protected XTreeSettings createSettings() {
      return new XTreeSettings();
    }
  }
}
