package de.lmu.ifi.dbs.index.metrical.mtree;

import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.metrical.mtree.util.DistanceEntry;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.Iterator;
import java.util.List;

/**
 * Encapsulates the required parameters for a split of a node in a M-Tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class Split<D extends Distance<D>> {
  /**
   * The id of the first promotion object.
   */
  public Integer firstPromoted;

  /**
   * The id of the second promotion object.
   */
  public Integer secondPromoted;

  /**
   * The first covering radius.
   */
  public D firstCoveringRadius;

  /**
   * The second covering radius.
   */
  public D secondCoveringRadius;

  /**
   * Entries assigned to first promotion object
   */
  public List<Entry<D>> assignmentsToFirst;

  /**
   * Entries assigned to second promotion object
   */
  public List<Entry<D>> assignmentsToSecond;

  /**
   * Creates a new split object.
   */
  public Split() {
  }

  /**
   * Assigns the first object of the specified list to the assignment, removes this object
   * from the other list and returns the new covering radius.
   *
   * @param assignment the assignment list
   * @param list       the list, the first object should be assigned
   * @param other      the other list, the object should be removed
   * @param currentCR  the current covering radius
   * @param isLeaf     true, if the node of the entries to be assigned is a leaf, false othwerwise
   * @return the new covering radius
   */
  protected D assignNN(List<Entry<D>> assignment,
                       List<DistanceEntry<D>> list,
                       List<DistanceEntry<D>> other,
                       D currentCR,
                       boolean isLeaf) {

    DistanceEntry<D> distEntry = list.remove(0);
    Integer id = distEntry.getEntry().getObjectID();

    remove(id, other);
    assignment.add(distEntry.getEntry());

    if (isLeaf) {
      return Util.max(currentCR, distEntry.getDistance());
    }
    else {
      return Util.max(currentCR, (D) distEntry.getDistance().plus(((DirectoryEntry<D>) distEntry.getEntry()).getCoveringRadius()));
    }
  }

  /**
   * Removes the entry with the specified id from the given list.
   *
   * @param id   the id of the entry to be removed
   * @param list the list from where the entry should be removed
   */
  private void remove(Integer id, List<DistanceEntry<D>> list) {
    for (Iterator<DistanceEntry<D>> iterator = list.iterator(); iterator.hasNext();) {
      DistanceEntry de = iterator.next();
      if (de.getEntry().getObjectID() == id) iterator.remove();
    }
  }
}
