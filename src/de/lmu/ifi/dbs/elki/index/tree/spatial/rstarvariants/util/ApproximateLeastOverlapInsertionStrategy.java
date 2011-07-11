package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.util;

import java.util.Collections;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.index.tree.Node;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexPathComponent;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.TopBoundedHeap;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.FCPair;

/**
 * Insertion strategy that exhaustively tests all childs for the least overlap
 * when inserting.
 * 
 * @author Elke Achtert
 * @author Franz Graf
 * @author Marisa Petri
 * 
 * @param <N> Node type
 * @param <E> Entry type
 */
public class ApproximateLeastOverlapInsertionStrategy implements InsertionStrategy {
  /**
   * Defines how many children are tested for finding the child generating the
   * least overlap when inserting an object. Default 0 means all children
   */
  private int insertionCandidates = 0;

  /**
   * Constructor. s
   * 
   * @param insertionCandidates Number of children to test.
   */
  public ApproximateLeastOverlapInsertionStrategy(int insertionCandidates) {
    super();
    this.insertionCandidates = insertionCandidates;
  }

  /**
   * Returns the path information of the entry of the specified node which needs
   * least overlap enlargement if the given mbr would be inserted into.
   * 
   * @param node the node of which the children should be tested
   * @param mbr the mbr to be inserted into the children
   * @return the path information of the entry which needs least overlap
   *         enlargement if the given mbr would be inserted into
   */
  @Override
  public <N extends Node<E>, E extends SpatialEntry> TreeIndexPathComponent<E> findInsertChild(N node, SpatialComparable mbr) {
    Enlargement<E> min = null;

    TopBoundedHeap<FCPair<Double, E>> entriesToTest = new TopBoundedHeap<FCPair<Double, E>>(insertionCandidates, Collections.reverseOrder());
    for(int i = 0; i < node.getNumEntries(); i++) {
      E entry_i = node.getEntry(i);
      HyperBoundingBox newMBR = SpatialUtil.unionTolerant(mbr, entry_i);
      double volume = /* entry_i.getMBR() == null ? 0 : */SpatialUtil.volume(entry_i);
      double inc_volume = SpatialUtil.volume(newMBR) - volume;
      entriesToTest.add(new FCPair<Double, E>(inc_volume, entry_i));
    }

    while(!entriesToTest.isEmpty()) {
      E entry_i = entriesToTest.poll().getSecond();
      int index = -1;
      HyperBoundingBox newMBR = SpatialUtil.unionTolerant(mbr, entry_i);

      double currOverlap = 0;
      double newOverlap = 0;
      for(int k = 0; k < node.getNumEntries(); k++) {
        E entry_k = node.getEntry(k);
        if(entry_i != entry_k) {
          currOverlap += SpatialUtil.relativeOverlap(entry_i, entry_k);
          newOverlap += SpatialUtil.relativeOverlap(newMBR, entry_k);
        }
        else {
          index = k;
        }
      }

      double volume = /* entry_i.getMBR() == null ? 0 : */SpatialUtil.volume(entry_i);
      double inc_volume = SpatialUtil.volume(newMBR) - volume;
      double inc_overlap = newOverlap - currOverlap;
      Enlargement<E> enlargement = new Enlargement<E>(new TreeIndexPathComponent<E>(entry_i, index), volume, inc_volume, inc_overlap);

      if(min == null || min.compareTo(enlargement) > 0) {
        min = enlargement;
      }
    }

    assert min != null;
    return min.getPathComponent();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Fast-insertion parameter. Optional.
     */
    public static OptionID INSERTION_CANDIDATES_ID = OptionID.getOrCreateOptionID("rtree.insertion-candidates", "defines how many children are tested for finding the child generating the least overlap when inserting an object.");

    int insertionCandidates = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter insertionCandidatesP = new IntParameter(INSERTION_CANDIDATES_ID, new GreaterConstraint(0));
      if(config.grab(insertionCandidatesP)) {
        insertionCandidates = insertionCandidatesP.getValue();
      }
    }

    @Override
    protected ApproximateLeastOverlapInsertionStrategy makeInstance() {
      return new ApproximateLeastOverlapInsertionStrategy(insertionCandidates);
    }
  }
}