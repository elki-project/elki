/*
 * Copyright 2011-2015 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.pht.v8;

import static ch.ethz.globis.pht.PhTreeHelper.applyHcPos;

import java.util.NoSuchElementException;

import org.zoodb.index.critbit.CritBit64COW.CBIterator;
import org.zoodb.index.critbit.CritBit64COW.Entry;

/**
 * An iterator for full iteration of the the PH-tree. It does not use
 * range filters.
 * 
 * @author Tilmann
 *
 * @param <T>
 */
class NodeIteratorFull<T> {
	private final int DIM;
	private final int postLen;
	private long next = -1;
	private long nextPost = -1;
	private long nextSub = -1;
	private long[] nextPostKey;
	private T nextPostVal;
	private Node<T> nextSubNode;
	private final Node<T> node;
	private int currentOffsetPostKey;
	private int currentOffsetPostVal;
	private int currentOffsetSub;
	private CBIterator<PhTree8.NodeEntry<T>> niIterator;
	private final int nMaxPost;
	private final int nMaxSub;
	private int postsFound = 0;
	private int posSubLHC = -1; //position in sub-node LHC array
	private final int postEntryLen;
	private final long[] valTemplate;
	private boolean isPostFinished;
	private boolean isSubFinished;

	/**
	 * 
	 * @param node The root node
	 * @param DIM
	 * @param valTemplate An empty buffer (long[DIM]) for temporary values.
	 *        A null indicates that no values should be extracted.
	 */
	public NodeIteratorFull(Node<T> node, int DIM, long[] valTemplate) {
		this.DIM = DIM;
		this.node = node;
		this.valTemplate = valTemplate;
		this.postLen = node.getPostLen();
		nMaxPost = node.getPostCount();
		nMaxSub = node.getSubCount();
		isPostFinished = (nMaxPost <= 0);
		isSubFinished = (nMaxSub <= 0);
		//Position of the current entry
		currentOffsetSub = node.getBitPos_SubNodeIndex(DIM);
		currentOffsetSub -= (node.isSubHC()) ? 0 : Node.SIK_WIDTH(DIM);
		if (node.isPostNI()) {
			niIterator = node.ind().iterator();
			//not needed
			postEntryLen = -1;
		} else {
			currentOffsetPostKey = node.getBitPos_PostIndex(DIM);
			// -set key offset to position before first element
			// -set value offset to first element
			if (node.isPostHC()) {
				//length of post-fix WITHOUT key
				postEntryLen = DIM*postLen;
				currentOffsetPostVal = currentOffsetPostKey + (1<<DIM)*Node.PINN_HC_WIDTH;  
				currentOffsetPostKey -= Node.PINN_HC_WIDTH;
			} else {
				//length of post-fix WITH key
				postEntryLen = Node.PIK_WIDTH(DIM)+DIM*postLen;
				currentOffsetPostVal = currentOffsetPostKey + Node.PIK_WIDTH(DIM);  
				currentOffsetPostKey -= postEntryLen;
			}
		}

		//get infix
		if (valTemplate != null) {
			node.getInfix(valTemplate);
		}

		next = getNext();
	}

	boolean hasNext() {
		return next != -1;
	}

	void increment() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		next = getNext();
	}

	long getCurrentPos() {
		return next;
	}

	/**
	 * Return whether the next value returned by next() is a sub-node or not.
	 * 
	 * @return True if the current value (returned by next()) is a sub-node, 
	 * otherwise false
	 */
	boolean isNextSub() {
		return node.isPostNI() ? (nextSubNode != null) : (next == nextSub);
	}

	private void readValue(long pos, int offsPostKey) {
		if (valTemplate != null) {
			long[] key = new long[DIM];
			System.arraycopy(valTemplate, 0, key, 0, DIM);
			applyHcPos(pos, postLen, key);
			nextPostVal = node.getPostPOB(offsPostKey, pos, key);
			nextPostKey = key;
		}
		//Don't set to 'null' here, that interferes with parallel iteration over post/sub 
		//nextSubNode = null;
	}

	private void readValue(long pos, PhTree8.NodeEntry<T> e) {
		//(valTemplate== null) always matches, special case for iterator in delete()
		if (valTemplate != null) {
			long[] buf = new long[DIM];
			System.arraycopy(valTemplate, 0, buf, 0, valTemplate.length);
			applyHcPos(pos, postLen, buf);

			//extract postfix
			final long mask = (~0L)<<postLen;
			long[] eKey = e.getKey();
			for (int i = 0; i < buf.length; i++) {
				buf[i] &= mask;  
				buf[i] |= eKey[i];
			}
			nextPostKey = e.getKey();
		}
		nextPostVal = e.getValue();
		nextSubNode = null;
	}


	private long getNext() {
		if (node.isPostNI()) {
			niFindNext();
			return next;
		}

		//Search for next entry if there are more entries and if current
		//entry has already been returned (or is -1).
		// (nextPost == next) is true when the previously returned entry (=next) was a postfix.
		if (!isPostFinished && nextPost == next) {
			if (node.isPostHC()) {
				//while loop until 1 is found.
				long currentPos = next; 
				nextPost = -1;
				while (!isPostFinished) {
					if (currentPos >= 0) {
						currentPos++;  //pos w/o bit-offset
					} else {
						currentPos = 0; //initial value
					}
					if (currentPos >= (1<<DIM)) {
						isPostFinished = true;
						break;
					}
					currentOffsetPostKey += Node.PINN_HC_WIDTH;  //pos with bit-offset
					if (Bits.getBit(node.ba, currentOffsetPostKey)) {
						//read post-fix
						int offs = (int) (currentOffsetPostVal+currentPos*postEntryLen);
						readValue(currentPos, offs);
						nextPost = currentPos;
						break;
					}
				}
			} else {
				nextPost = -1;
				if (postsFound >= nMaxPost) {
					isPostFinished = true;
				} else {
					currentOffsetPostKey += postEntryLen;
					long currentPos = Bits.readArray(node.ba, currentOffsetPostKey, Node.PIK_WIDTH(DIM));
					//read post-fix
					readValue(currentPos, currentOffsetPostKey + Node.PIK_WIDTH(DIM));
					nextPost = currentPos;
					postsFound++;
				}
			}
		}
		if (!isSubFinished && nextSub == next) {
			if (node.isSubHC()) {
				int currentPos = (int) next;  //We use (int) because arrays are always (int).
				int maxPos = 1<<DIM; 
				nextSub = -1;
				while (!isSubFinished) {
					currentPos++;
					if (currentPos >= maxPos) {
						isSubFinished = true;
						break;
					}
					if (node.subNRef(currentPos) != null) {
						nextSub = currentPos;
						nextSubNode = node.subNRef(currentPos);
						break;
					}
				}
			} else {
				nextSub = -1;
				if (posSubLHC + 1 >= nMaxSub) {
					isSubFinished = true;
				} else {
					currentOffsetSub += Node.SIK_WIDTH(DIM);
					long currentPos = Bits.readArray(node.ba, currentOffsetSub, Node.SIK_WIDTH(DIM));
					posSubLHC++;
					nextSub = currentPos;
					nextSubNode = node.subNRef(posSubLHC);
				}
			}
		}

		if (isPostFinished && isSubFinished) {
			return -1;
		} 
		if (!isPostFinished && !isSubFinished) {
			if (nextSub < nextPost) {
				return nextSub;
			} else {
				return nextPost;
			}
		}
		if (isPostFinished) {
			return nextSub;
		} else {
			return nextPost;
		}
	}

	private void niFindNext() {
		if (niIterator.hasNext()) {
			Entry<PhTree8.NodeEntry<T>> e = niIterator.nextEntry();
			long pos = e.key();
			next = pos;
			nextSubNode = e.value().node;
			if (nextSubNode == null) {
				readValue(e.key(), e.value());
			} else {
				nextPostVal = null;
				nextPostKey = null;
			}
		} else {
			next = -1;
		}
		return;
	}


	/**
	 * Return the count of currently found sub-nodes minus one. For LHC, this is equal to 
	 * the position in the sub-node array.
	 * @return subCount - 1
	 */
	public int getPosSubLHC() {
		return posSubLHC;
	}

	/**
	 * Return the value at the current position of the POST-ITERATOR. This may be a higher
	 * value than the current pos in case current pos indicates a sub-ref.
	 * @return the current value at the current post-position.
	 */
	public long[] getCurrentPostKey() {
		return nextPostKey;
	}

	/**
	 * Return the value at the current position of the POST-ITERATOR. This may be a higher
	 * value than the current pos in case current pos indicates a sub-ref.
	 * @return the current value at the current post-position.
	 */
	public T getCurrentPostVal() {
		return nextPostVal;
	}

	public Node<T> getCurrentSubNode() {
		return nextSubNode;
	}

	public Node<T> node() {
		return node;
	}
}