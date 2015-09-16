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

import org.zoodb.index.critbit.CritBit64COW.Entry;
import org.zoodb.index.critbit.CritBit64COW.QueryIteratorMask;

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
public class NodeIteratorNoGC<T> {
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
	private QueryIteratorMask<NodeEntry<T>> niIterator;
	private int nMaxPost;
	private int nMaxSub;
	private int nPostsFound = 0;
	private int posSubLHC = -1; //position in sub-node LHC array
	private int postEntryLen;
	private final long[] valTemplate;
	private long maskLower;
	private long maskUpper;
	private long[] rangeMin;
	private long[] rangeMax;
	private boolean usePostHcIncrementer;
	private boolean useSubHcIncrementer;
	private boolean isPostFinished;
	private boolean isSubFinished;
	private final PhEntry<T> nextPost1;
	private final PhEntry<T> nextPost2;
	private boolean isNextPost1free;


	/**
	 * 
	 * @param node
	 * @param DIM
	 * @param valTemplate A null indicates that no values are to be extracted.
	 * @param lower The minimum HC-Pos that a value should have.
	 * @param upper
	 * @param minValue The minimum value that any found value should have. If the found value is
	 *  lower, the search continues.
	 * @param maxValue
	 */
	public NodeIteratorNoGC(int DIM, long[] valTemplate) {
		this.DIM = DIM;
		this.valTemplate = valTemplate;
		this.nextPost1 = new PhEntry<T>(new long[DIM], null);
		this.nextPost2 = new PhEntry<T>(new long[DIM], null);
	}
	
	private void reinit(Node<T> node, long[] rangeMin, long[] rangeMax, long lower, long upper) {
		this.rangeMin = rangeMin;
		this.rangeMax = rangeMax;
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
	
		this.node = node;
		this.isPostHC = node.isPostHC();
		this.isPostNI = node.isPostNI();
		this.isSubHC = node.isSubHC();
		this.postLen = node.getPostLen();
		nMaxPost = node.getPostCount();
		nMaxSub = node.getSubCount();
		isPostFinished = (nMaxPost <= 0);
		isSubFinished = (nMaxSub <= 0);
		this.maskLower = lower;
		this.maskUpper = upper;
		//Position of the current entry
		currentOffsetSub = node.getBitPos_SubNodeIndex(DIM);
		if (isPostNI) {
			postEntryLen = -1; //not used
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


		useSubHcIncrementer = false;
		usePostHcIncrementer = false;

		if (DIM > 3) {
			//LHC, NI, ...
			long maxHcAddr = ~((-1L)<<DIM);
			int nSetFilterBits = Long.bitCount(maskLower | ((~maskUpper) & maxHcAddr));
			//nPossibleMatch = (2^k-x)
			long nPossibleMatch = 1L << (DIM - nSetFilterBits);
			if (isPostNI) {
				int nChild = node.ind().size();
				int logNChild = Long.SIZE - Long.numberOfLeadingZeros(nChild);
				//the following will overflow for k=60
				boolean useHcIncrementer = (nChild > nPossibleMatch*(double)logNChild*2);
				//DIM < 60 as safeguard against overflow of (nPossibleMatch*logNChild)
				if (useHcIncrementer && PhTree8.HCI_ENABLED && DIM < 50) {
					niIterator = null;
				} else {
					niIterator = node.ind().queryWithMask(maskLower, maskUpper);
				}
			} else if (PhTree8.HCI_ENABLED){
				if (isPostHC) {
					//nPossibleMatch < 2^k?
					usePostHcIncrementer = nPossibleMatch < maxHcAddr;
				} else {
					int logNPost = Long.SIZE - Long.numberOfLeadingZeros(nMaxPost) + 1;
					usePostHcIncrementer = (nMaxPost > nPossibleMatch*(double)logNPost); 
				}
				if (isSubHC) {
					useSubHcIncrementer = nPossibleMatch < maxHcAddr;
				} else {
					int logNSub = Long.SIZE - Long.numberOfLeadingZeros(nMaxSub) + 1;
					useSubHcIncrementer = (nMaxSub > nPossibleMatch*(double)logNSub); 
				}
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

		if (!node.getPostPOB(offsPostKey, pos, result, rangeMin, rangeMax)) {
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
			if (eKey[i] < rangeMin[i] || eKey[i] > rangeMax[i]) {
				return false;
			}
		}
		System.arraycopy(eKey, 0, result.getKey(), 0, DIM);
		result.setValue(e.getValue());
		nextSubNode = null;
		isNextPost1free = !isNextPost1free;
		return true;
	}

	private long getNextPostHCI(long currentPos, PhEntry<T> result) {
		//Ideally we would switch between b-serch-HCI and incr-search depending on the expected
		//distance to the next value.
		do {
			if (currentPos == -1) {
				//starting position;
				currentPos = maskLower;
			} else {
				currentPos = PhTree8.inc(currentPos, maskLower, maskUpper);
				if (currentPos <= maskLower) {
					isPostFinished = true;
					return -1;
				}
			}
			if (!isPostNI) {
				int pob = node.getPostOffsetBits(currentPos, DIM);
				if (pob >= 0) {
					if (!readValue(currentPos, pob, result)) {
						continue;
					}
					return currentPos;
				}
			}
		} while (true);//currentPos >= 0);
	}

	private long getNextSubHCI(long currentPos) {
		do {
			if (currentPos == -1) {
				//starting position;
				currentPos = maskLower;
			} else {
				currentPos = PhTree8.inc(currentPos, maskLower, maskUpper);
				if (currentPos <= maskLower) {
					isSubFinished = true;
					return -1;
				}
			}
			if (isSubHC) {
				if (node.subNRef((int)currentPos) == null) {
					//this can happen because above method returns negative values only for LHC.
					continue; //not found --> continue
				}
				posSubLHC = (int) currentPos;
				nextSubNode = node.subNRef((int) currentPos);
				//found --> abort
				return currentPos;
			} else {
				int subOffsBits = currentOffsetSub;//node.getBitPos_SubNodeIndex(DIM);
				int subNodePos = Bits.binarySearch(node.ba, subOffsBits, nMaxSub, (int)currentPos, Node.SIK_WIDTH(DIM), 0);
				if (subNodePos >= 0) {
					posSubLHC = subNodePos;
					nextSubNode = node.subNRef(subNodePos);
					//found --> abort
					return currentPos;
				}
			}
		} while (true);//currentPos >= 0);
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
			if (usePostHcIncrementer) {
				nextPost = getNextPostHCI(nextPost, result);
			} else if (isPostHC) {
				getNextPostAHC(result);
			} else {
				getNextPostLHC(result);
			}
		}
		if (!isSubFinished && nextSub == next) {
			if (useSubHcIncrementer) {
				nextSub = getNextSubHCI(nextSub);
			} else if (isSubHC) {
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
			if (currentPos >= 0) {
				currentPos++;  //pos w/o bit-offset
			} else {
				currentPos = maskLower; //initial value
				currentOffsetPostKey += maskLower*Node.PINN_HC_WIDTH;  //pos with bit-offset
			}
			if (currentPos >= (1<<DIM)) {
				isPostFinished = true;
				break;
			}
			boolean bit = Bits.getBit(node.ba, currentOffsetPostKey);
			currentOffsetPostKey += Node.PINN_HC_WIDTH;  //pos with bit-offset
			if (bit) {
				//check HC-pos
				if (!checkHcPos(currentPos)) {
					if (currentPos > maskUpper) {
						isPostFinished = true;
						break;
					}
					continue;
				}
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
			//check HC-pos
			if (!checkHcPos(currentPos)) {
				if (currentPos > maskUpper) {
					isPostFinished = true;
					break;
				}
				continue;
			}
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
		int maxPos = 1<<DIM; 
		nextSub = -1;
		while (!isSubFinished) {
			if (currentPos < 0) {
				currentPos = (int) maskLower;
			} else {
				currentPos++;
			}
			if (currentPos >= maxPos) {
				isSubFinished = true;
				break;
			}
			if (node.subNRef(currentPos) != null) {
				//check HC-pos
				if (!checkHcPos(currentPos)) {
					if (currentPos > maskUpper) {
						isSubFinished = true;
						break;
					}
					continue;
				}
				nextSub = currentPos;
				nextSubNode = node.subNRef(currentPos);
				break;
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
			//check HC-pos
			if (!checkHcPos(currentPos)) {
				if (currentPos > maskUpper) {
					isSubFinished = true;
					break;
				}
				continue;
			}
			nextSub = currentPos;
			nextSubNode = node.subNRef(posSubLHC);
			break;
		}
	}

	private void niFindNext(PhEntry<T> result) {
		//iterator?
		if (niIterator != null) {
			while (niIterator.hasNext()) {
				Entry<NodeEntry<T>> e = niIterator.nextEntry();
				next = e.key();
				nextSubNode = e.value().node;
				if (nextSubNode == null) {
					if (!readValue(e.key(), e.value(), result)) {
						continue;
					}
				}
				return;
			}

			next = -1;
			return;
		}


		//HCI
		//repeat until we found a value inside the given range
		long currentPos = next; 
		do {
			if (currentPos != -1 && currentPos >= maskUpper) {
				break;
			}

			if (currentPos == -1) {
				//starting position;
				currentPos = maskLower;
			} else {
				currentPos = PhTree8.inc(currentPos, maskLower, maskUpper);
				if (currentPos <= maskLower) {
					break;
				}
			}

			NodeEntry<T> e = node.niGet(currentPos);
			if (e == null) {
				continue;
			}

			next = currentPos;

			//sub-node?
			nextSubNode = e.node;
			if (e.node != null) {
				return;
			}

			//read and check post-fix
			if (readValue(currentPos, e, result)) {
				return;
			}
		} while (true);
		next = -1;
	}


	private boolean checkHcPos(long pos) {
		return ((pos | maskLower) & maskUpper) == pos;
	}

	public Node<T> getCurrentSubNode() {
		return nextSubNode;
	}

	public Node<T> node() {
		return node;
	}

	void init(long[] rangeMin, long[] rangeMax, long[] valTemplate, Node<T> node) {
		//create limits for the local node. there is a lower and an upper limit. Each limit
		//consists of a series of DIM bit, one for each dimension.
		//For the lower limit, a '1' indicates that the 'lower' half of this dimension does 
		//not need to be queried.
		//For the upper limit, a '0' indicates that the 'higher' half does not need to be 
		//queried.
		//
		//              ||  lowerLimit=0 || lowerLimit=1 || upperLimit = 0 || upperLimit = 1
		// =============||===================================================================
		// query lower  ||     YES             NO
		// ============ || ==================================================================
		// query higher ||                                     NO               YES
		//
		long maskHcBit = 1L << node.getPostLen();
		long maskVT = (-1L) << node.getPostLen();
		long lowerLimit = 0;
		long upperLimit = 0;
		//to prevent problems with signed long when using 64 bit
		if (maskHcBit >= 0) { //i.e. postLen < 63
			for (int i = 0; i < valTemplate.length; i++) {
				lowerLimit <<= 1;
				upperLimit <<= 1;
				long nodeBisection = (valTemplate[i] | maskHcBit) & maskVT; 
				if (rangeMin[i] >= nodeBisection) {
					//==> set to 1 if lower value should not be queried 
					lowerLimit |= 1L;
				}
				if (rangeMax[i] >= nodeBisection) {
					//Leave 0 if higher value should not be queried.
					upperLimit |= 1L;
				}
			}
		} else {
			//currentDepth==0

			//special treatment for signed longs
			//The problem (difference) here is that a '1' at the leading bit does indicate a
			//LOWER value, opposed to indicating a HIGHER value as in the remaining 63 bits.
			//The hypercube assumes that a leading '0' indicates a lower value.
			//Solution: We leave HC as it is.

			for (int i = 0; i < valTemplate.length; i++) {
				lowerLimit <<= 1;
				upperLimit <<= 1;
				if (rangeMin[i] < 0) {
					//If minimum is positive, we don't need the search negative values 
					//==> set upperLimit to 0, prevent searching values starting with '1'.
					upperLimit |= 1L;
				}
				if (rangeMax[i] < 0) {
					//Leave 0 if higher value should not be queried
					//If maximum is negative, we do not need to search positive values 
					//(starting with '0').
					//--> lowerLimit = '1'
					lowerLimit |= 1L;
				}
			}
		}
		reinit(node, rangeMin, rangeMax, lowerLimit, upperLimit);
	}

}
