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

import org.zoodb.index.critbit.CritBit64.CBIterator;
import org.zoodb.index.critbit.CritBit64.Entry;

import ch.ethz.globis.pht.PhEntry;
import ch.ethz.globis.pht.PhTreeHelper;
import ch.ethz.globis.pht.v8.PhTree8.NodeEntry;



/**
 * This NodeIterator reuses existing instances, which may be easier on the Java GC.
 * 
 * 
 * @author ztilmann
 *
 * @param <T>
 */
public class NodeIteratorFullNoGC<T> {
  private final int DIM;
  private boolean isPostHC;
  private boolean isPostNI;
  private boolean isSubHC;
  private int postLen;
  private long next = -1;
  private long nextPost = -1;
  private long nextSub = -1;
  private Node<T> nextSubNode;
  private Node<T> node;
  private int currentOffsetPostKey;
  private int currentOffsetPostVal;
  private int currentOffsetSub;
  private CBIterator<NodeEntry<T>> niIterator;
  private int nMaxPost;
  private int nMaxSub;
  private int nPostsFound = 0;
  private int posSubLHC = -1; //position in sub-node LHC array
  private int postEntryLen;
  private final long[] valTemplate;
  private boolean isPostFinished;
  private boolean isSubFinished;
  private PhTraversalChecker<T> checker;
  private final PhEntry<T> nextPost1;
  private final PhEntry<T> nextPost2;
  private boolean isNextPost1free;
  private final long MAX_POS;


  /**
   * 
   * @param DIM
   * @param valTemplate A null indicates that no values are to be extracted.
   */
  public NodeIteratorFullNoGC(int DIM, long[] valTemplate) {
    this.DIM = DIM;
    this.MAX_POS = (1L << DIM) -1;
    this.valTemplate = valTemplate;
    this.nextPost1 = new PhEntry<T>(new long[DIM], null);
    this.nextPost2 = new PhEntry<T>(new long[DIM], null);
  }

  /**
   * 
   * @param node
   * @param rangeMin The minimum value that any found value should have. If the found value is
   *  lower, the search continues.
   * @param rangeMax
   * @param lower The minimum HC-Pos that a value should have.
   * @param upper
   * @param checker result verifier, can be null.
   */
  private void reinit(Node<T> node, PhTraversalChecker<T> checker) {
    next = -1;
    nextPost = -1;
    nextSub = -1;
    nextSubNode = null;
    currentOffsetPostKey = 0;
    currentOffsetPostVal = 0;
    currentOffsetSub = 0;
    niIterator = null;
    nPostsFound = 0;
    posSubLHC = -1; //position in sub-node LHC array
    this.checker = checker;

    this.node = node;
    this.isPostHC = node.isPostHC();
    this.isPostNI = node.isPostNI();
    this.isSubHC = node.isSubHC();
    this.postLen = node.getPostLen();
    nMaxPost = node.getPostCount();
    nMaxSub = node.getSubCount();
    isPostFinished = (nMaxPost <= 0);
    isSubFinished = (nMaxSub <= 0);
    //Position of the current entry
    currentOffsetSub = node.getBitPos_SubNodeIndex(DIM);
    if (isPostNI) {
      postEntryLen = -1; //not used
      niIterator = node.ind().iterator();
    } else {
      currentOffsetPostKey = node.getBitPos_PostIndex(DIM);
      // -set key offset to position before first element
      // -set value offset to first element
      if (isPostHC) {
        //length of post-fix WITHOUT key
        postEntryLen = DIM*postLen;
        currentOffsetPostVal = currentOffsetPostKey + (1<<DIM)*Node.PINN_HC_WIDTH;  
      } else {
        //length of post-fix WITH key
        postEntryLen = Node.PIK_WIDTH(DIM)+DIM*postLen;
        currentOffsetPostVal = currentOffsetPostKey + Node.PIK_WIDTH(DIM);  
      }
    }
  }

  /**
   * @return TRUE if the node has more elements, irrespective of whether they match the query.
   */
  boolean hasNext() {
    return !isPostFinished || !isSubFinished;
  }

  /**
   * Advances the cursor. 
   * @return TRUE iff a matching element was found.
   */
  boolean increment() {
    next = getNext(isNextPost1free ? nextPost1 : nextPost2);
    return next != -1;
  }

  long getCurrentPos() {
    return isPostNI ? next : nextSub; 
    //return next;
  }

  PhEntry<T> getCurrentPost() {
    return isNextPost1free ? nextPost2 : nextPost1;
  }

  /**
   * Return whether the next value returned by next() is a sub-node or not.
   * 
   * @return True if the current value (returned by next()) is a sub-node, 
   * otherwise false
   */
  boolean isNextSub() {
    return isPostNI ? (nextSubNode != null) : (next == nextSub);
  }

  /**
   * 
   * @return False if the value does not match the range, otherwise true.
   */
  private boolean readValue(long pos, int offsPostKey, PhEntry<T> result) {
    long[] key = result.getKey();
    System.arraycopy(valTemplate, 0, key, 0, DIM);
    PhTreeHelper.applyHcPos(pos, postLen, key);

    result.setValue( node.getPostPOB(offsPostKey, pos, key) );

    if (checker != null && !checker.isValid(key)) {
      return false;
    }

    //Don't set to 'null' here, that interferes with parallel iteration over post/sub 
    //nextSubNode = null;
    isNextPost1free = !isNextPost1free;
    return true;
  }

  private boolean readValue(long pos, NodeEntry<T> e, PhEntry<T> result) {
    //extract postfix
    final long mask = postLen < 63 ? (~0L)<<postLen+1 : 0;
    long[] eKey = e.getKey();
    PhTreeHelper.applyHcPos(pos, postLen, eKey);
    for (int i = 0; i < eKey.length; i++) {
      eKey[i] |= (valTemplate[i] & mask);
    }
    if (checker != null && !checker.isValid(eKey)) {
      return false;
    }
    System.arraycopy(eKey, 0, result.getKey(), 0, DIM);
    result.setValue(e.getValue());
    nextSubNode = null;
    isNextPost1free = !isNextPost1free;
    return true;
  }

  /**
   * 
   * @return False if the value does not match the range, otherwise true.
   */
  private boolean readSub(long pos, Node<T> sub) {
    PhTreeHelper.applyHcPos(pos, postLen, valTemplate);
    sub.getInfix(valTemplate);
    return (checker == null || checker.isValid(sub, valTemplate));
  }

  private long getNext(PhEntry<T> result) {
    if (node.isPostNI()) {
      niFindNext(result);
      return next;
    }

    //Search for next entry if there are more entries and if current
    //entry has already been returned (or is -1).
    // (nextPost == next) is true when the previously returned entry (=next) was a postfix.
    if (!isPostFinished && nextPost == next) {
      if (isPostHC) {
        getNextPostAHC(result);
      } else {
        getNextPostLHC(result);
      }
    }
    if (!isSubFinished && nextSub == next) {
      if (isSubHC) {
        getNextSubAHC();
      } else {
        getNextSubLHC();
      }
      //			if (nextSub >= 0 && nextSub < nextPost && !isSubFinished) {
      //				return nextSub;
      //			}
    }

    if (isPostFinished && isSubFinished) {
      return -1;
    } 
    if (!isPostFinished && !isSubFinished) {
      return (nextSub < nextPost) ? nextSub : nextPost;
    }
    return isPostFinished ? nextSub : nextPost;
  }

  private void getNextPostAHC(PhEntry<T> result) {
    //while loop until 1 is found.
    long currentPos = nextPost; 
    nextPost = -1;
    while (!isPostFinished) {
      currentPos++;  //pos w/o bit-offset
      if (currentPos > MAX_POS) {
        isPostFinished = true;
        break;
      }
      boolean bit = Bits.getBit(node.ba, currentOffsetPostKey);
      currentOffsetPostKey += Node.PINN_HC_WIDTH;  //pos with bit-offset
      if (bit) {
        //check post-fix
        int offs = (int) (currentOffsetPostVal+currentPos*postEntryLen);
        if (!readValue(currentPos, offs, result)) {
          continue;
        }
        nextPost = currentPos;
        break;
      }
    }
  }

  private void getNextPostLHC(PhEntry<T> result) {
    nextPost = -1;
    while (!isPostFinished) {
      if (++nPostsFound > nMaxPost) {
        isPostFinished = true;
        break;
      }
      int offs = currentOffsetPostKey;
      long currentPos = Bits.readArray(node.ba, offs, Node.PIK_WIDTH(DIM));
      currentOffsetPostKey += postEntryLen;
      //check post-fix
      if (!readValue(currentPos, offs + Node.PIK_WIDTH(DIM), result)) {
        continue;
      }
      nextPost = currentPos;
      break;
    }
  }

  private void getNextSubAHC() {
    int currentPos = (int) nextSub;  //We use (int) because arrays are always (int).
    nextSub = -1;
    while (!isSubFinished) {
      currentPos++;
      if (currentPos > MAX_POS) {
        isSubFinished = true;
        break;
      }
      if (node.subNRef(currentPos) != null) {
        Node<T> sub = node.subNRef(currentPos);
        if (readSub(currentPos, sub)) {
          nextSub = currentPos;
          nextSubNode = sub;
          break;
        }
      }
    }
  } 

  private void getNextSubLHC() {
    nextSub = -1;
    while (!isSubFinished) {
      if (posSubLHC + 1  >= nMaxSub) {
        isSubFinished = true;
        break;
      }
      long currentPos = Bits.readArray(node.ba, currentOffsetSub, Node.SIK_WIDTH(DIM));
      currentOffsetSub += Node.SIK_WIDTH(DIM);
      posSubLHC++;
      Node<T> sub = node.subNRef(posSubLHC);
      if (readSub(currentPos, sub)) {
        nextSub = currentPos;
        nextSubNode = sub;
        break;
      }
    }
  }

  private void niFindNext(PhEntry<T> result) {
    while (niIterator.hasNext()) {
      Entry<NodeEntry<T>> e = niIterator.nextEntry();
      next = e.key();
      nextSubNode = e.value().node;
      if (nextSubNode == null) {
        if (!readValue(e.key(), e.value(), result)) {
          continue;
        }
      } else {
        if (!readSub(e.key(), nextSubNode)) {
          continue;
        }
      }
      return;
    }

    next = -1;
    return;
  }

  public Node<T> getCurrentSubNode() {
    return nextSubNode;
  }

  public Node<T> node() {
    return node;
  }

  void init(Node<T> node, PhTraversalChecker<T> checker) {
    reinit(node, checker);
  }

}
