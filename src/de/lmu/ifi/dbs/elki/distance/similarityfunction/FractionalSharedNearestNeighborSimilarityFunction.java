package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.SharedNearestNeighborsPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * @author Arthur Zimek
 */
// todo arthur comment class
public class FractionalSharedNearestNeighborSimilarityFunction<O extends DatabaseObject, D extends Distance<D>> extends AbstractPreprocessorBasedSimilarityFunction<O, SharedNearestNeighborsPreprocessor<O, D>, DoubleDistance> {

  /**
   * Holds the number of nearest neighbors to be used.
   */
  private int numberOfNeighbors;

  /**
   * Provides a SharedNearestNeighborSimilarityFunction with a pattern defined
   * to accept Strings that define a non-negative Integer.
   */
  public FractionalSharedNearestNeighborSimilarityFunction() {
    super(Pattern.compile("\\d+"));
  }

  @SuppressWarnings("unchecked")
  public DoubleDistance similarity(Integer id1, Integer id2) {
    SortedSet<Integer> neighbors1 = (SortedSet<Integer>) getDatabase().getAssociation(getAssociationID(), id1);
    SortedSet<Integer> neighbors2 = (SortedSet<Integer>) getDatabase().getAssociation(getAssociationID(), id2);
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
    return new DoubleDistance(intersection / numberOfNeighbors);
  }

  public DoubleDistance infiniteDistance() {
    return new DoubleDistance(Double.POSITIVE_INFINITY);
  }

  public boolean isInfiniteDistance(DoubleDistance distance) {
    return distance.equals(new DoubleDistance(Double.POSITIVE_INFINITY)) || distance.equals(new DoubleDistance(Double.NEGATIVE_INFINITY));
  }

  public boolean isNullDistance(DoubleDistance distance) {
    return distance.equals(new DoubleDistance(0));
  }

  public boolean isUndefinedDistance(@SuppressWarnings("unused") DoubleDistance distance) {
    throw new UnsupportedOperationException("Undefinded distance not supported!");
  }

  public DoubleDistance nullDistance() {
    return new DoubleDistance(0);
  }

  public DoubleDistance undefinedDistance() {
    throw new UnsupportedOperationException("Undefinded distance not supported!");
  }

  public DoubleDistance valueOf(String pattern) throws IllegalArgumentException {
    if(matches(pattern)) {
      return new DoubleDistance(Double.parseDouble(pattern));
    }
    else {
      throw new IllegalArgumentException("Given pattern \"" + pattern + "\" does not match required pattern \"" + requiredInputPattern() + "\"");
    }
  }

  /**
   * @return the association ID for the association to be set by the
   *         preprocessor, which is
   *         {@link AssociationID#SHARED_NEAREST_NEIGHBORS_SET}
   */
  public AssociationID<SortedSet<?>> getAssociationID() {
    return AssociationID.SHARED_NEAREST_NEIGHBORS_SET;
  }

  /**
   * @return the name of the default preprocessor, which is
   *         {@link SharedNearestNeighborsPreprocessor}
   */
  public String getDefaultPreprocessorClassName() {
    return SharedNearestNeighborsPreprocessor.class.getName();
  }

  public String getPreprocessorDescription() {
    return "The Classname of the preprocessor to determine the neighbors of the objects.";
  }

  /**
   * @return the super class for the preprocessor, which is
   *         {@link SharedNearestNeighborsPreprocessor}
   */
  @SuppressWarnings("unchecked")
  public Class<? extends Preprocessor> getPreprocessorSuperClass() {
    return SharedNearestNeighborsPreprocessor.class;
  }

  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    // number of neighbors
    numberOfNeighbors = getPreprocessor().getNumberOfNeighbors();
    return remainingParameters;
  }

}
