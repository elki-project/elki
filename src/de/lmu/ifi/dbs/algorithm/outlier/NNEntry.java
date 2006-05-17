package de.lmu.ifi.dbs.algorithm.outlier;

import java.io.Serializable;

/**
 * Represents an entry in an NN-Table.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class NNEntry implements Serializable {
  private Integer id_o;
  private int index;
  private Integer id_p;
  // reachdist(o,p)
  private double reachDist;
  private double dist;

  public NNEntry(Integer id_o, int k, Integer id_p, double reachDist, double dist) {
    this.id_o = id_o;
    this.index = k;
    this.id_p = id_p;
    this.reachDist = reachDist;
    this.dist = dist;
  }

  public String toString() {
    return "(" + id_o + ", " + index + ", " + id_p +
           ", " + reachDist + ", " + dist + ")";

  }

  public Integer getObjectID() {
    return id_o;
  }

  public Integer getNNObjectID() {
    return id_p;
  }

  public int getIndex() {
    return index;
  }

  public double getReachDist() {
    return reachDist;
  }

  public double getDistance() {
    return dist;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final NNEntry nnEntry = (NNEntry) o;

    if (Double.compare(nnEntry.dist, dist) != 0) return false;
    if (index != nnEntry.index) return false;
    if (Double.compare(nnEntry.reachDist, reachDist) != 0) return false;
    if (!id_o.equals(nnEntry.id_o)) return false;
    return id_p.equals(nnEntry.id_p);
  }

  public int hashCode() {
    int result;
    long temp;
    result = id_o.hashCode();
    result = 29 * result + index;
    result = 29 * result + id_p.hashCode();
    temp = reachDist != +0.0d ? Double.doubleToLongBits(reachDist) : 0L;
    result = 29 * result + (int) (temp ^ (temp >>> 32));
    temp = dist != +0.0d ? Double.doubleToLongBits(dist) : 0L;
    result = 29 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }
}
