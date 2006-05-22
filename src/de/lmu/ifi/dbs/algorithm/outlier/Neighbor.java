package de.lmu.ifi.dbs.algorithm.outlier;

import java.io.Serializable;

/**
 * Represents an entry in a NNTable, encapsulates information about neighboring objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class Neighbor implements Serializable, Cloneable {
  /**
   * The object id;
   */
  private Integer objectID;

  /**
   * The id of the neighboring object.
   */
  private Integer neighborID;

  /**
   * The index of the neighboring object
   * in the object's kNN array.
   */
  private int index;

  /**
   * The distance between the object and its neighbor.
   */
  private double distance;

  /**
   * The reachability distance of the neighbor w.r.t. the object:
   * reachDist(object, neighbor) = max(kNNDist(neighbor), dist(object, neighbor))
   */
  private double reachabilityDistance;

  /**
   * Provides a new neighbor object with the specified parameters.
   *
   * @param objectID   the object id
   * @param index      the index of the neighboring object in the object's kNN array
   * @param neighborID the id of the neighboring object
   * @param dist       the distance between the object and its neighbor
   */
  public Neighbor(Integer objectID, int index, Integer neighborID, double dist) {
    this.objectID = objectID;
    this.index = index;
    this.neighborID = neighborID;
    this.distance = dist;
  }

  /**
   * Provides a new neighbor object with the specified parameters.
   *
   * @param objectID   the object id
   * @param index      the index of the neighboring object in the object's kNN array
   * @param neighborID the id of the neighboring object
   * @param dist       the distance between the object and its neighbor
   * @param reachDist  the reachability distance of the neighbor w.r.t. the object
   */
  public Neighbor(Integer objectID, int index, Integer neighborID, double dist, double reachDist) {
    this.objectID = objectID;
    this.index = index;
    this.neighborID = neighborID;
    this.distance = dist;
    this.reachabilityDistance = reachDist;
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  public String toString() {
    return "(" + index + ", " + neighborID +
           ", " + reachabilityDistance + ", " + distance + ")";

  }

  /**
   * Returns the object id.
   *
   * @return the object id
   */
  public Integer getObjectID() {
    return objectID;
  }

  /**
   * Returns the id of the neighboring object.
   *
   * @return the id of the neighboring object
   */
  public Integer getNeighborID() {
    return neighborID;
  }

  /**
   * Returns the index of the neighboring object in the object's kNN array.
   *
   * @return the index of the neighboring object in the object's kNN array
   */
  public int getIndex() {
    return index;
  }

  /**
   * Sets the index of the neighboring object in the object's kNN array.
   * @param index the index to be set
   */
  public void setIndex(int index) {
    this.index = index;
  }

  /**
   * Returns the reachability distance of the neighbor w.r.t. the object.
   *
   * @return the reachability distance of the neighbor w.r.t. the object.
   */
  public double getReachabilityDistance() {
    return reachabilityDistance;
  }

  /**
   * Sets the reachability distance of the neighbor w.r.t. the object.
   *
   * @param reachabilityDistance the reachability distance to be set
   */
  public void setReachabilityDistance(double reachabilityDistance) {
    this.reachabilityDistance = reachabilityDistance;
  }

  /**
   * Returns the distance between the object and its neighbor.
   *
   * @return the distance between the object and its neighbor
   */
  public double getDistance() {
    return distance;
  }

  /**
   * Indicates whether some other object is "equal to" this one.   * @param o
   *
   * @return <code>true</code> if this object is the same as the o argument,
   *         <code>false</code> otherwise.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final Neighbor neighbor = (Neighbor) o;

    if (Double.compare(neighbor.distance, distance) != 0) return false;
    if (index != neighbor.index) return false;
    if (Double.compare(neighbor.reachabilityDistance, reachabilityDistance) != 0) return false;
    return neighborID.equals(neighbor.neighborID);
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  public int hashCode() {
    int result;
    long temp;
    result = neighborID.hashCode();
    result = 29 * result + index;
    temp = distance != +0.0d ? Double.doubleToLongBits(distance) : 0L;
    result = 29 * result + (int) (temp ^ (temp >>> 32));
    temp = reachabilityDistance != +0.0d ? Double.doubleToLongBits(reachabilityDistance) : 0L;
    result = 29 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  /**
   * Creates and returns a copy of this object.
   */
  public Neighbor copy() {
    return new Neighbor(objectID,
                        index,
                        neighborID,
                        distance,
                        reachabilityDistance);
  }
}
