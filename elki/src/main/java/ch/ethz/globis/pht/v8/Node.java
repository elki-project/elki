/*
 * Copyright 2011-2015 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.pht.v8;

import static ch.ethz.globis.pht.PhTreeHelper.DEBUG_FULL;
import static ch.ethz.globis.pht.PhTreeHelper.applyHcPos;

import org.zoodb.index.critbit.CritBit64COW;
import org.zoodb.index.critbit.CritBit64COW.CBIterator;
import org.zoodb.index.critbit.CritBit64COW.Entry;

import ch.ethz.globis.pht.PhEntry;
import ch.ethz.globis.pht.PhTreeHelper;
import ch.ethz.globis.pht.util.Refs;
import ch.ethz.globis.pht.v8.PhTree8.NodeEntry;


/**
 * Node of the PH-tree.
 * 
 * @author ztilmann
 *
 * @param <T>
 */
class Node<T> {

  //size of references in bytes
  private static final int REF_BITS = 4*8;

  private static final boolean NI_THRESHOLD(int subCnt, int postCnt) {
    return (subCnt > 500 || postCnt > 50);
  }

  static final int HC_BITS = 0;  //number of bits required for storing current (HC)-representation
  static final int PINN_HC_WIDTH = 1; //width of not-null flag for post-hc
  static final int PIK_WIDTH(int DIM) { return DIM; };//DIM; //post index key width 
  static final int SIK_WIDTH(int DIM) { return DIM; };//DIM; //sub index key width 

  private Node<T>[] subNRef;
  private T[] values;

  private int subCnt = 0;
  private int postCnt = 0;

  /**
   * Structure of the byte[] and the required bits
   * Post-HC (subIndex can be HC or LHC):
   * | isHC | isHC | pCnt | sCnt | subIndex HC/LHC | postKeys HC | postValues HC  |
   * |    1 |    1 |  DIM |  DIM | 0 / sCnt*DIM    | 2^DIM       | 2^DIM*DIM*pLen |
   * 
   * Post-LHC (subIndex can be HC or LHC):
   * | isHC | isHC | pCnt | sCnt | subIndex HC/LHC | post-keys and -values LHC |
   * |    1 |    1 |  DIM |  DIM | 0 / sCnt*DIM    | pCnt*(DIM + DIM*pLen)     |
   * 
   * 
   * pLen = postLen
   * pCnt = postCount
   * sCnt = subCount
   */
  long[] ba = null;

  // |   1st   |   2nd    |   3rd   |    4th   |
  // | isSubHC | isPostHC | isSubNI | isPostNI |
  private byte isHC = 0;

  private byte postLen = 0;
  private byte infixLen = 0; //prefix size

  private CritBit64COW<NodeEntry<T>> ind = null;

  @SuppressWarnings("unchecked")
  protected Node(Node<T> original, int dim) {
    if (original.subNRef != null) {
      int size = original.subNRef.length;
      this.subNRef = new Node[size];
      System.arraycopy(original.subNRef, 0, this.subNRef, 0, size);
    }
    if (original.values != null) {
      this.values = (T[]) original.values.clone();
    }
    if (original.ba != null) {
      this.ba = new long[original.ba.length];
      System.arraycopy(original.ba, 0, this.ba, 0, original.ba.length);
    }
    this.subCnt = original.subCnt;
    this.postCnt = original.postCnt;
    this.infixLen = original.infixLen;
    this.isHC = original.isHC;
    this.postLen = original.postLen;
    this.infixLen = original.infixLen;
    if (original.ind != null) {
      this.ind = original.ind.copy();
    }
    if (original.ba != null) {
      int nrBits = original.isPostNI() ? 
          calcArraySizeTotalBitsNI(dim) 
          : calcArraySizeTotalBits(original.getPostCount(), dim);
          this.ba = Bits.arrayCreate(nrBits);
          System.arraycopy(original.ba, 0, this.ba, 0, original.ba.length);
    }
  }

  protected Node(int infixLen, int postLen, int estimatedPostCount, int DIM, PhTree8<T> tree) {
    this.infixLen = (byte) infixLen;
    this.postLen = (byte) postLen;
    tree.increaseNrNodes();
    if (estimatedPostCount >= 0) {
      int size = calcArraySizeTotalBits(estimatedPostCount, DIM);
      this.ba = Bits.arrayCreate(size);
    }
  }

  static <T> Node<T> createNode(PhTree8<T> tree, int infixLen, int postLen, 
      int estimatedPostCount, final int DIM) {
    return new Node<T>(infixLen, postLen, estimatedPostCount, DIM, tree);
  }

  static <T> Node<T> createNode(Node<T> original, int dim) {
    return new Node<T>(original, dim);
  }

  NodeEntry<T> createNodeEntry(Node<T> sub) {
    return new NodeEntry<>(sub);
  }

  NodeEntry<T> createNodeEntry(long[] key, T value) {
    return new NodeEntry<>(key, value);
  }

  boolean hasInfixes() {
    return infixLen > 0;
  }

  int calcArraySizeTotalBits(int bufPostCnt, final int DIM) {
    int nBits = getBitPos_PostIndex(DIM);
    //post-fixes
    if (isPostHC()) {
      //hyper-cube
      nBits += (PINN_HC_WIDTH + DIM * postLen) * (1 << DIM);
    } else if (isPostNI()) {
      nBits += 0;
    } else {
      //hc-pos index
      nBits += bufPostCnt * (PIK_WIDTH(DIM) + DIM * postLen);
    }
    return nBits;
  }

  private int calcArraySizeTotalBitsNI(final int DIM) {
    return getBitPos_PostIndex(DIM);
  }

  long getInfix(int dim) {
    return Bits.readArray(this.ba, getBitPos_Infix()
        + dim*infixLen, infixLen) << (postLen+1);
  }


  void getInfix(long[] val) {
    if (!hasInfixes()) {
      return;
    }
    int maskLen = postLen + 1 + infixLen;
    //To cut of trailing bits
    long mask = (-1L) << maskLen;
    for (int i = 0; i < val.length; i++) {
      //Replace val with infix (val may be !=0 from traversal)
      val[i] &= mask;
      val[i] |= getInfix(i);
    }
  }


  /**
   * Get the infix without first deleting the incoming val[].
   * @param val
   */
  void getInfixNoOverwrite(long[] val) {
    if (!hasInfixes()) {
      return;
    }
    for (int i = 0; i < val.length; i++) {
      val[i] |= getInfix(i);
    }
  }


  void writeInfix(long[] key) {
    int pos = getBitPos_Infix();
    int shift = postLen+1;
    for (long k: key) {
      Bits.writeArray(this.ba, pos, infixLen, k >>> shift);
      pos += infixLen;
    }
  }


  long getInfixBit(int infId, final int infixInternalOffset) {
    int startBitTotal = infId*infixLen + infixInternalOffset;
    return Bits.getBit01(ba, getBitPos_Infix() + startBitTotal);
  }

  /**
   * 
   * @param pos The position of the node when mapped to a vector.
   * @return The sub node or null.
   */
  NodeEntry<T> getChildNI(long pos) {
    return niGet(pos);
  }

  /**
   * 
   * @param pos The position of the node when mapped to a vector.
   * @return The sub node or null.
   */
  Node<T> getSubNode(long pos, final int DIM) {
    if (ind != null) {
      NodeEntry<T> e = niGet(pos);
      if (e == null) {
        return null;
      }
      return e.node; 
    }
    if (subNRef == null) {
      return null;
    }
    if (isSubHC()) {
      return subNRef[(int) pos];
    }
    int subOffsBits = getBitPos_SubNodeIndex(DIM);
    int p2 = Bits.binarySearch(ba, subOffsBits, getSubCount(), pos, SIK_WIDTH(DIM), 0);
    if (p2 < 0) {
      return null;
    }
    return subNRef[p2];
  }


  /**
   * Return sub node at given position.
   * @param posHC Hyper-cube position for hyper-cube representation
   * @param posLHC array position for LHC representation
   * @return The sub node at the given position
   */
  Node<T> getSubNodeWithPos(long posHC, int posLHC) {
    if (isSubNI()) {
      return niGet(posHC).node;
    }
    Node<T> ret;
    if (isSubHC()) {
      ret = subNRef[(int)posHC];
    } else {
      ret = subNRef[posLHC];
    }
    return ret;
  }


  /**
   * Add a new sub-node. 
   * @param pos
   * @param sub
   */
  @SuppressWarnings("unchecked")
  void addSubNode(long pos, Node<T> sub, final int DIM) {
    final int bufSubCount = getSubCount();
    final int bufPostCount = getPostCount();

    if (!isSubNI() && NI_THRESHOLD(bufSubCount, bufPostCount)) {
      niBuild(bufSubCount, bufPostCount, DIM);
    }
    if (isSubNI()) {
      niPut(pos, sub);
      setSubCount(bufSubCount+1);
      return;
    }

    if (subNRef == null) {
      subNRef = new Node[2];
    }

    //decide here whether to use hyper-cube or linear representation
    if (isSubHC()) {
      subNRef[(int) pos] = sub;
      setSubCount(bufSubCount+1);
      return;
    }

    int subOffsBits = getBitPos_SubNodeIndex(DIM);

    //switch to normal array (full hyper-cube) if applicable.
    if (DIM<=31 && (REF_BITS+SIK_WIDTH(DIM))*(subNRef.length+1L) >= REF_BITS*(1L<<DIM)*1) {
      //migrate to full array!
      Node<T>[] na = new Node[1<<DIM];
      for (int i = 0; i < bufSubCount; i++) {
        int posOld = (int) Bits.readArray(ba, subOffsBits + i*SIK_WIDTH(DIM), SIK_WIDTH(DIM));
        na[posOld] = subNRef[i];
      }
      subNRef = na;
      Bits.removeBits(ba, subOffsBits, bufSubCount*SIK_WIDTH(DIM));
      setSubHC(true);
      subNRef[(int) pos] = sub;
      //subCount++;
      setSubCount(bufSubCount+1);
      int reqSize = calcArraySizeTotalBits(bufPostCount, DIM);
      ba = Bits.arrayTrim(ba, reqSize);
      return;
    }

    int p2 = Bits.binarySearch(ba, subOffsBits, bufSubCount, pos, SIK_WIDTH(DIM), 0);

    //subCount++;
    setSubCount(bufSubCount+1);

    int start = -(p2+1);
    int len = bufSubCount+1 - start-1;
    // resize only if necessary (could be multiples of 2 to avoid copying)!
    if (subNRef.length < bufSubCount+1) {
      int newLen = bufSubCount+1;
      newLen = (newLen&1)==0 ? newLen : newLen+1; //ensure multiples of two
      Node<T>[] na2 = new Node[newLen];
      System.arraycopy(subNRef, 0, na2, 0, start);
      System.arraycopy(subNRef, start, na2, start+1, len);
      subNRef = na2;
    } else {
      System.arraycopy(subNRef, start, subNRef, start+1, len);
    }
    subNRef[start] = sub;

    //resize index array?
    ba = Bits.arrayEnsureSize(ba, calcArraySizeTotalBits(bufPostCount, DIM));
    Bits.insertBits(ba, subOffsBits + start*SIK_WIDTH(DIM), SIK_WIDTH(DIM));
    Bits.writeArray(ba, subOffsBits + start*SIK_WIDTH(DIM), SIK_WIDTH(DIM), pos);
  }

  public void replacePost(int pob, long pos, long[] newKey, T value) {
    if (isPostNI()) {
      niPut(pos, newKey, value);
      return;
    }
    long[] ia = ba;
    int offs = pob;
    for (long key: newKey) {
      Bits.writeArray(ia, offs, postLen, key);
      offs += postLen;
    }
  }

  /**
   * Replace a sub-node, for example if the current sub-node is removed, it may have to be
   * replaced with a sub-sub-node.
   */
  void replaceSub(long pos, Node<T> newSub, final int DIM) {
    if (isSubNI()) {
      niPut(pos, newSub);
      return;
    }
    if (isSubHC()) {
      subNRef[(int) pos] = newSub;
    } else {
      //linearized cube
      int subOffsBits = getBitPos_SubNodeIndex(DIM);
      int p2 = Bits.binarySearch(ba, subOffsBits, getSubCount(), pos, SIK_WIDTH(DIM), 0);
      subNRef[p2] = newSub;
    }
  }

  @SuppressWarnings("unchecked")
  void removeSub(long pos, final int DIM) {
    int bufSubCnt = getSubCount();
    if (isSubNI()) {
      final int bufPostCnt = getPostCount();
      if (!NI_THRESHOLD(bufSubCnt, bufPostCnt)) {
        niDeconstruct(DIM, pos, true);
        return;
      }
    }
    if (isSubNI()) {
      niRemove(pos);
      setSubCount(bufSubCnt-1);
      return;
    }
    final int bufPostCnt = getPostCount();

    //switch representation (HC <-> Linear)?
    //+1 bit for null/not-null flag
    long sizeHC = REF_BITS*(1L<<DIM); 
    //+DIM assuming compressed IDs
    long sizeLin = (REF_BITS+SIK_WIDTH(DIM))*(subNRef.length-1L);
    if (isSubHC() && (sizeLin < sizeHC)) {
      //revert to linearized representation, if applicable
      int prePostBits_SubHC = getBitPos_PostIndex(DIM);
      setSubHC( false );
      bufSubCnt--;
      setSubCount(bufSubCnt);
      int prePostBits_SubLHC = getBitPos_PostIndex(DIM);
      int bia2Size = calcArraySizeTotalBits(bufPostCnt, DIM);
      long[] bia2 = Bits.arrayCreate(bia2Size);
      Node<T>[] sa2 = new Node[bufSubCnt];
      int preSubBits = getBitPos_SubNodeIndex(DIM);
      //Copy only bits that are relevant. Otherwise we might mess up the not-null table!
      Bits.copyBitsLeft(ba, 0, bia2, 0, preSubBits);
      int n=0;
      for (int i = 0; i < (1L<<DIM); i++) {
        if (i==pos) {
          //skip the item that should be deleted.
          continue;
        }
        if (subNRef[i] != null) {
          sa2[n]= subNRef[i];
          Bits.writeArray(bia2, preSubBits + n*SIK_WIDTH(DIM), SIK_WIDTH(DIM), i);
          n++;
        }
      }
      //length: we copy as many bits as fit into bia2, which is easiest to calculate
      Bits.copyBitsLeft(
          ba, prePostBits_SubHC, 
          bia2, prePostBits_SubLHC,
          bia2Size-prePostBits_SubLHC);  
      ba = bia2;
      subNRef = sa2;
      return;
    }			


    if (isSubHC()) {
      //hyper-cube
      setSubCount(bufSubCnt-1);
      subNRef[(int) pos] = null;
      //Nothing else to do.
    } else {
      //linearized cube
      int subOffsBits = getBitPos_SubNodeIndex(DIM);
      int p2 = Bits.binarySearch(ba, subOffsBits, bufSubCnt, pos, SIK_WIDTH(DIM), 0);

      //subCount--;
      setSubCount(bufSubCnt-1);

      //remove value
      int len = bufSubCnt - p2-1;
      // resize only if necessary (could be multiples of 2 to avoid copying)!
      // not -1 to allow being one larger than necessary.
      if (subNRef.length > bufSubCnt) {
        int newLen = bufSubCnt-1;
        newLen = (newLen&1)==0 ? newLen : newLen+1; //ensure multiples of two
        if (newLen > 0) {
          Node<T>[] na2 = new Node[newLen];
          System.arraycopy(subNRef, 0, na2, 0, p2);
          System.arraycopy(subNRef, p2+1, na2, p2, len);
          subNRef = na2;
        } else {
          subNRef = null;
        }
      } else {
        if (p2+1 < subNRef.length) {
          System.arraycopy(subNRef, p2+1, subNRef, p2, len);
        }
      }

      //resize index array
      int offsKey = getBitPos_SubNodeIndex(DIM) + SIK_WIDTH(DIM)*p2;
      Bits.removeBits(ba, offsKey, SIK_WIDTH(DIM));

      //shrink array
      ba = Bits.arrayTrim(ba, calcArraySizeTotalBits(bufPostCnt, DIM));
    }
  }

  /**
   * Compare two post-fixes. Takes as parameter not the position but the post-offset-bits.
   * @param pob
   * @param key
   * @return true, if the post-fixes match
   */
  boolean postEqualsPOB(int offsPostKey, long hcPos, long[] key) {
    if (isPostNI()) {
      long[] post = niGet(hcPos).getKey();
      long mask = ~((-1L) << postLen);
      for (int i = 0; i < key.length; i++) {
        //post requires a mask because we currently don't adjust it if the node moves 
        //up or down
        if (((post[i] ^ key[i]) & mask) != 0) {
          return false;
        }
      }
      return true;
    }

    long[] ia = ba;
    int offs = offsPostKey;
    //Can not be null at this point...
    //Also, length can be expected to be equal
    long mask = ~((-1L) << postLen);
    for (int i = 0; i < key.length; i++) {
      long l = Bits.readArray(ia, offs + i*postLen, postLen);
      if (l != (key[i] & mask)) {
        return false;
      }
    }
    return true;
  }

  boolean niContains(long hcPos) {
    return ind.contains(hcPos);
  }

  NodeEntry<T> niGet(long hcPos) {
    return ind.get(hcPos);
  }

  NodeEntry<T> niPut(long hcPos, long[] key, T value) {
    long[] copy = new long[key.length];
    System.arraycopy(key, 0, copy, 0, key.length);
    return ind.put(hcPos, createNodeEntry(copy, value));
  }

  NodeEntry<T> niPutNoCopy(long hcPos, long[] key, T value) {
    return ind.put(hcPos, createNodeEntry(key, value));
  }

  NodeEntry<T> niPut(long hcPos, Node<T> subNode) {
    return ind.put(hcPos, createNodeEntry(subNode));
  }

  NodeEntry<T> niRemove(long hcPos) {
    return ind.remove(hcPos);
  }

  /**
   * Compare two post-fixes. Takes as parameter not the position but the post-offset-bits.
   * @param key1
   * @param key2
   * @return true, if the post-fixes match
   */
  boolean postEquals(long[] key1, long[] key2) {
    //Can not be null at this point...
    //Also, length can be expected to be equal

    long mask = ~((-1L) << postLen);
    for (int i = 0; i < key1.length; i++) {
      if (((key1[i] ^ key2[i]) & mask) != 0) {
        return false;
      }
    }
    return true;
  }

  void addPost(long pos, long[] key, T value) {
    final int DIM = key.length;
    if (isPostNI()) {
      addPostPOB(pos, -1, key, value);
      return;
    }

    int offsKey = getPostOffsetBits(pos, DIM);
    addPostPOB(pos, offsKey, key, value);
  }

  /**
   * 
   * @param pos
   * @param offsPostKey POB: Post offset bits from getPostOffsetBits(...)
   * @param key
   */
  void addPostPOB(long pos, int offsPostKey, long[] key, T value) {
    final int DIM = key.length;
    final int bufSubCnt = getSubCount();
    final int bufPostCnt = getPostCount();
    //decide here whether to use hyper-cube or linear representation
    //--> Initially, the linear version is always smaller, because the cube has at least
    //    two entries, even for a single dimension. (unless DIM > 2*REF=2*32 bit 
    //    For one dimension, both need one additional bit to indicate either
    //    null/not-null (hypercube, actually two bit) or to indicate the index. 

    if (!isPostNI() && NI_THRESHOLD(bufSubCnt, bufPostCnt)) {
      niBuild(bufSubCnt, bufPostCnt, DIM);
    }
    if (isPostNI()) {
      niPut(pos, key, value);
      setPostCount(bufPostCnt+1);
      return;
    }

    if (values == null) {
       values = Refs.arrayCreate(1);
    }

    //switch representation (HC <-> Linear)?
    //+1 bit for null/not-null flag
    long sizeHC = (long) ((DIM * postLen + PINN_HC_WIDTH) * (1L << DIM)); 
    //+DIM because every index entry needs DIM bits
    long sizeLin = (DIM * postLen + PIK_WIDTH(DIM)) * (bufPostCnt+1L);
    if (!isPostHC() && (DIM<=31) && (sizeLin >= sizeHC)) {
      int prePostBits = getBitPos_PostIndex(DIM);
      setPostHC( true );
      long[] bia2 = Bits.arrayCreate(calcArraySizeTotalBits(bufPostCnt+1, DIM));
      T [] v2 = Refs.arrayCreate(1<<DIM);
      //Copy only bits that are relevant. Otherwise we might mess up the not-null table!
      Bits.copyBitsLeft(ba, 0, bia2, 0, prePostBits);
      int postLenTotal = DIM*postLen; 
      for (int i = 0; i < bufPostCnt; i++) {
        int entryPosLHC = prePostBits + i*(PIK_WIDTH(DIM)+postLenTotal);
        int p2 = (int)Bits.readArray(ba, entryPosLHC, PIK_WIDTH(DIM));
        Bits.setBit(bia2, prePostBits+PINN_HC_WIDTH*p2, true);
        Bits.copyBitsLeft(ba, entryPosLHC+PIK_WIDTH(DIM),
            bia2, prePostBits + (1<<DIM)*PINN_HC_WIDTH + postLenTotal*p2, 
            postLenTotal);
        v2[p2] = values[i];
      }
      ba = bia2;
      values = v2;
      offsPostKey = getPostOffsetBits(pos, DIM);
    }


    //get position
    offsPostKey = -(offsPostKey+1);

    //subBcnt++;
    setPostCount(bufPostCnt+1);

    if (isPostHC()) {
      //hyper-cube
      for (int i = 0; i < key.length; i++) {
        Bits.writeArray(ba, offsPostKey + postLen * i, postLen, key[i]);
      }
      int offsNN = getBitPos_PostIndex(DIM);
      Bits.setBit(ba, (int) (offsNN+PINN_HC_WIDTH*pos), true);
      values[(int) pos] = value;
    } else {
      long[] ia;
      int offs;
      if (!isPostNI()) {
        //resize array
        ba = Bits.arrayEnsureSize(ba, calcArraySizeTotalBits(bufPostCnt+1, DIM));
        ia = ba;
        offs = offsPostKey;
        Bits.insertBits(ia, offs-PIK_WIDTH(DIM), PIK_WIDTH(DIM) + DIM*postLen);
        //insert key
        Bits.writeArray(ia, offs-PIK_WIDTH(DIM), PIK_WIDTH(DIM), pos);
        //insert value:
        for (int i = 0; i < DIM; i++) {
          Bits.writeArray(ia, offs + postLen * i, postLen, key[i]);
        }
        values = Refs.arrayEnsureSize(values, bufPostCnt+1);
        Refs.insertAtPos(values, offs2ValPos(offs, pos, DIM), value);
      } else {
        throw new IllegalStateException();
      }

    }
  }

  long[] postToNI(int startBit, int postLen, int DIM) {
    long[] key = new long[DIM];
    for (int d = 0; d < key.length; d++) {
      key[d] |= Bits.readArray(ba, startBit, postLen);
      startBit += postLen;
    }
    return key;
  }

  void postFromNI(long[] ia, int startBit, long key[], int postLen) {
    //insert value:
    for (int d = 0; d < key.length; d++) {
      Bits.writeArray(ia, startBit + postLen * d, postLen, key[d]);
    }
  }

  void niBuild(int bufSubCnt, int bufPostCnt, int DIM) {
    //Migrate node to node-index representation
    if (ind != null || isPostNI() || isSubNI()) {
      throw new IllegalStateException();
    }
    ind = CritBit64COW.create();

    //read posts 
    if (isPostHC()) {
      int prePostBitsKey = getBitPos_PostIndex(DIM);
      int prePostBitsVal = prePostBitsKey + (1<<DIM)*PINN_HC_WIDTH;
      int postLenTotal = DIM*postLen;
      for (int i = 0; i < (1L<<DIM); i++) {
        if (Bits.getBit(ba, prePostBitsKey + PINN_HC_WIDTH*i)) {
          int postPosLHC = prePostBitsVal + i*postLenTotal;
          //Bits.writeArray(bia2, entryPosLHC, PIK_WIDTH(DIM), i);
          //Bits.copyBitsLeft(
          //		ba, prePostBits + (1<<DIM)*PINN_HC_WIDTH + postLenTotal*i, 
          //		bia2, entryPosLHC+PIK_WIDTH(DIM),
          //		postLenTotal);
          long[] key = postToNI(postPosLHC, postLen, DIM);
          postPosLHC += DIM*postLen;
          niPutNoCopy(i, key, values[i]);
        }
      }
    } else {
      int prePostBits = getBitPos_PostIndex(DIM);
      int postPosLHC = prePostBits;
      for (int i = 0; i < bufPostCnt; i++) {
        //int entryPosLHC = prePostBits + i*(PIK_WIDTH(DIM)+postLenTotal);
        long p2 = Bits.readArray(ba, postPosLHC, PIK_WIDTH(DIM));
        postPosLHC += PIK_WIDTH(DIM);
        //This reads compressed keys...
        //	Bits.setBit(bia2, prePostBits+PINN_HC_WIDTH*p2, true);
        //	Bits.copyBitsLeft(ba, entryPosLHC+PIK_WIDTH(DIM),
        //			bia2, prePostBits + (1<<DIM)*PINN_HC_WIDTH + postLenTotal*p2, 
        //			postLenTotal);
        long[] key = postToNI(postPosLHC, postLen, DIM);
        postPosLHC += DIM*postLen;

        niPutNoCopy(p2, key, values[i]);
      }
    }

    //sub nodes
    if (isSubHC()) {
      for (int i = 0; i < (1L<<DIM); i++) {
        if (subNRef[i] != null) {
          niPut(i, subNRef[i]);
        }
      }
    } else {
      int subOffsBits = getBitPos_SubNodeIndex(DIM);
      for (int i = 0; i < bufSubCnt; i++) {
        long posOld = Bits.readArray(ba, subOffsBits, SIK_WIDTH(DIM));
        subOffsBits += SIK_WIDTH(DIM);
        niPut(posOld, subNRef[i]);
      }
    }

    setPostHC(false);
    setSubHC(false);
    setPostNI(true);
    setSubNI(true);
    ba = Bits.arrayTrim(ba, calcArraySizeTotalBitsNI(DIM));
    subNRef = null;
    values = null; 
  }

  /**
   * 
   * @param bufSubCnt
   * @param bufPostCnt
   * @param DIM
   * @param posToRemove
   * @param removeSub Remove sub or post?
   * @return Previous value if post was removed
   */
  @SuppressWarnings("unchecked")
  T niDeconstruct(int DIM, long posToRemove, boolean removeSub) {
    //Migrate node to node-index representation
    if (ind == null || !isPostNI() || !isSubNI()) {
      throw new IllegalStateException();
    }

    setPostNI(false);
    setSubNI(false);
    final int newSubCnt;
    final int newPostCnt;
    if (removeSub) {
      newSubCnt = getSubCount()-1;
      newPostCnt = getPostCount();
      setSubCount(newSubCnt);
    } else {
      newSubCnt = getSubCount();
      newPostCnt = getPostCount()-1;
      setPostCount(newPostCnt);
    }

    //calc post mode.
    //+1 bit for null/not-null flag
    long sizePostHC = (DIM * postLen + PINN_HC_WIDTH) * (1L << DIM); 
    //+DIM because every index entry needs DIM bits
    long sizePostLin = (DIM * postLen + PIK_WIDTH(DIM)) * newPostCnt;
    boolean isPostHC = (DIM<=31) && (sizePostLin >= sizePostHC);
    setPostHC(isPostHC);



    //sub-nodes:
    //switch to normal array (full hyper-cube) if applicable.
    if (DIM<=31 && (REF_BITS+SIK_WIDTH(DIM))*newSubCnt >= REF_BITS*(1L<<DIM)) {
      //migrate to full HC array
      Node<T>[] na = new Node[1<<DIM];
      CBIterator<NodeEntry<T>> it = ind.iterator();
      while (it.hasNext()) {
        Entry<NodeEntry<T>> e = it.nextEntry();
        if (e.value().node != null && e.key() != posToRemove) {
          na[(int) e.key()] = e.value().node;
        }
      }
      subNRef = na;
      setSubHC(true);
    } else {
      //migrate to LHC
      setSubHC( false );
      int bia2Size = calcArraySizeTotalBits(newPostCnt, DIM);
      long[] bia2 = Bits.arrayCreate(bia2Size);
      Node<T>[] sa2 = new Node[newSubCnt];
      int preSubBits = getBitPos_SubNodeIndex(DIM);
      //Copy only bits that are relevant. Otherwise we might mess up the not-null table!
      Bits.copyBitsLeft(ba, 0, bia2, 0, preSubBits);
      int n=0;
      CBIterator<NodeEntry<T>> it = ind.iterator();
      while (it.hasNext()) {
        Entry<NodeEntry<T>> e = it.nextEntry();
        if (e.value().node != null) {
          long pos = e.key();
          if (pos == posToRemove) {
            //skip the item that should be deleted.
            continue;
          }
          sa2[n] = e.value().node;
          Bits.writeArray(bia2, preSubBits + n*SIK_WIDTH(DIM), SIK_WIDTH(DIM), pos);
          n++;
        }
      }
      ba = bia2;
      subNRef = sa2;
    }

    //post-data:
    T oldValue = null;
    int prePostBits = getBitPos_PostIndex(DIM);
    long[] bia2 = Bits.arrayCreate(calcArraySizeTotalBits(newPostCnt, DIM));
    //Copy only bits that are relevant. Otherwise we might mess up the not-null table!
    Bits.copyBitsLeft(ba, 0, bia2, 0, prePostBits);
    int postLenTotal = DIM*postLen;
    if (isPostHC) {
      //HC mode
      T [] v2 = Refs.arrayCreate(1<<DIM);
      int startBitBase = prePostBits + (1<<DIM)*PINN_HC_WIDTH;
      CBIterator<NodeEntry<T>> it = ind.iterator();
      while (it.hasNext()) {
        Entry<NodeEntry<T>> e = it.nextEntry();
        if (e.value().getKey() != null) {
          if (e.key() == posToRemove) {
            oldValue = e.value().getValue();
            continue;
          }
          int p2 = (int) e.key();
          Bits.setBit(bia2, prePostBits+PINN_HC_WIDTH*p2, true);
          int startBit = startBitBase + postLenTotal*p2;
          postFromNI(bia2, startBit, e.value().getKey(), postLen);
          v2[p2] = e.value().getValue();
        }
      }
      ba = bia2;
      values = v2;
    } else {
      //LHC mode
      T[] v2 = Refs.arrayCreate(newPostCnt);
      int n=0;
      CBIterator<NodeEntry<T>> it = ind.iterator();
      int entryPosLHC = prePostBits;
      while (it.hasNext()) {
        Entry<NodeEntry<T>> e = it.nextEntry();
        long pos = e.key();
        if (e.value().getKey() != null) {
          if (pos == posToRemove) {
            //skip the item that should be deleted.
            oldValue = e.value().getValue();
            continue;
          }
          v2[n] = e.value().getValue();
          Bits.writeArray(bia2, entryPosLHC, PIK_WIDTH(DIM), pos);
          entryPosLHC += PIK_WIDTH(DIM);
          postFromNI(bia2, entryPosLHC, e.value().getKey(), postLen);
          entryPosLHC += postLenTotal;
          n++;
        }
      }
      ba = bia2;
      values = v2;
    }			

    if (newPostCnt == 0) {
      values = null;
    }
    ind = null;
    return oldValue;
  }


  T getPostPOB(int offsPostKey, long pos, long[] key) {
    if (isPostNI()) {
      final long mask = (~0L)<<postLen;
      NodeEntry<T> e = niGet(pos);
      long[] eKey = e.getKey();
      for (int i = 0; i < key.length; i++) {
        key[i] &= mask;
        key[i] |= eKey[i];
      }
      //System.arraycopy(e.getKey(), 0, key, 0, key.length);
      return e.getValue();
    }

    long[] ia = ba;
    int offs;
    offs = offsPostKey;
    int valPos = offs2ValPos(offs, pos, key.length);
    final long mask = (~0L)<<postLen;
    for (int i = 0; i < key.length; i++) {
      key[i] &= mask;
      key[i] |= Bits.readArray(ia, offs, postLen);
      offs += postLen;
    }
    return values[valPos];   
  }


  /**
   * Get post-fix.
   * @param offsPostKey
   * @param key
   * @param range After the method call, this contains the postfix if the postfix matches the
   * range. Otherwise it contains only part of the postfix.
   * @return NodeEntry if the postfix matches the range, otherwise null.
   * @Deprecated Use next method instead.
   */
  NodeEntry<T> getPostPOB(int offsPostKey, long hcPos, long[] key, 
      long[] rangeMin, long[] rangeMax) {

    long[] ia = ba;
    int offs = offsPostKey;
    final long mask = (~0L)<<postLen;
    for (int i = 0; i < key.length; i++) {
      key[i] &= mask;
      key[i] |= Bits.readArray(ia, offs, postLen);
      if (key[i] < rangeMin[i] || key[i] > rangeMax[i]) {
        return null;
      }
      offs += postLen;
    }
    int valPos = offs2ValPos(offsPostKey, hcPos, key.length);
    return createNodeEntry(key, values[valPos]);
  }


  /**
   * Get post-fix.
   * @param offsPostKey
   * @param key
   * @param range After the method call, this contains the postfix if the postfix matches the
   * range. Otherwise it contains only part of the postfix.
   * @return NodeEntry if the postfix matches the range, otherwise null.
   */
  boolean getPostPOB(int offsPostKey, long hcPos, PhEntry<T> e, 
      long[] rangeMin, long[] rangeMax) {
    long[] ia = ba;
    int offs = offsPostKey;
    long[] key = e.getKey();
    final long mask = (~0L)<<postLen;
    for (int i = 0; i < key.length; i++) {
      key[i] &= mask;
      key[i] |= Bits.readArray(ia, offs, postLen);
      if (key[i] < rangeMin[i] || key[i] > rangeMax[i]) {
        return false;
      }
      offs += postLen;
    }
    int valPos = offs2ValPos(offsPostKey, hcPos, key.length);
    e.setValue(values[valPos]);
    return true;
  }


  /**
   * Get post-fix.
   * @param offsPostKey
   * @param hcPos
   * @param key
   * @return NodeEntry if the postfix matches the range, otherwise null.
   */
  NodeEntry<T> getPostPOB_E(int offsPostKey, long hcPos, long[] key) {
    long[] ia = ba;
    int offs = offsPostKey;
    final long mask = (~0L)<<postLen;
    for (int i = 0; i < key.length; i++) {
      key[i] &= mask;
      key[i] |= Bits.readArray(ia, offs, postLen);
      offs += postLen;
    }
    int valPos = offs2ValPos(offsPostKey, hcPos, key.length);
    return createNodeEntry(key, values[valPos]);
  }


  /**
   * Get post-fix.
   * @param offsPostKey
   * @param key
   * @param range After the method call, this contains the postfix if the postfix matches the
   * range. Otherwise it contains only part of the postfix.
   * @return true, if the postfix matches the range.
   */
  @SuppressWarnings("unchecked")
  PhEntry<T> getPostPOB(int offsPostKey, long hcPos, int DIM, long[] valTemplate,
      long[] rangeMin, long[] rangeMax, int[] minToCheck, int[] maxToCheck) {
    long[] ia = ba;

    int valPos = offs2ValPos(offsPostKey, hcPos, DIM);
    T val = values[valPos];
    if (val instanceof PhEntry) {
      long[] key = ((PhEntry<T>)val).getKey(); 
      for (int i: minToCheck) {
        if (key[i] < rangeMin[i]) {
          return null;
        }
      }
      for (int i: maxToCheck) {
        if (key[i] > rangeMax[i]) {
          return null;
        }
      }
      return (PhEntry<T>) val;
    }

    long[] key = new long[DIM];
    System.arraycopy(valTemplate, 0, key, 0, DIM);
    PhTreeHelper.applyHcPos(hcPos, postLen, key);

    final long mask = (~0L)<<postLen;
    for (int i: minToCheck) {
      key[i] &= mask;
      key[i] |= Bits.readArray(ia, offsPostKey+i*postLen, postLen);
      if (key[i] < rangeMin[i]) {
        return null;
      }
    }
    for (int i: maxToCheck) {
      key[i] &= mask;
      key[i] |= Bits.readArray(ia, offsPostKey+i*postLen, postLen);
      if (key[i] > rangeMax[i]) {
        return null;
      }
    }


    int offs = offsPostKey;
    for (int i = 0; i < key.length; i++) {
      key[i] &= mask;
      key[i] |= Bits.readArray(ia, offs, postLen);
      offs += postLen;
    }
    return createNodeEntry(key, val);
  }

  /**
   * Same as above, but without checks.
   */
  @SuppressWarnings("unchecked")
  PhEntry<T> getPostPOBNoCheck(int offsPostKey, long hcPos, int DIM, long[] valTemplate,
      long[] rangeMin, long[] rangeMax) {
    int valPos = offs2ValPos(offsPostKey, hcPos, DIM);
    T val = values[valPos];
    if (val instanceof PhEntry) {
      return (PhEntry<T>) val;
    }

    long[] key = new long[DIM];
    System.arraycopy(valTemplate, 0, key, 0, DIM);
    PhTreeHelper.applyHcPos(hcPos, postLen, key);
    if (DEBUG_FULL) {
      //verify that we don't have keys here that can't possibly match...
      final long mask = (~0L)<<postLen;
      for (int i = 0; i < key.length; i++) {
        key[i] &= mask;
        if (key[i] < (rangeMin[i]&mask) || key[i] > (rangeMax[i]&mask)) {
          throw new IllegalStateException("k=" + key[i] + " m/m=" + rangeMin[i] +
              "/" + rangeMax[i]);
        }
      }
    }

    long[] ia = ba;
    int offs = offsPostKey;
    final long mask = (~0L)<<postLen;
    for (int i = 0; i < key.length; i++) {
      key[i] &= mask;
      key[i] |= Bits.readArray(ia, offs, postLen);
      offs += postLen;
    }
    return new PhEntry<T>(key, val);
  }

  T getPostValuePOB(int offs, long pos, int DIM) {
    if (!isPostNI()) {
      int valPos = offs2ValPos(offs, pos, DIM);
      return values[valPos];
    } 

    return niGet(pos).getValue();
  }


  T updatePostValuePOB(int offs, long pos, long[] key, int DIM, T value) {
    if (!isPostNI()) {
      int valPos = offs2ValPos(offs, pos, DIM);
      T old = values[valPos];
      values[valPos] = value;
      return old;
    } 

    return niPutNoCopy(pos, key, value).getValue(); 
  }


  T getPostValue(long pos, int DIM) {
    if (isPostHC()) {
      return getPostValuePOB(PhTree8.UNKNOWN, pos, PhTree8.UNKNOWN); 
    }
    int offs = getPostOffsetBits(pos, DIM);
    return getPostValuePOB(offs, pos, DIM);
  }


  T getPost(long pos, long[] key) {
    if (isPostNI()) {
      return getPostPOB(-1, pos, key);
    }
    final int DIM = key.length;
    int offs = getPostOffsetBits(pos, DIM);
    return getPostPOB(offs, pos, key);
  }


  T removePostPOB(long pos, int offsPostKey, final int DIM) {
    final int bufPostCnt = getPostCount();
    final int bufSubCnt = getSubCount();

    if (isPostNI()) {
      if (!NI_THRESHOLD(bufSubCnt, bufPostCnt)) {
        T v = niDeconstruct(DIM, pos, false);
        return v;
      }
    }
    if (isPostNI()) {
      setPostCount(bufPostCnt-1);
      return niRemove(pos).getValue();
    }

    T oldVal = null;

    //switch representation (HC <-> Linear)?
    //+1 bit for null/not-null flag
    long sizeHC = (DIM * postLen + PINN_HC_WIDTH) * (1L << DIM); 
    //+DIM assuming compressed IDs
    long sizeLin = (DIM * postLen + PIK_WIDTH(DIM)) * (bufPostCnt-1L);
    if (isPostHC() && (sizeLin < sizeHC)) {
      //revert to linearized representation, if applicable
      setPostHC( false );
      long[] bia2 = Bits.arrayCreate(calcArraySizeTotalBits(bufPostCnt-1, DIM));
      T[] v2 = Refs.arrayCreate(bufPostCnt);
      int prePostBits = getBitPos_PostIndex(DIM);
      int prePostBitsVal = prePostBits + (1<<DIM)*PINN_HC_WIDTH;
      //Copy only bits that are relevant. Otherwise we might mess up the not-null table!
      Bits.copyBitsLeft(ba, 0, bia2, 0, prePostBits);
      int postLenTotal = DIM*postLen;
      int n=0;
      for (int i = 0; i < (1L<<DIM); i++) {
        if (i==pos) {
          //skip the item that should be deleted.
          oldVal = values[i];
          continue;
        }
        if (Bits.getBit(ba, prePostBits + PINN_HC_WIDTH*i)) {
          int entryPosLHC = prePostBits + n*(PIK_WIDTH(DIM)+postLenTotal);
          Bits.writeArray(bia2, entryPosLHC, PIK_WIDTH(DIM), i);
          Bits.copyBitsLeft(
              ba, prePostBitsVal + postLenTotal*i, 
              bia2, entryPosLHC+PIK_WIDTH(DIM),
              postLenTotal);
          v2[n] = values[i];
          n++;
        }
      }
      ba = bia2;
      values = v2;
      //subBcnt--;
      setPostCount(bufPostCnt-1);
      if (bufPostCnt-1 == 0) {
        values = null;
      }
      return oldVal;
    }			

    //subBcnt--;
    setPostCount(bufPostCnt-1);

    if (isPostHC()) {
      //hyper-cube
      int offsNN = getBitPos_PostIndex(DIM);
      Bits.setBit(ba, (int) (offsNN+PINN_HC_WIDTH*pos), false);
      oldVal = values[(int) pos]; 
      values[(int) pos] = null;
      //Nothing else to do, values can just stay where they are
    } else {
      if (!isPostNI()) {
        //linearized cube:
        //remove key and value
        Bits.removeBits(ba, offsPostKey-PIK_WIDTH(DIM), PIK_WIDTH(DIM) + DIM*postLen);
        //shrink array
        ba = Bits.arrayTrim(ba, calcArraySizeTotalBits(bufPostCnt-1, DIM));
        //values:
        int valPos = offs2ValPos(offsPostKey, pos, DIM);
        oldVal = values[valPos]; 
        Refs.removeAtPos(values, valPos);
        values = Refs.arrayTrim(values, bufPostCnt-1);
      } else {
        throw new IllegalStateException();
      }
    }
    if (bufPostCnt-1 == 0) {
      values = null;
    }
    return oldVal;
  }


  /**
   * @return True if the post-fixes are stored as hyper-cube
   */
  boolean isPostHC() {
    return (isHC & 0b10) != 0;
    //return Bits.getBit(ba, 0);
  }


  /**
   * Set whether the post-fixes are stored as hyper-cube.
   */
  void setPostHC(boolean b) {
    isHC = (byte) (b ? (isHC | 0b10) : (isHC & (~0b10)));
    //Bits.setBit(ba, 0, b);
  }


  /**
   * @return True if the sub-nodes are stored as hyper-cube
   */
  boolean isSubHC() {
    return (isHC & 0b01) != 0;
    //return Bits.getBit(ba, 1);
  }


  /**
   * Set whether the sub-nodes are stored as hyper-cube.
   */
  void setSubHC(boolean b) {
    isHC = (byte) (b ? (isHC | 0b01) : (isHC & (~0b01)));
    //Bits.setBit(ba, 1, b);
  }


  /**
   * @return True if the sub-nodes are stored as hyper-cube
   */
  boolean isSubLHC() {
    //bit 0 and 2 = 1+4
    return (isHC & 0b101) == 0;
  }

  boolean isPostLHC() {
    //bit 1 and 3 = 2+8
    return (isHC & 0b110) == 0;
  }


  boolean isPostNI() {
    return (isHC & 0b100) != 0;
  }


  void setPostNI(boolean b) {
    isHC = (byte) (b ? (isHC | 0b100) : (isHC & (~0b100)));
  }


  boolean isSubNI() {
    return isPostNI();
  }


  void setSubNI(boolean b) {
    setPostNI(b);
  }


  /**
   * @return Post-fix counter
   */
  int getPostCount() {
    return postCnt;
  }


  /**
   * Set post-fix counter.
   */
  void setPostCount(int cnt) {
    postCnt = cnt;
  }


  /**
   * @return Sub-node counter
   */
  int getSubCount() {
    return subCnt;
  }


  /**
   * Set sub-node counter.
   */
  void setSubCount(int cnt) {
    subCnt = cnt;
  }


  /**
   * Posts start after sub-index.
   * Sub-index is empty in case of sub-hypercube.
   * @return Position of first bit of post index or not-null table.
   */
  int getBitPos_PostIndex(final int DIM) {
    int offsOfSubs = 0;
    //subHC and subNI require no space
    if (isSubLHC()) {
      //linearized cube
      offsOfSubs = getSubCount() * SIK_WIDTH(DIM); 
    }
    return getBitPos_SubNodeIndex(DIM) + offsOfSubs; 
  }

  int getBitPos_SubNodeIndex(final int DIM) {
    return getBitPos_Infix() + (infixLen*DIM);
  }

  int getBitPos_Infix() {
    // isPostHC / isSubHC / postCount / subCount
    return HC_BITS;//   +   DIM+1   +   DIM+1;
  }

  /**
   * 
   * @param offs
   * @param pos
   * @param DIM
   * @param bufSubCnt use -1 to have it calculated by this method
   * @return
   */
  private int offs2ValPos(int offs, long pos, int DIM) {
    if (isPostHC()) {
      return (int) pos;
    } else {
      int offsInd = getBitPos_PostIndex(DIM);
      //get p2 of:
      //return p2 * (PIK_WIDTH(DIM) + postLen * DIM) + offsInd + PIK_WIDTH(DIM);
      int valPos = (offs - PIK_WIDTH(DIM) - offsInd) / (postLen*DIM+PIK_WIDTH(DIM)); 
      return valPos;
    }
  }


  /**
   * 
   * @param pos
   * @param DIM
   * @return 		The position (in bits) of the postfix VALUE. For LHC, the key is stored 
   * 				directly before the value.
   */
  int getPostOffsetBits(long pos, final int DIM) {
    int offsInd = getBitPos_PostIndex(DIM);
    if (isPostHC()) {
      //hyper-cube
      int posInt = (int) pos;  //Hypercube can not be larger than 2^31
      boolean notNull = Bits.getBit(ba, offsInd+PINN_HC_WIDTH*posInt);
      offsInd += PINN_HC_WIDTH*(1<<DIM);
      if (!notNull) {
        return -(posInt * postLen * DIM + offsInd)-1;
      }
      return posInt * postLen * DIM + offsInd;
    } else {
      if (!isPostNI()) {
        //linearized cube
        int p2 = Bits.binarySearch(ba, offsInd, getPostCount(), pos, PIK_WIDTH(DIM), 
            DIM * postLen);
        if (p2 < 0) {
          p2 = -(p2+1);
          p2 *= (PIK_WIDTH(DIM) + postLen * DIM);
          p2 += PIK_WIDTH(DIM);
          return -(p2 + offsInd) -1;
        }
        return p2 * (PIK_WIDTH(DIM) + postLen * DIM) + offsInd + PIK_WIDTH(DIM);
      } else {
        NodeEntry<T> e = niGet(pos);
        if (e != null && e.getKey() != null) {
          return (int)pos;
        }
        return (int) (-pos -1);
      }
    }
  }

  boolean hasPostFix(long pos, final int DIM) {
    if (!isPostNI()) {
      return getPostOffsetBits(pos, DIM) >= 0;
    }
    NodeEntry<T> e = niGet(pos);
    return (e != null) && (e.getKey() != null);
  }

  /**
   * Adjust the infix in cases were the parent node is removed.
   * @param infix
   */
  public void adjustInfix(long[] prefix, int infixLenOfParent, int postLenOfParent, long hcPos) {
    applyHcPos(hcPos, postLenOfParent, prefix);
    getInfixNoOverwrite(prefix);
    int DIM = prefix.length;
    // update infix-len and resize array
    int infOffs = getBitPos_Infix();
    int newInfixLen = infixLenOfParent + 1 + getInfixLen();
    setInfixLen(newInfixLen);
    ba = Bits.arrayEnsureSize(ba, calcArraySizeTotalBits(
        getPostCount(), DIM));
    Bits.insertBits(ba, infOffs, DIM*(infixLenOfParent+1));

    // update infix
    writeInfix(prefix);
  }

  int getInfixLen() {
    return infixLen;
  }

  void setInfixLen(int newInfLen) {
    infixLen = (byte) newInfLen;
  }

  int getPostLen() {
    return postLen;
  }
  Node<T> subNRef(int pos) {
    return subNRef[pos];
  }
  CritBit64COW<NodeEntry<T>> ind() {
    return ind;
  }

  CBIterator<NodeEntry<T>> niIterator() {
    return ind.iterator();
  }

  Node<T>[] subNRef() {
    return subNRef;
  }

  long getVersion() {
    //this is properly implemented in the sub-classes
    throw new UnsupportedOperationException();
  }

  void lockRLock() {
    //this is properly implemented in the sub-classes
    throw new UnsupportedOperationException();
  }

  void unlockRLock() {
    //this is properly implemented in the sub-classes
    throw new UnsupportedOperationException();
  }

  void lockWLock() {
    //this is properly implemented in the sub-classes
    throw new UnsupportedOperationException();
  }

  void unlockWLock() {
    //this is properly implemented in the sub-classes
    throw new UnsupportedOperationException();
  }
}
