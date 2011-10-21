package experimentalcode.shared.index.xtree;
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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.bulk.BulkSplit;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.insert.InsertionStrategy;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.EqualStringConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint.IntervalBoundary;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;

/**
 * Factory class for XTree.
 * 
 * @author Erich Schubert
 * 
 * @param <O> object type
 * @param <X> actual tree type
 */
public abstract class XTreeBaseFactory<O extends NumberVector<O, ?>, N extends XNode<E, N>, E extends SpatialEntry, X extends AbstractRStarTree<N, E> & Index> extends AbstractRStarTreeFactory<O, N, E, X> {
  protected double relativeMinEntries;

  protected double relativeMinFanout;

  protected float reinsert_fraction;

  protected float max_overlap;

  protected int overlap_type;

  /**
   * Constructor.
   * 
   * @param fileName
   * @param pageSize
   * @param cacheSize
   * @param bulkSplitter bulk loading strategy
   * @param insertionStrategy
   * @param relativeMinEntries
   * @param relativeMinFanout
   * @param reinsert_fraction
   * @param max_overlap
   * @param overlap_type
   */
  public XTreeBaseFactory(String fileName, int pageSize, long cacheSize, BulkSplit bulkSplitter, InsertionStrategy insertionStrategy, double relativeMinEntries, double relativeMinFanout, float reinsert_fraction, float max_overlap, int overlap_type) {
    super(fileName, pageSize, cacheSize, bulkSplitter, insertionStrategy, null, null);
    this.relativeMinEntries = relativeMinEntries;
    this.relativeMinFanout = relativeMinFanout;
    this.reinsert_fraction = reinsert_fraction;
    this.max_overlap = max_overlap;
    this.overlap_type = overlap_type;
  }

  /**
   * Parameter for minimum number of entries per page; defaults to
   * <code>.4</code> times the number of maximum entries.
   */
  public static final OptionID MIN_ENTRIES_ID = OptionID.getOrCreateOptionID("xtree.min_entry_fraction", "The fraction (in [0,1]) of maximally allowed page entries which is to be be used as minimum number of page entries");

  /**
   * Parameter for minimum number of entries per directory page when going for a
   * minimum overlap split; defaults to <code>.3</code> times the number of
   * maximum entries.
   */
  public static final OptionID MIN_FANOUT_ID = OptionID.getOrCreateOptionID("xtree.min_fanout_fraction", "The fraction (in [0,1]) of maximally allowed directory page entries which is to be tolerated as minimum number of directory page entries for minimum overlap splits");

  /**
   * Parameter for the number of re-insertions to be performed instead of doing
   * a split; defaults to <code>.3</code> times the number of maximum entries.
   */
  public static final OptionID REINSERT_ID = OptionID.getOrCreateOptionID("xtree.reinsert_fraction", "The fraction (in [0,1]) of entries to be reinserted instead of performing a split");

  /**
   * Parameter for the maximally allowed overlap. Defaults to <code>.2</code>.
   */
  public static final OptionID MAX_OVERLAP_ID = OptionID.getOrCreateOptionID("xtree.max_overlap_fraction", "The fraction (in [0,1]) of allowed entry overlaps. Overlap type specified in xtree.overlap_type");

  /**
   * Parameter for defining the overlap type to be used for the maximum overlap
   * test. Available options:
   * <dl>
   * <dt><code>DataOverlap</code></dt>
   * <dd>The overlap is the ratio of total data objects in the overlapping
   * region.</dd>
   * <dt><code>VolumeOverlap</code></dt>
   * <dd>The overlap is the fraction of the overlapping region of the two
   * original mbrs:<br>
   * <code>(overlap volume of mbr 1 and mbr 2) / (volume of mbr 1 + volume of mbr 2)</code>
   * <br>
   * This option is faster than <code>DataOverlap</code>, however, it may result
   * in a tree structure which is not optimally adapted to the indexed data.</dd>
   * </dl>
   * Defaults to <code>VolumeOverlap</code>.
   */
  public static final OptionID OVERLAP_TYPE_ID = OptionID.getOrCreateOptionID("xtree.overlap_type", "How to calculate the maximum overlap? Options: \"DataOverlap\" = {ratio of data objects in the overlapping region}, \"VolumeOverlap\" = {(overlap volume) / (volume 1 + volume 2)}");

  /**
   * Parameterizable API.
   * 
   * @author Erich Schubert
   * 
   * @param <O> object type
   */
  public abstract static class Parameterizer<O extends NumberVector<O, ?>> extends AbstractRStarTreeFactory.Parameterizer<O> {
    protected double relativeMinEntries;

    protected double relativeMinFanout;

    protected float reinsert_fraction;

    protected float max_overlap;

    protected int overlap_type;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Bulk loads are not supported yet:
      // super.configBulkLoad(config);
      final DoubleParameter MIN_ENTRIES_PARAMETER = new DoubleParameter(MIN_ENTRIES_ID, new IntervalConstraint(0, IntervalBoundary.CLOSE, 1, IntervalBoundary.OPEN), 0.4);
      if(config.grab(MIN_ENTRIES_PARAMETER)) {
        relativeMinEntries = MIN_ENTRIES_PARAMETER.getValue();
      }
      final DoubleParameter MIN_FANOUT_PARAMETER = new DoubleParameter(MIN_FANOUT_ID, new IntervalConstraint(0, IntervalBoundary.CLOSE, 1, IntervalBoundary.CLOSE), 0.3);
      if(config.grab(MIN_FANOUT_PARAMETER)) {
        relativeMinFanout = MIN_FANOUT_PARAMETER.getValue();
      }
      final DoubleParameter REINSERT_PARAMETER = new DoubleParameter(REINSERT_ID, new IntervalConstraint(0, IntervalBoundary.CLOSE, 1, IntervalBoundary.OPEN), 0.3);
      if(config.grab(REINSERT_PARAMETER)) {
        reinsert_fraction = REINSERT_PARAMETER.getValue().floatValue();
      }
      final DoubleParameter MAX_OVERLAP_PARAMETER = new DoubleParameter(MAX_OVERLAP_ID, new IntervalConstraint(0, IntervalBoundary.OPEN, 1, IntervalBoundary.CLOSE), 0.2);
      if(config.grab(MAX_OVERLAP_PARAMETER)) {
        max_overlap = MAX_OVERLAP_PARAMETER.getValue().floatValue();
      }
      final StringParameter OVERLAP_TYPE_PARAMETER = new StringParameter(OVERLAP_TYPE_ID, new EqualStringConstraint(new String[] { "DataOverlap", "VolumeOverlap" }), "VolumeOverlap");
      if(config.grab(OVERLAP_TYPE_PARAMETER)) {
        String mOType = OVERLAP_TYPE_PARAMETER.getValue();
        if(mOType.equals("DataOverlap")) {
          overlap_type = XTreeBase.DATA_OVERLAP;
        }
        else if(mOType.equals("VolumeOverlap")) {
          overlap_type = XTreeBase.VOLUME_OVERLAP;
        }
        else {
          config.reportError(new WrongParameterValueException("Wrong input parameter for overlap type '" + mOType + "'"));
        }
      }
    }
  }
}