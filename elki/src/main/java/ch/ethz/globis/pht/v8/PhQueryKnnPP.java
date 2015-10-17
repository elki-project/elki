package ch.ethz.globis.pht.v8;

/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011-2015
Eidgenössische Technische Hochschule Zürich (ETH Zurich)
Institute for Information Systems
GlobIS Group

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

import static ch.ethz.globis.pht.PhTreeHelper.posInArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.NoSuchElementException;

import org.zoodb.index.critbit.CritBit64COW.CBIterator;
import org.zoodb.index.critbit.CritBit64COW.Entry;

import ch.ethz.globis.pht.PhDimFilter;
import ch.ethz.globis.pht.PhDistance;
import ch.ethz.globis.pht.PhEntry;
import ch.ethz.globis.pht.PhTree.PhIterator;
import ch.ethz.globis.pht.PhTree.PhQueryKNN;
import ch.ethz.globis.pht.v8.PhTree8.NodeEntry;

/**
 * kNN query implementation that uses preprocessors and distance functions.
 * 
 * @param <T> 
 */
public class PhQueryKnnPP<T> implements PhQueryKNN<T> {

  private final int DIM;
  private int nMin;
  private PhTree8<T> pht;
  private PhDistance distance;
  private final ArrayList<DistEntry<T>> entries = new ArrayList<>();
  private int resultSize = 0;
  private int currentPos = -1;
  private final PhIteratorFullNoGC<T> itEx;
  private final PhTraversalDistanceChecker<T> checker;

  public PhQueryKnnPP(PhTree8<T> pht) {
    this.DIM = pht.getDIM();
    this.pht = pht;
    this.checker = new PhTraversalDistanceChecker<>();
    this.itEx = new PhIteratorFullNoGC<>(pht, checker);
  }

  @Override
  public long[] nextKey() {
    return nextEntryReuse().getKey();
  }

  @Override
  public T nextValue() {
    return nextEntryReuse().getValue();
  }

  @Override
  public PhEntry<T> nextEntry() {
    return new PhEntry<T>(nextEntryReuse());
  } 

  @Override
  public PhEntry<T> nextEntryReuse() {
    if (currentPos >= resultSize) {
      throw new NoSuchElementException();
    }
    return entries.get(currentPos++);
  }

  @Override
  public boolean hasNext() {
    return currentPos < resultSize;
  }

  @Override
  public T next() {
    return nextValue();
  }

  @Override
  public PhQueryKNN<T> reset(int nMin, PhDistance dist, PhDimFilter dims,
      long... center) {
    this.distance = dist == null ? this.distance : dist;
    this.nMin = nMin;
    clearEntries();

    if (nMin > 0) {
      nearestNeighbourBinarySearch(center, nMin);
    }

    currentPos = 0;
    return this;
  }

  private void findKnnCandidate(long[] center, long[] ret) {
    findKnnCandidate(center, pht.getRoot(), ret);
  }

  private long[] findKnnCandidate(long[] key, Node<T> node, long[] ret) {
    if (node.getInfixLen() > 0) {
      long mask = ~((-1l)<<node.getInfixLen()); // e.g. (0-->0), (1-->1), (8-->127=0x01111111)
      int shiftMask = node.getPostLen()+1;
      //mask <<= shiftMask; //last bit is stored in bool-array
      mask = shiftMask==64 ? 0 : mask<<shiftMask;
      for (int i = 0; i < key.length; i++) {
        if (((key[i] ^ node.getInfix(i)) & mask) != 0) {
          //infix does not match
          //Still, this is the best node, so we take any value in this sub-tree
          return returnAnyValue(ret, key, node);
        }
      }
    }

    long pos = posInArray(key, node.getPostLen());

    //NI-node?
    if (node.isPostNI()) {
      NodeEntry<T> e = node.getChildNI(pos);
      if (e == null) {
        //return simply any value
        return returnAnyValue(ret, key, node);
      } else if (e.node != null) {
        return findKnnCandidate(key, e.node, ret);
      }
      //Never return closest key if we look for nMin>1 keys!
      if (nMin > 1 && distance.distEst(key, e.getKey()) == 0) {
        //Never return a perfect match if we look for nMin>1 keys!
        //otherwise the distance is too small.
        CBIterator<NodeEntry<T>> it = node.ind().iterator();
        Entry<NodeEntry<T>> cbe = it.nextEntry();  //we know that there are at least two!
        while (it.hasNext() && cbe.key()==pos) {
          //TODO use entryReuse?
          cbe = it.nextEntry();
        }
        e = cbe.value();
        if (e.node != null) {
          return findKnnCandidate(key, e.node, ret);
        }
      }
      //now return the key, even if it may not be an exact match (we don't check)
      long[] k = e.getKey();
      System.arraycopy(k, 0, ret, 0, k.length);
      return ret;
    }

    //check sub-node (more likely than postfix, because there can be more than one value)
    Node<T> sub = node.getSubNode(pos, DIM);
    if (sub != null) {
      return findKnnCandidate(key, sub, ret);
    }

    //check for local key/postfix
    int pob = node.getPostOffsetBits(pos, DIM);
    if (pob >= 0 && nMin == 1) {
      //Never return closest key if we look for nMin>1 keys!
      //now return the key, even if it may not be an exact match (we don't check)
      System.arraycopy(key, 0, ret, 0, key.length);
      node.getPost(pos, ret);
      return ret;
    }

    //Okay just perform a query on the current node and return the first value that we find.
    return returnAnyValue(ret, key, node);
  }

  private long[] returnAnyValue(long[] ret, long[] key, Node<T> node) {
    NodeIteratorFullNoGC<T> ni = new NodeIteratorFullNoGC<>(DIM, key);
    ni.init(node, null);
    while (ni.increment()) {
      if (ni.isNextSub()) {
        //traverse sub node
        ni.init(ni.getCurrentSubNode(), null);
      } else {
        PhEntry<T> e = ni.getCurrentPost();
        //Never return closest key if we look for nMin>1 keys!
        if (nMin > 1 && distance.distEst(key, e.getKey()) == 0) {
          //Never return a perfect match if we look for nMin>1 keys!
          //otherwise the distance is too small.
          continue;
        }
        System.arraycopy(e.getKey(), 0, ret, 0, key.length);
        return ret;
      }
    }
    throw new IllegalStateException();
  }

  /**
   * This approach applies binary search to queries.
   * It start with a query that covers the whole tree. Then whenever it finds an entry (the first)
   * it discards the query and starts a smaller one with half the distance to the search-point.
   * This effectively reduces the volume by 2^k.
   * Once a query returns no result, it uses the previous query to traverse all results
   * and find the nearest result.
   * As an intermediate step, it may INCREASE the query size until a non-empty query appears.
   * Then it could decrease again, like a true binary search.
   * 
   * When looking for nMin > 1, one could search for queries with at least nMin results...
   * 
   * @param val
   * @param nMin
   */
  private void nearestNeighbourBinarySearch(long[] val, int nMin) {
    //special case with minDist = 0
    if (nMin == 1 && pht.contains(val)) {
      addEntry(new PhEntry<T>(val, pht.get(val)), val);
      return;
    }

    //special case with size() <= nMin
    if (pht.size() <= nMin) {
      PhIterator<T> itEx = pht.queryExtent();
      while (itEx.hasNext()) {
        PhEntry<T> e = itEx.nextEntryReuse();
        addEntry(e, val);
      }

      sortEntries();
      return;
    }

    long[] cand = new long[DIM];
    findKnnCandidate(val, cand);
    double currentDist = distance.dist(val, cand);
    if (currentDist < 0.0000001) {
      //TODO this is quite arbitrary
      System.err.println("KNN-Query: dist too small: " + currentDist);
      currentDist = 1.0;
    }

    //System.out.println("knnQ start");
    do {
      int result = getCandidates(currentDist, nMin, val);

      if (result == 0) {
        //done!
        return;
      }

      currentDist *= 2;
      //System.out.println("knnQ dist: " + currentDist);
    } while (true);
  }

  private final int getCandidates(double maxDist, int nMin, long[] val) {
    //What are we doing here?
    //There are several cases to consider:
    //1) maxDistSQ is too small, we get < nMin values
    //2) If there are enough entries, we have to consider the following
    //2a) Prevent going further down in case we have exactly nMin entries
    //2b) Prevent going further down in case all entries beyond nMin have the same distance 
    //    than nMin. (no more than nMax different distances)
    //2c) Optimisation: Prevent going further down if we have more than nMin DIFFERENT entries, 
    //	  but we exhausted them all, so we can still easily return a final result.
    //2d) There are too many entries.
    //
    //Finally we should consider an epsilon to avoid removing equally distanced entries
    //TODO TEST epsilon!
    //
    //Solutions:
    // a) Check nMin entries. Return SPACE_NEEDS_EXPANSION if size()<nMin.
    //    [optional?: Return SUCCESS if query space is exhausted and n==nMin].
    // b) Sort them.
    // c) get distance of last entry.
    // d) continue search until we get 
    //    currentEntryCount > nMin*3
    //    AND 
    //    currentEntryCount > 2 * validEntryCount (entries with dist<=dist(cand.get(nMin-1))). 
    //    --> This makes sure that don't shrink the search space if there are only a few too
    //        many entries in the local space.
    //        This is important, because we may have omitted many entries with dist>maxDistQ
    //        TODO keep count?
    //    --> The second term ensures that we don't abort if most of the entries are actually 
    //        valid. 
    // e) If we exhaust the query space, return all with dist<=dist(cand.get(nMin-1)).
    // f) return SPACE_NEEDS_CONTRACTION
    //We have to

    //Epsilon for calculating the distance depends on DIM, the magnitude of the values and
    //the precision of the Double mantissa.
    //TODO, this should use the lowerBound i.o. upperBound
    final double EPS = DIM * maxDist / (double)(1L << 51);//2^(53-2));
    final int CONSOLIDATION_INTERVAL = 10;
    clearEntries();
    checker.set(val, distance, maxDist);
    itEx.reset();
    double maxDistSQ = maxDist * maxDist;// * KNN_SAFETY;

    //a)
    while (itEx.hasNext() && resultSize < nMin) {
      PhEntry<T> en = itEx.nextEntryReuse();
      addEntry(en, val);
    }

    //b)
    sortEntries();

    if (resultSize < nMin) {
      //too small
      return 1;
    }
    if (!itEx.hasNext()) {
      //perfect fit!
      return 0;
    }

    //c)
    maxDistSQ = squareDist(entries.get(nMin-1).getKey(), val);
    checker.set(val, distance, Math.sqrt(maxDistSQ));

    //d)
    while (itEx.hasNext()) {
      PhEntry<T> e = itEx.nextEntryReuse();
      addEntry(e, val);
      if (resultSize % CONSOLIDATION_INTERVAL == 0) {
        maxDistSQ = consolidate(nMin, EPS, val, maxDistSQ);
        //TODO this only works for SQRT
        //update query-dist
        checker.set(val, distance, Math.sqrt(maxDistSQ));
      }
    }
    //e)
    consolidate(nMin, EPS, val, maxDistSQ);
    return 0;
  }

  private double consolidate(int nMin, double EPS, long[] val, double maxSQ) {
    sortEntries();
    double maxSQnew = squareDist(entries.get(nMin-1).getKey(), val);
    if (maxSQnew < maxSQ+EPS) { //TODO epsilon?
      maxSQ = maxSQnew;
      for (int i2 = nMin; i2 < resultSize; i2++) {
        //purge 
        if (squareDist(entries.get(i2).getKey(), val) + EPS > maxSQ) {
          resultSize = i2;
          break;
        }
      }
    }
    return maxSQ;
  }

  private double squareDist(long[] v1, long[] v2) {
    return distance.distEst(v1, v2);
  }


  private static class DistEntry<T> extends PhEntry<T> {
    static final Comparator<DistEntry<?>> COMP = new Comparator<DistEntry<?>>() {
      @Override
      public int compare(DistEntry<?> o1, DistEntry<?> o2) {
        //We assume only normal positive numbers
        double d = o1.dist - o2.dist;
        return d > 0 ? 1 : (d < 0 ? -1 : 0); 
      }
    };

    double dist;

    DistEntry(long[] key, T value, double dist) {
      super(key, value);
      this.dist = dist;
    }

    DistEntry(PhEntry<T> e, double dist) {
      super(e);
      this.dist = dist;
    }

    void set(PhEntry<T> e, double dist) {
      super.setValue(e.getValue());
      System.arraycopy(e.getKey(), 0, getKey(), 0, getKey().length);
      this.dist = dist;
    }
  }

  private void addEntry(PhEntry<T> e, long[] center) {
    double dist = distance.dist(center, e.getKey());
    if (resultSize < entries.size()) {
      entries.get(resultSize).set(e, dist);
    } else {
      DistEntry<T> de = new DistEntry<>(e, dist);
      entries.add(de);
    }
    resultSize++;
  }

  private void clearEntries() {
    resultSize = 0;
    for (int i = 0; i < entries.size(); i++) {
      entries.get(i).dist = Double.MAX_VALUE;
    }
    //TODO clear if size > 2*nMax?
  }

  private void sortEntries() {
    Collections.sort(entries, DistEntry.COMP);
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
