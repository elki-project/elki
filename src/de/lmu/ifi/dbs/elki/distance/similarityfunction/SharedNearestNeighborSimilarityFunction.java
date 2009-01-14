package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.IntegerDistance;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.SharedNearestNeighborsPreprocessor;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.regex.Pattern;

/**
 * @author Arthur Zimek
 */
// todo arthur comment class
public class SharedNearestNeighborSimilarityFunction<O extends DatabaseObject, D extends Distance<D>>
    extends AbstractPreprocessorBasedSimilarityFunction<O, SharedNearestNeighborsPreprocessor<O, D>, IntegerDistance> {

    /**
   * Provides a SharedNearestNeighborSimilarityFunction with a pattern defined to accept
   * Strings that define a non-negative Integer.
   */
    public SharedNearestNeighborSimilarityFunction() {
        super(Pattern.compile("\\d+"));
    }

    public IntegerDistance similarity(Integer id1, Integer id2) {
        SortedSet<Integer> neighbors1 = getDatabase().getAssociation(getAssociationID(), id1);
        SortedSet<Integer> neighbors2 = getDatabase().getAssociation(getAssociationID(), id2);
        return new IntegerDistance(countSharedNeighbors(neighbors1, neighbors2));
    }
    
    static public int countSharedNeighbors(SortedSet<Integer> neighbors1, SortedSet<Integer> neighbors2) {
      int intersection = 0;
      Iterator<Integer> iter1 = neighbors1.iterator();
      Iterator<Integer> iter2 = neighbors2.iterator();
      Integer neighbors1ID = null;
      Integer neighbors2ID = null;
      if(iter1.hasNext()) {
        neighbors1ID = iter1.next();
      }
      if(iter2.hasNext()) {
        neighbors2ID = iter2.next();
      }
      while((iter1.hasNext() || iter2.hasNext()) && neighbors1ID != null && neighbors2ID != null) {
        if(neighbors1ID.equals(neighbors2ID)) {
          intersection++;
          if(iter1.hasNext()) {
            neighbors1ID = iter1.next();
          }
          else {
            neighbors1ID = null;
          }
          if(iter2.hasNext()) {
            neighbors2ID = iter2.next();
          }
          else {
            neighbors2ID = null;
          }
        }
        else if(neighbors1ID < neighbors2ID) {
          if(iter1.hasNext()) {
            neighbors1ID = iter1.next();
          }
          else {
            neighbors1ID = null;
          }
        }
        else // neighbors1ID > neighbors2ID
        {
          if(iter2.hasNext()) {
            neighbors2ID = iter2.next();
          }
          else {
            neighbors2ID = null;
          }
        }
      }
      return intersection;
    }

    public IntegerDistance infiniteDistance() {
        return new IntegerDistance(Integer.MAX_VALUE);
    }

    public boolean isInfiniteDistance(IntegerDistance distance) {
        return distance.equals(new IntegerDistance(Integer.MAX_VALUE));
    }

    public boolean isNullDistance(IntegerDistance distance) {
        return distance.equals(new IntegerDistance(0));
    }

    public boolean isUndefinedDistance(@SuppressWarnings("unused") IntegerDistance distance) {
        throw new UnsupportedOperationException("Undefinded distance not supported!");
    }

    public IntegerDistance nullDistance() {
        return new IntegerDistance(0);
    }

    public IntegerDistance undefinedDistance() {
        throw new UnsupportedOperationException("Undefinded distance not supported!");
    }

    public IntegerDistance valueOf(String pattern) throws IllegalArgumentException {
        if (matches(pattern)) {
            return new IntegerDistance(Integer.parseInt(pattern));
        }
        else {
            throw new IllegalArgumentException("Given pattern \"" + pattern + "\" does not match required pattern \"" + requiredInputPattern() + "\"");
        }
    }

    /**
     * @return the association ID for the association to be set by the preprocessor,
     *         which is {@link AssociationID#SHARED_NEAREST_NEIGHBORS_SET}
     */
    public AssociationID<SortedSet<Integer>> getAssociationID() {
        return AssociationID.SHARED_NEAREST_NEIGHBORS_SET;
    }

    /**
     * @return the name of the default preprocessor,
     *         which is {@link SharedNearestNeighborsPreprocessor}
     */
    public String getDefaultPreprocessorClassName() {
        return SharedNearestNeighborsPreprocessor.class.getName();
    }

    public String getPreprocessorDescription() {
        return "The Classname of the preprocessor to determine the neighbors of the objects.";
    }

    /**
     * @return the super class for the preprocessor,
     *         which is {@link SharedNearestNeighborsPreprocessor}
     */
    @SuppressWarnings("unchecked")
    public Class<? extends Preprocessor> getPreprocessorSuperClass() {
        return SharedNearestNeighborsPreprocessor.class;
    }
}
