package de.lmu.ifi.dbs.elki.result;

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.HierarchyHashmapList;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.ModifiableHierarchy;

/**
 * Class to store a hierarchy of result objects.
 * 
 * @author Erich Schubert
 */
// TODO: add listener handling!
public class ResultHierarchy extends HierarchyHashmapList<Result> {
  /**
   * Constructor. 
   */
  public ResultHierarchy() {
    super();
  }

  @Override
  public void add(Result parent, Result child) {
    super.add(parent, child);
    // TODO: fire listeners
    if (child instanceof HierarchicalResult) {
      HierarchicalResult hr = (HierarchicalResult) child;
      ModifiableHierarchy<Result> h = hr.getHierarchy();
      // Merge hierarchy
      hr.setHierarchy(this);
      // Add grandchildren
      for (Result desc : h.getChildren(hr)) {
        this.add(hr, desc);
        if (desc instanceof HierarchicalResult) {
          ((HierarchicalResult) desc).setHierarchy(this);
        }
      }
      // Add grand*-children of any child
      for (Result desc : h.iterDescendants(hr)) {
        for (Result desc2 : h.getChildren(desc)) {
          this.add(desc, desc2);
          if (desc2 instanceof HierarchicalResult) {
            ((HierarchicalResult) desc2).setHierarchy(this);
          }
        }
      }
    }
  }

  @SuppressWarnings("unused")
  @Override
  public void remove(Result parent, Result child) {
    // TODO: unlink from hierarchy.
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("unused")
  @Override
  public void put(Result obj, List<Result> parents, List<Result> children) {
    // TODO: can we support this somehow? Or reduce visibility?
    throw new UnsupportedOperationException();
  }
}
