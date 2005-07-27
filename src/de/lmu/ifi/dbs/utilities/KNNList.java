package de.lmu.ifi.dbs.utilities;

// TODO nach db
public class KNNList {
  /*
  private SortedSet list;
  private int k;
  private DistanceFunction distFunction;

  public KNNList(int k, DistanceFunction distFunction) {
    this.list = new TreeSet();
    this.k = k;
    this.distFunction = distFunction;
  }

  public boolean add(DBNeighbor o) {
    if (list.size() < k) {
      list.add(o);
      return true;
    }

    DBNeighbor last = (DBNeighbor) list.last();
    if (o.getDistance().compareTo(last.getDistance()) < 0) {
      list.remove(last);
      list.add(o);
      return true;
    }

    return false;
  }

  public Distance getMaximumDistance() {
    if (list.isEmpty())
      return distFunction.infiniteDistance();

    DBNeighbor last = (DBNeighbor) list.last();
    return last.getDistance();
  }

  public List<DBNeighbor> toList() {
    return new ArrayList<DBNeighbor>(list);
  }

  public int size() {
    return list.size();
  }
  */
}
