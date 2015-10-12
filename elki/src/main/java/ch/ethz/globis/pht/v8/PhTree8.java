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

import static ch.ethz.globis.pht.PhTreeHelper.align8;
import static ch.ethz.globis.pht.PhTreeHelper.applyHcPos;
import static ch.ethz.globis.pht.PhTreeHelper.debugCheck;
import static ch.ethz.globis.pht.PhTreeHelper.getMaxConflictingBitsWithMask;
import static ch.ethz.globis.pht.PhTreeHelper.posInArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

import ch.ethz.globis.pht.PhDimFilter;
import ch.ethz.globis.pht.PhDistance;
import ch.ethz.globis.pht.PhDistanceL;
import ch.ethz.globis.pht.PhEntry;
import ch.ethz.globis.pht.PhTree;
import ch.ethz.globis.pht.PhTreeConfig;
import ch.ethz.globis.pht.PhTreeHelper;
import ch.ethz.globis.pht.util.PhTreeQStats;
import ch.ethz.globis.pht.util.StringBuilderLn;

/**
 * n-dimensional index (quad-/oct-/n-tree).
 * 
 * Hypercube: expanded byte array that contains 2^DIM references to sub-nodes (and posts, depending 
 * on implementation)
 * Linearization: Storing Hypercube as paired array of index<->non_null_reference 
 *
 * See also : T. Zaeschke, C. Zimmerli, M.C. Norrie; 
 * "The PH-Tree -- A Space-Efficient Storage Structure and Multi-Dimensional Index", 
 * (SIGMOD 2014)
 *
 * @author ztilmann (Tilmann Zaeschke)
 * 
 * @param <T> The value type of the tree 
 *
 */
public class PhTree8<T> extends PhTree<T> {

	//Enable HC incrementer / iteration
	static final boolean HCI_ENABLED = true; 
	
	static final int DEPTH_64 = 64;

	/** If the minMask is larger than the threshold, then the first value in a node iterator
	 * is looked up by binary search instead of full search. */
	static final int USE_MINMASK_BINARY_SEARCH_THRESHOLD = 10;

	//Dimension. This is the number of attributes of an entity.
	private final int DIM;

	private final AtomicInteger nEntries = new AtomicInteger();
	private final AtomicInteger nNodes = new AtomicInteger();

	static final int UNKNOWN = -1;

    private PhOperations<T> operations = new PhOperationsSimple<>(this);

    final long[] MIN;
    private final long[] MAX;

    static final class NodeEntry<T> extends PhEntry<T> {
      Node<T> node;
      NodeEntry(long[] key, T value) {
        super(key, value);
        this.node = null;
      }
      NodeEntry(Node<T> node) {
        super(null, null);
        this.node = node;
      }

      void setNode(Node<T> node) {
        set(null, null);
        this.node = node;
      }
      void setPost(long[] key, T val) {
        set(key, val);
        this.node = null;
      }
    }


    private Node<T> root = null;

    Node<T> getRoot() {
      return root;
    }

    void changeRoot(Node<T> newRoot) {
      this.root = newRoot;
    }

    public PhTree8(int dim) {
      DIM = dim;
		MIN = new long[DIM];
		Arrays.fill(MIN, Long.MIN_VALUE);
		MAX = new long[DIM];
		Arrays.fill(MAX, Long.MAX_VALUE);
		debugCheck();
	}

	protected PhTree8(PhTreeConfig cnf) {
		DIM = cnf.getDimActual();
		MIN = new long[DIM];
		MAX = new long[DIM];
		Arrays.fill(MAX, Long.MAX_VALUE);
		Arrays.fill(MIN, Long.MIN_VALUE);
		debugCheck();
	}

	void increaseNrNodes() {
		nNodes.incrementAndGet();
	}

	void decreaseNrNodes() {
		nNodes.decrementAndGet();
	}

	void increaseNrEntries() {
		nEntries.incrementAndGet();
	}

	void decreaseNrEntries() {
		nEntries.decrementAndGet();
	}

	@Override
	public int size() {
		return nEntries.get();
	}

	@Override
	public int getNodeCount() {
		return nNodes.get();
	}

	@Override
	public PhTreeQStats getQuality() {
		return getQuality(0, getRoot(), new PhTreeQStats(DEPTH_64));
	}

	private PhTreeQStats getQuality(int currentDepth, Node<T> node, PhTreeQStats stats) {
		stats.nNodes++;
		if (node.isPostHC()) {
			stats.nHCP++;
		}
		if (node.isSubHC()) {
			stats.nHCS++;
		}
		if (node.isPostNI()) {
			stats.nNI++;
		}
		stats.infixHist[node.getInfixLen()]++;
		stats.nodeDepthHist[currentDepth]++;
		int size = node.getPostCount() + node.getSubCount();
		stats.nodeSizeLogHist[32-Integer.numberOfLeadingZeros(size)]++;
		
		currentDepth += node.getInfixLen();
		stats.q_totalDepth += currentDepth;

		if (node.subNRef() != null) {
			for (Node<T> sub: node.subNRef()) {
				if (sub != null) {
					getQuality(currentDepth + 1, sub, stats);
				}
			}
		} else {
			if (node.ind() != null) {
				for (NodeEntry<T> n: node.ind()) {
					if (n.node != null) {
						getQuality(currentDepth + 1, n.node, stats);
					}
				}
			}
		}

		//count post-fixes
		stats.q_nPostFixN[currentDepth] += node.getPostCount();

		return stats;
	}


	@Override
	public PhTreeHelper.Stats getStats() {
		return getStats(0, getRoot(), new PhTreeHelper.Stats());
	}

	private PhTreeHelper.Stats getStats(int currentDepth, Node<T> node, PhTreeHelper.Stats stats) {
		final int REF = 4;//bytes for a reference
		stats.nNodes++;
		// this +  ref-SubNRef[] + ref-subB[] + refInd + refVal[] + infLen + infOffs
		stats.size += align8(12 + REF + REF + REF +  REF + 1 + 1 + 1 + 1);

		currentDepth += node.getInfixLen();
		int nChildren = 0;
		if (node.isPostNI()) {
			nChildren += node.ind().size();
			stats.size += (node.ind().size()-1) * 48 + 40;
			if (node.getSubCount() == 0) {
				stats.nLeafNodes++;
			} else {
				stats.nInnerNodes++;
			}
			for (NodeEntry<T> e: node.ind()) {
				stats.size += 24; //e
				if (e.node != null) {
					getStats(currentDepth + 1, e.node, stats);
				} else {
					//count post-fixes
					stats.size += 16 + e.getKey().length*8;
				}
			}
		} else {
			if (node.subNRef() != null) {
				stats.size += 16 + align8(node.subNRef().length * REF);
				stats.nInnerNodes++;
				for (Node<T> sub: node.subNRef()) {
					if (sub != null) {
						nChildren++;
						getStats(currentDepth + 1, sub, stats);
					}
				}
				stats.nSubOnly += nChildren;
			} else {
				stats.nLeafNodes++;
			}
			nChildren += node.getPostCount();
			//count post-fixes
			stats.size += 16 + align8(Bits.arraySizeInByte(node.ba));
		}


		if (nChildren == 1 && nEntries.get() > 1) {
			//This should not happen! Except for a root node if the tree has <2 entries.
			System.err.println("WARNING: found lonely node..." + (node == getRoot()));
			stats.nLonely++;
		}
		if (nChildren == 0) {
			//This should not happen! Except for a root node if the tree has <2 entries.
			System.err.println("WARNING: found ZOMBIE node..." + (node == getRoot()));
			stats.nLonely++;
		}
		stats.nChildren += nChildren;
		return stats;
	}

	@Override
	public PhTreeHelper.Stats getStatsIdealNoNode() {
		return getStatsIdealNoNode(0, getRoot(), new PhTreeHelper.Stats());
	}

	private PhTreeHelper.Stats getStatsIdealNoNode(int currentDepth, Node<T> node, PhTreeHelper.Stats stats) {
		final int REF = 4;//bytes for a reference
		stats.nNodes++;

		// 16=object[] +  16=byte[] + value[]
		stats.size += 16 + 16 + 16;

		//  infixLen + isHC + + postlen 
		stats.size += 1 + 1 + 1 + 4 * REF;

		int sizeBA = 0;
		sizeBA = node.calcArraySizeTotalBits(node.getPostCount(), DIM);
		sizeBA = Bits.calcArraySize(sizeBA);
		sizeBA = Bits.arraySizeInByte(sizeBA);
		stats.size += align8(sizeBA);

		currentDepth += node.getInfixLen();
		int nChildren = 0;

		if (node.isPostNI()) {
			nChildren = node.ind().size();
			stats.size += (nChildren-1) * 48 + 40;
			if (node.getSubCount() == 0) {
				stats.nLeafNodes++;
			} else {
				stats.nInnerNodes++;
			}
			for (NodeEntry<T> e: node.ind()) {
				stats.size += 24; //e
				if (e.node != null) {
					getStatsIdealNoNode(currentDepth + 1, e.node, stats);
				} else {
					//count post-fixes
					stats.size += 16 + e.getKey().length*8;
				}
			}
		} else {
			if (node.isSubHC()) {
				stats.nHCS++;
			}
			if (node.subNRef() != null) {
				//+ REF for the byte[]
				stats.size += align8(node.getSubCount() * REF + REF);
				stats.nInnerNodes++;
				for (Node<T> sub: node.subNRef()) {
					if (sub != null) {
						nChildren++;
						getStatsIdealNoNode(currentDepth + 1, sub, stats);
					}
				}
				stats.nSubOnly += nChildren;
			} else {
				//byte[] ref
				stats.size += align8(1 * REF);
				stats.nLeafNodes++;
			}

			//count post-fixes
			nChildren += node.getPostCount();
			if (node.isPostHC()) {
				stats.nHCP++;
			}
		}


		stats.nChildren += nChildren;

		//sanity checks
		if (nChildren == 1) {
			//This should not happen! Except for a root node if the tree has <2 entries.
			System.err.println("WARNING: found lonely node..." + (node == getRoot()));
			stats.nLonely++;
		}
		if (nChildren == 0) {
			//This should not happen! Except for a root node if the tree has <2 entries.
			System.err.println("WARNING: found ZOMBIE node..." + (node == getRoot()));
			stats.nLonely++;
		}
		if (node.isPostHC() && node.isSubHC()) {
			System.err.println("WARNING: Double HC found");
		}
		if (DIM<=31 && node.getPostCount() + node.getSubCount() > (1L<<DIM)) {
			System.err.println("WARNING: Over-populated node found: pc=" + node.getPostCount() + 
					"  sc=" + node.getSubCount());
		}
		//check space
		int baS = node.calcArraySizeTotalBits(node.getPostCount(), DIM);
		baS = Bits.calcArraySize(baS);
		if (baS < node.ba.length) {
			stats.nTooLarge++;
			if ((node.ba.length - baS)==2) {
				stats.nTooLarge2++;
			} else if ((node.ba.length - baS)==4) {
				stats.nTooLarge4++;
			} else {
				System.err.println("Array too large: " + node.ba.length + " - " + baS + " = " + 
						(node.ba.length - baS));
			}
		}
		return stats;
	}

	@Override
	public T put(long[] key, T value) {
        return operations.put(key, value);
    }

    void insertRoot(long[] key, T value) {
        root = operations.createNode(this, 0, DEPTH_64-1, 1, DIM);
        //calcPostfixes(valueSet, root, 0);
        long pos = posInArray(key, root.getPostLen());
        root.addPost(pos, key, value);
        increaseNrEntries();
    }

	@Override
	public boolean contains(long... key) {
		if (getRoot() == null) {
			return false;
		}
		return contains(key, 0, getRoot());
	}


	private boolean contains(long[] key, int currentDepth, Node<T> node) {
		if (node.getInfixLen() > 0) {
			long mask = ~((-1l)<<node.getInfixLen()); // e.g. (0-->0), (1-->1), (8-->127=0x01111111)
			int shiftMask = node.getPostLen()+1;
			//mask <<= shiftMask; //last bit is stored in bool-array
			mask = shiftMask==64 ? 0 : mask<<shiftMask;
			for (int i = 0; i < key.length; i++) {
				if (((key[i] ^ node.getInfix(i)) & mask) != 0) {
					//infix does not match
					return false;
				}
			}
			currentDepth += node.getInfixLen();
		}

		long pos = posInArray(key, node.getPostLen());

		//NI-node?
		if (node.isPostNI()) {
			NodeEntry<T> e = node.getChildNI(pos);
			if (e == null) {
				return false;
			} else if (e.node != null) {
				return contains(key, currentDepth + 1, e.node);
			}
			return node.postEquals(e.getKey(), key);
		}

		//check sub-node (more likely than postfix, because there can be more than one value)
		Node<T> sub = node.getSubNode(pos, DIM);
		if (sub != null) {
			return contains(key, currentDepth + 1, sub);
		}

		//check postfix
		int pob = node.getPostOffsetBits(pos, DIM);
		if (pob >= 0) {
			return node.postEqualsPOB(pob, pos, key);
		}

		return false;
	}

	@Override
	public T get(long... key) {
		if (getRoot() == null) {
			return null;
		}
		return get(key, 0, getRoot());
	}


	private T get(long[] key, int currentDepth, Node<T> node) {
		if (node.getInfixLen() > 0) {
			long mask = (1l<<node.getInfixLen()) - 1l; // e.g. (0-->0), (1-->1), (8-->127=0x01111111)
			int shiftMask = node.getPostLen()+1;
			//mask <<= shiftMask; //last bit is stored in bool-array
			mask = shiftMask==64 ? 0 : mask<<shiftMask;
			for (int i = 0; i < key.length; i++) {
				if (((key[i] ^ node.getInfix(i)) & mask) != 0) {
					//infix does not match
					return null;
				}
			}
			currentDepth += node.getInfixLen();
		}

		long pos = posInArray(key, node.getPostLen());

		//check sub-node (more likely than postfix, because there can be more than one value)
		Node<T> sub = node.getSubNode(pos, DIM);
		if (sub != null) {
			return get(key, currentDepth + 1, sub);
		}

		//check postfix
		int pob = node.getPostOffsetBits(pos, DIM);
		if (pob >= 0) {
			if (node.postEqualsPOB(pob, pos, key)) {
				return node.getPostValuePOB(pob, pos, DIM);
			}
		}

		return null;
	}

	/**
	 * A value-set is an object with n=DIM values.
	 * @param key
	 * @return true if the value was found
	 */
	@Override
	public T remove(long... key) {
        return operations.remove(key);
	}

	int getConflictingInfixBits(long[] key, long[] infix, Node<T> node) {
		if (node.getInfixLen() == 0) {
			return 0;
		}
		long mask = (1l<<node.getInfixLen()) - 1l; // e.g. (0-->0), (1-->1), (8-->127=0x01111111)
		int maskOffset = node.getPostLen()+1;
		mask = maskOffset==64 ? 0 : mask<< maskOffset; //last bit is stored in bool-array
		return getMaxConflictingBitsWithMask(key, infix, mask);
	}

	long posInArrayFromInfixes(Node<T> node, int infixInternalOffset) {
		//n=DIM,  i={0..n-1}
		// i = 0 :  |   0   |   1   |
		// i = 1 :  | 0 | 1 | 0 | 1 |
		// i = 2 :  |0|1|0|1|0|1|0|1|
		//len = 2^n

		long pos = 0;
		for (int i = 0; i < DIM; i++) {
			pos <<= 1;
//			if (node.getInfixBit(i, infixInternalOffset)) {
//				pos |= 1L;
//			}
			pos |= node.getInfixBit(i, infixInternalOffset);
		}
		return pos;
	}

	@Override
	public String toString() {
		return toStringPlain();
	}

	@Override
	public String toStringPlain() {
		StringBuilderLn sb = new StringBuilderLn();
		if (getRoot() != null) {
			toStringPlain(sb, 0, getRoot(), new long[DIM]);
		}
		return sb.toString();
	}

	private void toStringPlain(StringBuilderLn sb, int currentDepth, Node<T> node, long[] key) {
		//for a leaf node, the existence of a sub just indicates that the value exists.
		node.getInfix(key);
		currentDepth += node.getInfixLen();

		for (int i = 0; i < 1L << DIM; i++) {
			applyHcPos(i, node.getPostLen(), key);
			//inner node?
			Node<T> sub = node.getSubNode(i, DIM);
			if (sub != null) {
				toStringPlain(sb, currentDepth + 1, sub, key);
			}

			//post-fix?
			if (node.hasPostFix(i, DIM)) {
				node.getPost(i, key);
				sb.append(Bits.toBinary(key, DEPTH_64));
				sb.appendLn("  v=" + node.getPostValue(i, DIM));
			}
		}
	}


	@Override
	public String toStringTree() {
		StringBuilderLn sb = new StringBuilderLn();
		if (getRoot() != null) {
			toStringTree(sb, 0, getRoot(), new long[DIM], true);
		}
		return sb.toString();
	}

	private void toStringTree(StringBuilderLn sb, int currentDepth, Node<T> node, long[] key, 
			boolean printValue) {
		String ind = "*";
		for (int i = 0; i < currentDepth; i++) ind += "-";
		sb.append( ind + "il=" + node.getInfixLen() + " io=" + (node.getPostLen()+1) + 
				" sc=" + node.getSubCount() + " pc=" + node.getPostCount() + " inf=[");

		//for a leaf node, the existence of a sub just indicates that the value exists.
		node.getInfix(key);
		if (node.getInfixLen() > 0) {
			long[] inf = new long[DIM];
			node.getInfix(inf);
			sb.append(Bits.toBinary(inf, DEPTH_64));
			currentDepth += node.getInfixLen();
		}
		sb.appendLn("]");

		//To clean previous postfixes.
		for (int i = 0; i < 1L << DIM; i++) {
			applyHcPos(i, node.getPostLen(), key);
			Node<T> sub = node.getSubNode(i, DIM);
			if (sub != null) {
				sb.appendLn(ind + "# " + i + "  +");
				toStringTree(sb, currentDepth + 1, sub, key, printValue);
			}

			//post-fix?
			if (node.hasPostFix(i, DIM)) {
				T v = node.getPost(i, key);
				sb.append(ind + Bits.toBinary(key, DEPTH_64));
				if (printValue) {
					sb.append("  v=" + v);
				}
				sb.appendLn("");
			}
		}
	}


	@Override
	public PhIterator<T> queryExtent() {
		if (DIM < 10) {
			return new NDFullIterator<T>(getRoot(), DIM);
		} else {
			return new NDFullIterator2<T>(getRoot(), DIM);
		}
	}

	private static class NDFullIterator<T> implements PhIterator<T> {
		private final int DIM;
		private final Stack<Pos<T>> stack = new Stack<Pos<T>>();
		private static class Pos<T> {
			Pos(Node<T> node) {
				this.node = node;
				this.pos = -1;
			}
			Node<T> node;
			int pos;
		}
		private final long[] valTemplate;// = new long[DIM];
		private long[] nextKey = null;
		private T nextVal = null;

		public NDFullIterator(Node<T> root, final int DIM) {
			this.DIM = DIM;
			valTemplate = new long[DIM];
			if (root == null) {
				//empty index
				return;
			}
			stack.push(new Pos<T>(root));
			findNextElement();
		}

		private void findNextElement() {
			while (true) {
				Pos<T> p = stack.peek();
				while (p.pos+1 < (1L<<DIM)) {
					p.pos++;
					p.node.getInfix(valTemplate);
					Node<T> sub = p.node.getSubNode(p.pos, DIM); 
					if (sub != null) {
						applyHcPos(p.pos, p.node.getPostLen(), valTemplate);
						stack.push(new Pos<T>(sub));
						findNextElement();
						return;
					}
					int pob = p.node.getPostOffsetBits(p.pos, DIM);
					if (pob >= 0) {
						//get value
						long[] key = new long[DIM];
						System.arraycopy(valTemplate, 0, key, 0, DIM);
						applyHcPos(p.pos, p.node.getPostLen(), key);
						nextVal = p.node.getPostPOB(pob, p.pos, key);
						nextKey = key;
						return;
					}
				}
				stack.pop();
				if (stack.isEmpty()) {
					//finished
					nextKey = null;
					nextVal = null;
					return;
				}
			}
		}

		@Override
		public boolean hasNext() {
			return nextKey != null;
		}

		@Override
		public long[] nextKey() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			long[] res = nextKey;
			findNextElement();
			return res;
		}

		@Override
		public T nextValue() {
			T ret = nextVal;
			nextKey();
			return ret;
		}

		@Override
		public PhEntry<T> nextEntry() {
			PhEntry<T> ret = new PhEntry<>(nextKey, nextVal);
			nextKey();
			return ret;
		}

		@Override
		public T next() {
			return nextValue();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Not implemented yet.");
		}

	}


	private static class NDFullIterator2<T> implements PhIterator<T> {
		private final int DIM;
		private final Stack<NodeIteratorFull<T>> stack = new Stack<>();
		private final long[] valTemplate;
		private long[] nextKey = null;
		private T nextVal = null;

		public NDFullIterator2(Node<T> root, final int DIM) {
			this.DIM = DIM;
			valTemplate = new long[DIM];
			if (root == null) {
				//empty index
				return;
			}
			stack.push(new NodeIteratorFull<T>(root, DIM, valTemplate));
			findNextElement();
		}

		private void findNextElement() {
			while (true) {
				NodeIteratorFull<T> p = stack.peek();
				if (p.hasNext()) {
					long pos = p.getCurrentPos();

					if (p.isNextSub()) {
						applyHcPos(pos, p.node().getPostLen(), valTemplate);
						stack.push(
								new NodeIteratorFull<T>(p.getCurrentSubNode(), DIM, valTemplate));
						findNextElement();
					} else {
						nextVal = p.getCurrentPostVal();
						nextKey = p.getCurrentPostKey();
					}
					p.increment();
					return;
				}
				stack.pop();
				if (stack.isEmpty()) {
					//finished
					nextKey = null;
					nextVal = null;
					return;
				}
			}
		}

		@Override
		public boolean hasNext() {
			return nextKey != null;
		}

		@Override
		public long[] nextKey() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			long[] res = nextKey;
			findNextElement();
			return res;
		}

		@Override
		public T nextValue() {
			T ret = nextVal;
			nextKey();
			return ret;
		}

		@Override
		public PhEntry<T> nextEntry() {
			PhEntry<T> ret = new PhEntry<>(nextKey, nextVal);
			nextKey();
			return ret;
		}

		@Override
		public T next() {
			return nextValue();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Not implemented yet.");
		}
	}


	/**
	 * Performs a range query. The parameters are the min and max values.
	 * @param min
	 * @param max
	 * @return Result iterator.
	 */
	@Override
	public PhQuery<T> query(long[] min, long[] max) {
		if (min.length != DIM || max.length != DIM) {
			throw new IllegalArgumentException("Invalid number of arguments: " + min.length +  
					" / " + max.length + "  DIM=" + DIM);
		}
		//return new PhIteratorHighK<T>(getRoot(), min, max, DIM, DEPTH);
		PhQuery<T> q = new PhIteratorNoGC<>(this, DIM);
		q.reset(min, max);
		return q;
	}

	static final <T> boolean checkAndApplyInfix(Node<T> node, long[] valTemplate, 
			long[] rangeMin, long[] rangeMax) {
		//first check if node-prefix allows sub-node to contain any useful values
		int infixLen = node.getInfixLen();
		if (infixLen > 0) {
			int postLen = node.getPostLen();

			//assign infix
			int postHcInfixLen = postLen + 1 + infixLen;
			long maskClean = postHcInfixLen==64 ? //currentDepth == 0 && DEPTH == 64 
					0 : ((0xFFFFFFFFFFFFFFFFL<<postHcInfixLen));
			//first, clean trailing bits
			//Mask for comparing the tempVal with the ranges, except for bit that have not been
			//extracted yet.
			long compMask = (-1L)<<(postLen + 1);
			for (int dim = 0; dim < valTemplate.length; dim++) {
				//there would be no need to extract it each time.
				//--> For high k, infixLen is usually 0 (except root node), so the check below very 
				//rarely fails. Is an optimisation really useful?
				long in = node.getInfix(dim);
				valTemplate[dim] = (valTemplate[dim] & maskClean) | in;
				if (valTemplate[dim] > rangeMax[dim] || 
						valTemplate[dim] < (rangeMin[dim]&compMask)) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public int getDIM() {
		return DIM;
	}

	@Override
	public int getDEPTH() {
		return PhTree8.DEPTH_64;
	}

  /**
   * Performs a spherical range query with a maximum distance {@code maxDistance} from point
   * {@code center}.
   * @param center
   * @param maxDistance
   * @return Result iterator.
   */
  public PhQuery<T> query(long[] center, double maxDistance) {
    if (center.length != DIM || maxDistance < 0) {
      throw new IllegalArgumentException("Invalid arguments: " + center.length +  
          " / " + maxDistance + "  DIM=" + DIM);
    }
    PhQuery<T> q = new PhIteratorNoGC<T>(this, DIM);
    resetDistQuery(q, center, maxDistance);
    return q;
  }
  
  private PhQuery<T> resetDistQuery(PhQuery<T> query, long[] center, double maxDistance) {
    //TODO improve!
    long[] min = new long[DIM];
    long[] max = new long[DIM];
    for (int i = 0; i < DIM; i++) {
      min[i] = (long) (center[i] - maxDistance);
      max[i] = (long) (center[i] + maxDistance);
    }
    query.reset(min, max);
    return query;
  }

  /**
	 * Locate nearest neighbours for a given point in space.
	 * @param nMin number of values to be returned. More values may be returned with several have
	 * 				the same distance.
	 * @param v
	 * @return List of neighbours.
	 */
	@Override
	public ArrayList<long[]> nearestNeighbour(int nMin, long... v) {
    if (nMin > 0) {
      //nearestNeighbour(getRoot(), 0, v, nMin, ret);
      return nearestNeighbourBinarySearch(v, nMin, PhDistanceL.THIS);
    }
    return new ArrayList<long[]>();
	}

  //To correct for math errors
  //final double SAFETY = 1 + DIM*DIM * Double.MIN_NORMAL; 
  private static final double KNN_SAFETY = 1 + 0.000000001;

  /**
   * This approach applies binary search to queries.
   * It start with a query that covers the whole tree. Then whenever it finds an entry (the first)
   * it discards the query and starts a smaller one with half the distance to the search-point.
   * This effectifely reduces the volume by 2^k.
   * Once a query returns no result, it uses the previous query to traverse all results
   * and find the nearest result.
   * As an intermediate step, it may INCREASE the query size until a non-empty query appears.
   * Then it could decrease again, like a true binary search.
   * 
   * When looking von nMin > 1, one could search for queries with at least nMin results...
   * 
   * TODO find better starting point, e.g. start in node found by point search on v, and than use 
   * node-local neighbours to iterate further. --> Easier for 1NN.
   * 
   * 
   * @param val
   * @param nMin
   */
  private ArrayList<long[]> nearestNeighbourBinarySearch(long[] val, int nMin, PhDistance dist) {
    ArrayList<long[]> ret = new ArrayList<>();

    //special case with minDist = 0
    if (nMin == 1 && contains(val)) {
      ret.add(val);
      return ret;
    }
    final LAComparator comp = new LAComparator(val, dist);


    PhIterator<T> itEx = queryExtent();
    while (itEx.hasNext() && ret.size() < nMin) {
      long[] e = itEx.nextKey();
      ret.add(e);
    }
    //TODO optimise comparator, e.g. use special entries that store the 'distance'?
    Collections.sort(ret, comp);

    if (!itEx.hasNext()) {
      //tree has <- nMin entries, return all!
      return ret; 
    }

    //The minimum distance tells us that below that distance, there are not enough values.
    //It helps that there can only be one point with dist=0
    double minFailDist = 0;
    //The maximum known distance required to get nMax entries
    //use 'double' allow diagonals in 64-bit cube? max=sqrt(k)*Long.MAX_VALUE
    double maxReqDist = dist.dist(ret.get(nMin-1), val)*KNN_SAFETY;

    //TODO optimize:  bestDS >>> (64-numberOfLeadingZeros)  i.o.  Math.sqrt
    //TODO for know we assume that val+/- bestD lies inside the value range of Long....?!!?!?
    double currentDist = (maxReqDist+minFailDist)/2;

    PhQuery<T> itEx2 = new PhIteratorNoGC<>(this, DIM);
    ArrayList<long[]> candidates = new ArrayList<>();
    do {
      int result = getCandidates(currentDist, nMin, candidates, val, comp, itEx2, dist);

      if (result == 0) {
        //done!
        return candidates;
      }
      if (result < 0) {
        //so this was too wide
        maxReqDist = currentDist;
      } else {
        //too few results, we need a bigger range
        //TODO use min of previous round instead...
        minFailDist = currentDist;
      }

      double newDist = (maxReqDist+minFailDist) / 2;

      double delta = Math.abs(newDist-currentDist);//(maxReqDist-minFailDist);///maxReqDist;
      if (delta < (Math.sqrt(DIM)-1)*KNN_SAFETY) {
        //Get and sort ALL points in range...
        //TODO do not use getCandidates but implement directly
        getCandidates(maxReqDist*KNN_SAFETY, 10*1000*1000, candidates, val, comp, itEx2, dist);
        return candidates;
      }

      currentDist = newDist;
    } while (true); //currentDist > 1);  //TODO > 0?

    //throw new IllegalStateException();
    //return candidates;

    //TODO consolidate?
  }

  private final int getCandidates(double maxDist, int nMin, 
      ArrayList<long[]> cand, long[] val, Comparator<long[]> comp, PhQuery<T> itEx,
      PhDistance dist) {
    //What are we doing here?
    //There are several cases to consider:
    //1) maxDistSQ is too small, we get < nMin values
    //2) If there are enough entries, we have to consider the following
    //2a) Prevent going further down in case we have exactly nMin entries
    //2b) Prevent going further down in case all entries beyond nMin have the same distance 
    //    than nMin. (no more than nMax different distances)
    //2c) Optimisation: Prevent going further down if we have more than nMin DIFFERENT entries, 
    //    but we exhausted them all, so we can still easily return a final result.
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
    final int N_MIN_MULTIPLIER = 5;
    final int ENTRY_TO_SIZE_RATIO = 2;
    final int CONSOLIDATION_INTERVAL = 10;
    cand.clear();
    int nChecked = 0;
    resetDistQuery(itEx, val, maxDist);
    double maxDistSQ = maxDist * maxDist;// * KNN_SAFETY;

    //a)
    while (itEx.hasNext() && cand.size() < nMin) {
      long[] e = itEx.nextKey();
      nChecked++;
      //TODO this test can be removed once we have a proper iterator.
      if (maxDistSQ >= dist.distEst(e, val)) {
        cand.add(e);
      }
    }

    //b)
    //TODO optimise comparator, e.g. use special entries that store the 'distance'?
    //TODO consolidate only after size-check?
    Collections.sort(cand, comp);

    if (cand.size() < nMin) {
      //too small
      return 1;
    }
    if (!itEx.hasNext()) {
      //perfect fit!
      return 0;
    }

    //c)
    maxDistSQ = dist.distEst(cand.get(nMin-1), val);

    //TODO restart with new iterator based on maxDistSQ = maxSQ? 
    //TODO --> allow query with squared distance?

    //d)
    while (itEx.hasNext()) {
      long[] e = itEx.nextKey();
      nChecked++;
      if (maxDistSQ+EPS >= dist.distEst(e, val)) {
        cand.add(e);
      }
      if (cand.size() % CONSOLIDATION_INTERVAL == 0) {
        maxDistSQ = consolidate(cand, nMin, EPS, val, maxDistSQ, comp, dist); 
      }
      if (nChecked > nMin*N_MIN_MULTIPLIER && nChecked > ENTRY_TO_SIZE_RATIO * cand.size()) {
        //TODO is this necessary?
        //f)
        consolidate(cand, nMin, EPS, val, maxDistSQ, comp, dist);
        return -1;
      }
    }
    //e)
    consolidate(cand, nMin, EPS, val, maxDistSQ, comp, dist);
    return 0;
  }

  private static double consolidate(ArrayList<long[]> cand, int nMin, double EPS, long[] val,
      double maxSQ, Comparator<long[]> comp, PhDistance dist) {
    Collections.sort(cand, comp);
    double maxSQnew = dist.distEst(cand.get(nMin-1), val);
    if (maxSQnew < maxSQ+EPS) { //TODO epsilon?
      maxSQ = maxSQnew;
      for (int i2 = nMin; i2 < cand.size(); i2++) {
        //purge 
        if (dist.distEst(cand.get(i2), val) + EPS > maxSQ) {
          while (cand.size() > i2) {
            cand.remove(cand.size()-1);  
          }
          break;
        }
      }
    }
    return maxSQ;
  }

  private static final class LAComparator implements Comparator<long[]> {
    private final long[] val;

    private final PhDistance dist;
    
    public LAComparator(long[] val, PhDistance dist) {
      this.val = val;
      this.dist = dist;
    }

    @Override
    public int compare(long[] o1, long[] o2) {
      double d = distSQ(o1) - distSQ(o2);
      //Do not return d directly because it may exceed the limits of Integer.
      return d > 0 ? 1 : (d < 0 ? -1 : 0);
    }

    private final double distSQ(long[] v) {
      return dist.distEst(val, v);
    }
  }


//  private static final double squareDist(long[] v1, long[] v2) {
//    double r = 0;
//    for (int i = 0; i < v1.length; i++) {
//      double x = (double)v1[i]-(double)v2[i]; //cast to double to handle overflows... 
//      r += x*x;
//    }
//    return r;
//  }
//
//  private static final double dist(long[] v1, long[] v2) {
//    return Math.sqrt(squareDist(v1, v2));
//  }

  /**
   * How to do nearest neighbour queries:
   * 
   * 1) Search for value, stay in node if value not found.
   *    Here we already know that the closest value can not be further away than any value in the
   *    local node, i.e. with an editDistance=DIM on the level of the node's HC.
   *    But it depends on the depth of the tree (# of parent nodes) whether this is helpful.
   *    For root noodes, this is not helpful at all. 
   * 2) Search local node, all postfixes/sub-nodes with editDistance=1.
   *    In each sub-node, depending on the geometry, we can exclude some quadrants.
   *    I.e. with editDistance=1, only half of the quadrants need to be searched, unless the closest
   *    quadrant is empty.
   *    With higher editDistance of the subnode, it gets more complicated.
   *    With luck, we can already exclude finding one with editDistance > 1. 
   *    From the result, take the closes value.
   * 3) Perform range query on whole tree (proceed through parent nodes iteratively???) on a 
   *    hyper rectangle with the currently known minimum distance.
   *    Check all results and find minimum.
   *    Problem: the complexity of the last step is unbound...
   *     
   * @param node
   * @param currentDepth
   * @param val
   * @param nMin
   * @param ret
   */
  private void nearestNeighbour(Node<T> node, int currentDepth, long[] val, int nMin,
      ArrayList<PhEntry<T>> ret) {
    if (node.getInfixLen() > 0) {
      long mask = (1l<<(long)node.getInfixLen()) - 1l;//eg. (0-->0), (1-->1), (8-->127=0x01111111)
      int shiftMask = node.getPostLen()+1;
      //mask <<= shiftMask; //last bit is stored in bool-array
      mask = shiftMask==64 ? 0 : mask<<shiftMask;
      for (int i = 0; i < val.length; i++) {
        if (((val[i] ^ node.getInfix(i)) & mask) != 0) {
          //infix does not match
          //--> there is no direct match, so lets find the nearest neighbour, which
          //must be on this node.
          break;
        }
      }
      currentDepth += node.getInfixLen();
    }

    long pos = posInArray(val, node.getPostLen());
    boolean isPostHC = node.isPostHC();
    boolean isSubHC = node.isSubHC();

    //check subnode (more likely than postfix, because there can be more than one value)
    Node<T> sub = node.getSubNode(pos, DIM);
    if (sub != null) {
      nearestNeighbour(sub, currentDepth+1, val, nMin, ret);
    } else {
      //check postfix
      int pob = node.getPostOffsetBits(pos, DIM);
      if (pob >= 0) {
        //If we have a match, we ignore it (we ignore perfect matches).
        //Otherwise we return the value, because it is a close neighbour.
        long[] v = new long[DIM];
        System.arraycopy(val, 0, v, 0, DIM);
        //TODOnode.getPost(pos, posPostLHC, v, postLen, isPostHC, bufOffsOfPosts);
        applyHcPos(pos, node.getPostLen(), v);
        T value = node.getPost(pos, v);
        ret.add(new PhEntry<T>(v, value));
      }
    }

    if (ret.size() >= nMin) {
      return;
    }

    //TODO now start permutations...
    if (isPostHC) {
      //TODO search with permutation
    } else {
      //TODO traverse all and calculate distance
    }

    if (isSubHC) {
      //TODO search with permutation
    } else {
      //TODO traverse all and calculate distance
    }
  }

  /**
	 * Best HC incrementer ever. 
	 * @param v
	 * @param min
	 * @param max
	 * @return next valid value or min.
	 */
	static long inc(long v, long min, long max) {
		//first, fill all 'invalid' bits with '1' (bits that can have only one value).
		long r = v | (~max);
		//increment. The '1's in the invalid bits will cause bitwise overflow to the next valid bit.
		r++;
		//remove invalid bits.
		return (r & max) | min;

		//return -1 if we exceed 'max' and cause an overflow or return the original value. The
		//latter can happen if there is only one possible value (all filter bits are set).
		//The <= is also owed to the bug tested in testBugDecrease()
		//return (r <= v) ? -1 : r;
	}

	@Override
	public List<long[]> nearestNeighbour(int nMin, PhDistance dist,
			PhDimFilter dims, long... key) {
    if (nMin > 0) {
      //nearestNeighbour(getRoot(), 0, v, nMin, ret);
      return nearestNeighbourBinarySearch(key, nMin, dist);
    }
    return new ArrayList<long[]>();
	}

	@Override
	public T update(long[] oldKey, long[] newKey) {
	  return operations.update(oldKey, newKey);
	}
	

	/**
	 * Remove all entries from the tree.
	 */
	@Override
	public void clear() {
		root = null;
		nEntries.set(0);
		nNodes.set(0);
	}

	void adjustCounts(int deletedPosts, int deletedNodes) {
		nEntries.addAndGet(-deletedPosts);
		nNodes.addAndGet(-deletedNodes);
	}
}

