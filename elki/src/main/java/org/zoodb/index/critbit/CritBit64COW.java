/*
 * Copyright 2009-2015 Tilmann Zaeschke. All rights reserved.
 * 
 * The author can be contacted via email: zoodb@gmx.de
 * https://github.com/tzaeschke/critbit
 * 
 * This file is part of ZooDB / Critbit.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.zoodb.index.critbit;

/**
 * This version of CritBit uses Copy-On-Write to create new versions of the tree
 * following each write operation.
 *
 */
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A concurrency enabled version of the CritBit64 tree. 
 * The implementation uses copy-on-write concurrency, therefore locking occurs only during updates,
 * read-access is never locked.
 * 
 * Currently write/update access is limited to one thread at a time.
 * Read access guarantees full snapshot consistency for all read access including iterators.
 * 
 * Version 1.3.2
 * - Improved mask checking in QueryWithMask
 * 
 * Version 1.3.1
 * - Fixed issue #3 where iterators won't work with 'null' as values.
 * 
 * Version 1.0: initial implementation
 * 
 * @author bvancea
 *
 * @param <V>
 */
public class CritBit64COW<V> implements Iterable<V> {

    private final int DEPTH = 64;
    private final Lock writeLock = new ReentrantLock();

    private AtomicInfo<V> info = new AtomicInfo<>();

    private static class Node<V> {
        //TODO replace posDiff with posMask
        //     --> Possibly easier to calculate (non-log?)
        //     --> Similarly powerful....
        //TODO merge loPost & loVal!
        V loVal;
        V hiVal;
        Node<V> lo;
        Node<V> hi;
        //the following contain either the actual key or the prefix for sub-nodes
        long loPost;
        long hiPost;
        byte posDiff;

        Node(long loPost, V loVal, long hiPost, V hiVal, int posDiff) {
            this.loPost = loPost;
            this.loVal = loVal;
            this.hiPost = hiPost;
            this.hiVal = hiVal;
            this.posDiff = (byte) posDiff;
        }

        Node(Node<V> original) {
            this.loVal = original.loVal;
            this.hiVal = original.hiVal;
            this.lo = original.lo;
            this.hi = original.hi;
            this.loPost = original.loPost;
            this.hiPost = original.hiPost;
            this.posDiff = original.posDiff;
        }
    }

    private static class AtomicInfo<V> {
        private Node<V> root;
        //the following contains either the actual key or the prefix for the sub-node
        private long rootKey;
        private V rootVal;
        private int size;

        public AtomicInfo<V> copy() {
            AtomicInfo<V> c = new AtomicInfo<>();
            c.root = this.root;
            c.rootKey = this.rootKey;
            c.rootVal = this.rootVal;
            c.size = this.size;
            return c;
        }
    }

    private CritBit64COW() {
        //private
    }

    /**
     * Create a 1D crit-bit tree with 64 bit key length.
     * @return a 1D crit-bit tree
     */
    public static <V> CritBit64COW<V> create() {
        return new CritBit64COW<V>();
    }

    public CritBit64COW<V> copy() {
        CritBit64COW<V> critBitCopy = create();
        critBitCopy.info = this.info.copy();
        return critBitCopy;
    }

    /**
     * Add a key value pair to the tree or replace the value if the key already exists.
     * @param key
     * @param val
     * @return The previous value or {@code null} if there was no previous value
     */
    public V put(long key, V val) {
        try {
            writeLock.lock();
            AtomicInfo<V> newInfo = this.info.copy();
            V ret = doPut(key, val, newInfo);
            this.info = newInfo;
            return ret;
        } finally {
            writeLock.unlock();;
        }
    }

    private V doPut(long key, V val, AtomicInfo<V> newInfo) {
        AtomicInfo<V> currentInfo = this.info;
        int size = currentInfo.size;
        if (size == 0) {
        	newInfo.rootKey = key;
        	newInfo.rootVal = val;
        	newInfo.size++;
        	return null;
        }
        if (size == 1) {
        	Node<V> n2 = createNode(key, val, info.rootKey, info.rootVal);
        	if (n2 == null) {
        		V prev = currentInfo.rootVal;
        		newInfo.rootVal = val;
        		return prev;
        	}
        	newInfo.root = n2;
        	newInfo.rootKey = extractPrefix(key, n2.posDiff - 1);
        	newInfo.rootVal = null;
        	newInfo.size++;
        	return null;
        }
        Node<V> n = newInfo.root;
        int parentPosDiff = -1;
        long prefix = newInfo.rootKey;
        Node<V> parent = null;
        boolean isCurrentChildLo = false;
        while (true) {
        	//perform copy on write
        	n = new Node<V>(n);
        	if (parent != null) {
        		if (isCurrentChildLo) {
        			parent.lo = n;
        		} else {
        			parent.hi = n;
        		}
        	} else {
        		newInfo.root = n;
        	}

        	//insertion
        	if (parentPosDiff + 1 != n.posDiff) {
        		//split in prefix?
        		int posDiff = compare(key, prefix);
        		if (posDiff < n.posDiff && posDiff != -1) {
        			Node<V> newSub;
        			long subPrefix = extractPrefix(prefix, posDiff - 1);
        			if (BitTools.getBit(key, posDiff)) {
        				newSub = new Node<V>(prefix, null, key, val, posDiff);
        				newSub.lo = n;
        			} else {
        				newSub = new Node<V>(key, val, prefix, null, posDiff);
        				newSub.hi = n;
        			}
        			if (parent == null) {
        				newInfo.rootKey = subPrefix;
        				newInfo.root = newSub;
        			} else if (isCurrentChildLo) {
        				parent.loPost = subPrefix;
        				parent.lo = newSub;
        			} else {
        				parent.hiPost = subPrefix;
        				parent.hi = newSub;
        			}
        			newInfo.size++;
        			return null;
        		}
        	}

        	//prefix matches, so now we check sub-nodes and postfixes
        	if (BitTools.getBit(key, n.posDiff)) {
        		if (n.hi != null) {
        			prefix = n.hiPost;
        			parent = n;
        			n = n.hi;
        			isCurrentChildLo = false;
        		} else {
        			Node<V> n2 = createNode(key, val, n.hiPost, n.hiVal);
        			if (n2 == null) {
        				V prev = n.hiVal;
        				n.hiVal = val;
        				return prev;
        			}
        			n.hi = n2;
        			n.hiPost = extractPrefix(key, n2.posDiff - 1);
        			n.hiVal = null;
        			newInfo.size++;
        			return null;
        		}
        	} else {
        		if (n.lo != null) {
        			prefix = n.loPost;
        			parent = n;
        			n = n.lo;
        			isCurrentChildLo = true;
        		} else {
        			Node<V> n2 = createNode(key, val, n.loPost, n.loVal);
        			if (n2 == null) {
        				V prev = n.loVal;
        				n.loVal = val;
        				return prev;
        			}
        			n.lo = n2;
        			n.loPost = extractPrefix(key, n2.posDiff - 1);
        			n.loVal = null;
        			newInfo.size++;
        			return null;
        		}
        	}
        	parentPosDiff = n.posDiff;
        }
    }

    public void printTree() {
        System.out.println("Tree: \n" + toString());
    }

    @Override
    public String toString() {
        AtomicInfo<V> info = this.info;
        if (info.size == 0) {
            return "- -";
        }
        if (info.root == null) {
            return "-" + BitTools.toBinary(info.rootKey, 64) + " v=" + info.rootVal;
        }
        Node<V> n = info.root;
        StringBuilder s = new StringBuilder();
        printNode(n, s, "", 0, info.rootKey);
        return s.toString();
    }

    private void printNode(Node<V> n, StringBuilder s, String level, int currentDepth, long infix) {
        char NL = '\n';
        if (currentDepth != n.posDiff) {
            s.append(level + "n: " + currentDepth + "/" + n.posDiff + " " +
                    BitTools.toBinary(infix, 64) + NL);
        } else {
            s.append(level + "n: " + currentDepth + "/" + n.posDiff + " i=0" + NL);
        }
        if (n.lo != null) {
            printNode(n.lo, s, level + "-", n.posDiff+1, n.loPost);
        } else {
            s.append(level + " " + BitTools.toBinary(n.loPost, 64) + " v=" + n.loVal + NL);
        }
        if (n.hi != null) {
            printNode(n.hi, s, level + "-", n.posDiff+1, n.hiPost);
        } else {
            s.append(level + " " + BitTools.toBinary(n.hiPost,64) + " v=" + n.hiVal + NL);
        }
    }

    public boolean checkTree() {
        AtomicInfo<V> info = this.info;
        if (info.root == null) {
            if (info.size > 1) {
                System.err.println("root node = null AND size = " + info.size);
                return false;
            }
            return true;
        }
        return checkNode(info.root, 0, info.rootKey);
    }

    private boolean checkNode(Node<V> n, int firstBitOfNode, long prefix) {
        //check prefix
        if (n.posDiff < firstBitOfNode) {
            System.err.println("prefix with len=0 detected!");
            return false;
        }
        if (n.lo != null) {
            if (!doesPrefixMatch(n.posDiff-1, n.loPost, prefix)) {
                System.err.println("lo: prefix mismatch");
                return false;
            }
            checkNode(n.lo, n.posDiff+1, n.loPost);
        }
        if (n.hi != null) {
            if (!doesPrefixMatch(n.posDiff-1, n.hiPost, prefix)) {
                System.err.println("hi: prefix mismatch");
                return false;
            }
            checkNode(n.hi, n.posDiff+1, n.hiPost);
        }
        return true;
    }

    private Node<V> createNode(long k1, V val1, long k2, V val2) {
        int posDiff = compare(k1, k2);
        if (posDiff == -1) {
            return null;
        }
        if (BitTools.getBit(k2, posDiff)) {
            return new Node<V>(k1, val1, k2, val2, posDiff);
        } else {
            return new Node<V>(k2, val2, k1, val1, posDiff);
        }
    }

    /**
     *
     * @param v
     * @param endPos last bit of prefix, counting starts with 0 for 1st bit
     * @return The prefix.
     */
    private static long extractPrefix(long v, int endPos) {
        long inf = v;
        //avoid shifting by 64 bit which means 0 shifting in Java!
        if (endPos < 63) {
            inf &= ~((-1L) >>> (1+endPos)); // & 0x3f == %64
        }
        return inf;
    }

    /**
     *
     * @param v
     * @return True if the prefix matches the value or if no prefix is defined
     */
    private boolean doesPrefixMatch(int posDiff, long v, long prefix) {
        if (posDiff > 0) {
            return (v ^ prefix) >>> (64-posDiff) == 0;
        }
        return true;
    }

    /**
     * Compares two values.
     * @param v1
     * @param v2
     * @return Position of the differing bit, or -1 if both values are equal
     */
    private static int compare(long v1, long v2) {
        return (v1 == v2) ? -1 : Long.numberOfLeadingZeros(v1 ^ v2);
    }

    /**
     * Get the size of the tree.
     * @return the number of keys in the tree
     */
    public int size() {
        return info.size;
    }

    /**
     * Check whether a given key exists in the tree.
     * @param key
     * @return {@code true} if the key exists otherwise {@code false}
     */
    public boolean contains(long key) {
        AtomicInfo<V> info = this.info;
        int size = info.size;
        if (size == 0) {
            return false;
        }
        if (size == 1) {
            return key == info.rootKey;
        }
        Node<V> n = info.root;
        long prefix = info.rootKey;
        while (doesPrefixMatch(n.posDiff, key, prefix)) {
            //prefix matches, so now we check sub-nodes and postfixes
            if (BitTools.getBit(key, n.posDiff)) {
                if (n.hi != null) {
                    prefix = n.hiPost;
                    n = n.hi;
                    continue;
                }
                return key == n.hiPost;
            } else {
                if (n.lo != null) {
                    prefix = n.loPost;
                    n = n.lo;
                    continue;
                }
                return key == n.loPost;
            }
        }
        return false;
    }

    /**
     * Get the value for a given key.
     * @param key
     * @return the values associated with {@code key} or {@code null} if the key does not exist.
     */
    public V get(long key) {
        AtomicInfo<V> info = this.info;
        if (info.size == 0) {
            return null;
        }
        if (info.size == 1) {
            return (key == info.rootKey) ? info.rootVal : null;
        }
        Node<V> n = info.root;
        long prefix = info.rootKey;
        while (doesPrefixMatch(n.posDiff, key, prefix)) {
        	//prefix matches, so now we check sub-nodes and postfixes
        	if (BitTools.getBit(key, n.posDiff)) {
        		if (n.hi != null) {
        			prefix = n.hiPost;
        			n = n.hi;
        			continue;
        		}
        		return (key == n.hiPost) ? n.hiVal : null;
        	} else {
        		if (n.lo != null) {
        			prefix = n.loPost;
        			n = n.lo;
        			continue;
        		}
        		return (key == n.loPost) ? n.loVal : null;
        	}
        }
        return null;
    }

    /**
     * Remove a key and its value
     * @param key
     * @return The value of the key of {@code null} if the value was not found.
     */
    public V remove(long key) {
    	try {
    		writeLock.lock();
    		if (contains(key)) {
    			AtomicInfo<V> newInfo = info.copy();
    			V ret = doRemove(key, newInfo);
    			this.info = newInfo;
    			return ret;
    		} else {
    			return null;
    		}
    	} finally {
    		writeLock.unlock();
    	}
    }

    private V doRemove(long key, AtomicInfo<V> newInfo) {
    	AtomicInfo<V> info = this.info;
    	if (info.size == 0) {
    		return null;
    	}
    	if (info.size == 1) {
    		if (key == info.rootKey) {
    			newInfo.size--;
    			newInfo.rootKey = 0;
    			V prev = info.rootVal;
    			info.rootVal = null;
    			return prev;
    		}
    		return null;
    	}
    	Node<V> n = info.root;
    	Node<V> parent = null;
    	boolean isParentHigh = false;
    	long prefix = info.rootKey;
    	while (doesPrefixMatch(n.posDiff, key, prefix)) {
    		//perform copy on write
    		n = new Node<V>(n);
    		if (parent != null) {
    			if (!isParentHigh) {
    				parent.lo = n;
    			} else {
    				parent.hi = n;
    			}
    		} else {
    			newInfo.root = n;
    		}

    		//prefix matches, so now we check sub-nodes and postfixes
    		if (BitTools.getBit(key, n.posDiff)) {
    			if (n.hi != null) {
    				isParentHigh = true;
    				prefix = n.hiPost;
    				parent = n;
    				n = n.hi;
    				continue;
    			} else {
    				if (key != n.hiPost) {
    					return null;
    				}
    				//match! --> delete node
    				//replace data in parent node
    				updateParentAfterRemove(newInfo, parent, n.loPost, n.loVal, n.lo, isParentHigh);
    				return n.hiVal;
    			}
    		} else {
    			if (n.lo != null) {
    				isParentHigh = false;
    				prefix = n.loPost;
    				parent = n;
    				n = n.lo;
    				continue;
    			} else {
    				if (key != n.loPost) {
    					return null;
    				}
    				//match! --> delete node
    				//replace data in parent node
    				//for new prefixes...
    				updateParentAfterRemove(newInfo, parent, n.hiPost, n.hiVal, n.hi, isParentHigh);
    				return n.loVal;
    			}
    		}
    	}
        return null;
    }
    private void updateParentAfterRemove(AtomicInfo<V> newInfo, Node<V> parent, long newPost, V newVal,
                                         Node<V> newSub, boolean isParentHigh) {

        newPost = (newSub == null) ? newPost : extractPrefix(newPost, newSub.posDiff-1);
        if (parent == null) {
            newInfo.rootKey = newPost;
            newInfo.rootVal = newVal;
            newInfo.root = newSub;
        } else if (isParentHigh) {
            parent.hiPost = newPost;
            parent.hiVal = newVal;
            parent.hi = newSub;
        } else {
            parent.loPost = newPost;
            parent.loVal = newVal;
            parent.lo = newSub;
        }
        newInfo.size--;
    }

    public CBIterator<V> iterator() {
        return new CBIterator<V>(this, DEPTH);
    }

    public static class CBIterator<V> implements Iterator<V> {
        private long nextKey = 0;
        private V nextValue = null;
        private boolean hasNext = true;
        private final Node<V>[] stack;
        //0==read_lower; 1==read_upper; 2==go_to_parent
        private static final byte READ_LOWER = 0;
        private static final byte READ_UPPER = 1;
        private static final byte RETURN_TO_PARENT = 2;
        private final byte[] readHigherNext;
        private int stackTop = -1;

        @SuppressWarnings("unchecked")
        public CBIterator(CritBit64COW<V> cb, int DEPTH) {
            this.stack = new Node[DEPTH];
            this.readHigherNext = new byte[DEPTH];  // default = false
            AtomicInfo<V> info = cb.info;
            if (info.size == 0) {
                //Tree is empty
            	hasNext = false;
                return;
            }
            if (info.size == 1) {
                nextValue = info.rootVal;
                nextKey = info.rootKey;
                return;
            }
            stack[++stackTop] = info.root;
            findNext();
        }

        private void findNext() {
            while (stackTop >= 0) {
                Node<V> n = stack[stackTop];
                //check lower
                if (readHigherNext[stackTop] == READ_LOWER) {
                    readHigherNext[stackTop] = READ_UPPER;
                    if (n.lo == null) {
                        nextValue = n.loVal;
                        nextKey = n.loPost;
                        return;
                    } else {
                        stack[++stackTop] = n.lo;
                        readHigherNext[stackTop] = READ_LOWER;
                        continue;
                    }
                }
                //check upper
                if (readHigherNext[stackTop] == READ_UPPER) {
                    readHigherNext[stackTop] = RETURN_TO_PARENT;
                    if (n.hi == null) {
                        nextValue = n.hiVal;
                        nextKey = n.hiPost;
                        --stackTop;
                        return;
                        //proceed to move up a level
                    } else {
                        stack[++stackTop] = n.hi;
                        readHigherNext[stackTop] = READ_LOWER;
                        continue;
                    }
                }
                //proceed to move up a level
                --stackTop;
            }
            //Finished
            nextValue = null;
            nextKey = 0;
            hasNext = false;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public V next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            V ret = nextValue;
            findNext();
            return ret;
        }

        public long nextKey() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            long ret = nextKey;
            findNext();
            return ret;
        }

        public Entry<V> nextEntry() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Entry<V> ret = new Entry<V>(nextKey, nextValue);
            findNext();
            return ret;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * Queries the tree for entries with min<=key<=max.
     * @param min
     * @param max
     * @return An iterator over the matching entries.
     */
    public QueryIterator<V> query(long min, long max) {
        return new QueryIterator<V>(this, min, max, DEPTH);
    }

    public static class QueryIterator<V> implements Iterator<V> {
        private final long minOrig;
        private final long maxOrig;
        private long nextKey = 0;
        private V nextValue = null;
        private boolean hasNext = true;
        private final Node<V>[] stack;
        //0==read_lower; 1==read_upper; 2==go_to_parent
        private static final byte READ_LOWER = 0;
        private static final byte READ_UPPER = 1;
        private static final byte RETURN_TO_PARENT = 2;
        private final byte[] readHigherNext;
        private final long[] prefixes;
        private int stackTop = -1;

        @SuppressWarnings("unchecked")
        public QueryIterator(CritBit64COW<V> cb, long minOrig, long maxOrig, int DEPTH) {
            this.stack = new Node[DEPTH];
            this.readHigherNext = new byte[DEPTH];  // default = false
            this.prefixes = new long[DEPTH];
            this.minOrig = minOrig;
            this.maxOrig = maxOrig;
            AtomicInfo<V> info = cb.info;
            if (info.size == 0) {
                //Tree is empty
            	hasNext = false;
                return;
            }
            if (info.size == 1) {
                hasNext = checkMatchFullIntoNextVal(info.rootKey, info.rootVal);
                return;
            }
            Node<V> n = info.root;
            if (!checkMatch(info.rootKey, n.posDiff-1)) {
                return;
            }
            stack[++stackTop] = info.root;
            prefixes[stackTop] = info.rootKey;
            findNext();
        }

        private void findNext() {
            while (stackTop >= 0) {
                Node<V> n = stack[stackTop];
                //check lower
                if (readHigherNext[stackTop] == READ_LOWER) {
                    readHigherNext[stackTop] = READ_UPPER;
                    long valTemp = BitTools.set0(prefixes[stackTop], n.posDiff);
                    if (checkMatch(valTemp, n.posDiff)) {
                        if (n.lo == null) {
                            if (checkMatchFullIntoNextVal(n.loPost, n.loVal)) {
                                return;
                            }
                            //proceed to check upper
                        } else {
                            stack[++stackTop] = n.lo;
                            prefixes[stackTop] = n.loPost;
                            readHigherNext[stackTop] = READ_LOWER;
                            continue;
                        }
                    }
                }
                //check upper
                if (readHigherNext[stackTop] == READ_UPPER) {
                    readHigherNext[stackTop] = RETURN_TO_PARENT;
                    long valTemp = BitTools.set1(prefixes[stackTop], n.posDiff);
                    if (checkMatch(valTemp, n.posDiff)) {
                        if (n.hi == null) {
                            if (checkMatchFullIntoNextVal(n.hiPost, n.hiVal)) {
                                --stackTop;
                                return;
                            }
                            //proceed to move up a level
                        } else {
                            stack[++stackTop] = n.hi;
                            prefixes[stackTop] = n.hiPost;
                            readHigherNext[stackTop] = READ_LOWER;
                            continue;
                        }
                    }
                }
                //proceed to move up a level
                --stackTop;
            }
            //Finished
            nextValue = null;
            nextKey = 0;
            hasNext = false;
        }


        /**
         * Full comparison on the parameter. Assigns the parameter to 'nextVal' if comparison
         * fits.
         * @param keyTemplate
         * @return Whether we have a match or not
         */
        private boolean checkMatchFullIntoNextVal(long keyTemplate, V value) {
            if ((minOrig > keyTemplate) || (keyTemplate > maxOrig)) {
                return false;
            }
            nextValue = value;
            nextKey = keyTemplate;
            return true;
        }

        private boolean checkMatch(long keyTemplate, int currentDepth) {
            int toIgnore = 63 - currentDepth;
            long mask = (-1L) << toIgnore;
            if ((minOrig & mask) > (keyTemplate & mask)) {
                return false;
            }
            if ((keyTemplate & mask) > (maxOrig & mask)) {
                return false;
            }

            return true;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public V next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            V ret = nextValue;
            findNext();
            return ret;
        }

        public long nextKey() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            long ret = nextKey;
            findNext();
            return ret;
        }

        public Entry<V> nextEntry() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Entry<V> ret = new Entry<V>(nextKey, nextValue);
            findNext();
            return ret;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * Queries the tree for entries with min<=key<=max. Unlike the normal query, this
     * query also excludes all elements with (key|min)!=key and (key&max!=max).
     * See PH-Tree for a discussion.
     * @param min
     * @param max
     * @return An iterator over the matching entries.
     */
    public QueryIteratorMask<V> queryWithMask(long min, long max) {
        return new QueryIteratorMask<V>(this, min, max, DEPTH);
    }

    public static class QueryIteratorMask<V> implements Iterator<V> {
        private final long minOrig;
        private final long maxOrig;
        private long nextKey = 0;
        private V nextValue = null;
        private boolean hasNext = true;
        private final Node<V>[] stack;
        //0==read_lower; 1==read_upper; 2==go_to_parent
        private static final byte READ_LOWER = 0;
        private static final byte READ_UPPER = 1;
        private static final byte RETURN_TO_PARENT = 2;
        private final byte[] readHigherNext;
        private final long[] prefixes;
        private int stackTop = -1;

        @SuppressWarnings("unchecked")
        public QueryIteratorMask(CritBit64COW<V> cb, long minOrig, long maxOrig, int DEPTH) {
            this.stack = new Node[DEPTH];
            this.readHigherNext = new byte[DEPTH];  // default = false
            this.prefixes = new long[DEPTH];
            this.minOrig = minOrig;
            this.maxOrig = maxOrig;

            AtomicInfo<V> info = cb.info;
            if (info.size == 0) {
                //Tree is empty
            	hasNext = false;
                return;
            }
            if (info.size == 1) {
                hasNext = checkMatchFullIntoNextVal(info.rootKey, info.rootVal);
                return;
            }
            Node<V> n = info.root;
            if (!checkMatch(info.rootKey, n.posDiff-1)) {
                return;
            }
            stack[++stackTop] = info.root;
            prefixes[stackTop] = info.rootKey;
            findNext();
        }

        private void findNext() {
            while (stackTop >= 0) {
                Node<V> n = stack[stackTop];
                //check lower
                if (readHigherNext[stackTop] == READ_LOWER) {
                    readHigherNext[stackTop] = READ_UPPER;
                    long valTemp = BitTools.set0(prefixes[stackTop], n.posDiff);
                    if (checkMatch(valTemp, n.posDiff)) {
                        if (n.lo == null) {
                            if (checkMatchFullIntoNextVal(n.loPost, n.loVal)) {
                                return;
                            }
                            //proceed to check upper
                        } else {
                            stack[++stackTop] = n.lo;
                            prefixes[stackTop] = n.loPost;
                            readHigherNext[stackTop] = READ_LOWER;
                            continue;
                        }
                    }
                }
                //check upper
                if (readHigherNext[stackTop] == READ_UPPER) {
                    readHigherNext[stackTop] = RETURN_TO_PARENT;
                    long valTemp = BitTools.set1(prefixes[stackTop], n.posDiff);
                    if (checkMatch(valTemp, n.posDiff)) {
                        if (n.hi == null) {
                            if (checkMatchFullIntoNextVal(n.hiPost, n.hiVal)) {
                                --stackTop;
                                return;
                            }
                            //proceed to move up a level
                        } else {
                            stack[++stackTop] = n.hi;
                            prefixes[stackTop] = n.hiPost;
                            readHigherNext[stackTop] = READ_LOWER;
                            continue;
                        }
                    }
                }
                //proceed to move up a level
                --stackTop;
            }
            //Finished
            nextValue = null;
            nextKey = 0;
            hasNext = false;
        }


        /**
         * Full comparison on the parameter. Assigns the parameter to 'nextVal' if comparison
         * fits.
         * @param keyTemplate
         * @return Whether we have a match or not
         */
        private boolean checkMatchFullIntoNextVal(long keyTemplate, V value) {
			if (((keyTemplate | minOrig) & maxOrig) != keyTemplate) {
				return false;
			}
            nextValue = value;
            nextKey = keyTemplate;
            return true;
        }

        private boolean checkMatch(long keyTemplate, int currentDepth) {
            int toIgnore = 63 - currentDepth;
			return (((keyTemplate | minOrig) & maxOrig) ^ keyTemplate) >>> toIgnore == 0;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public V next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            V ret = nextValue;
            findNext();
            return ret;
        }

        public long nextKey() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            long ret = nextKey;
            findNext();
            return ret;
        }

        public Entry<V> nextEntry() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Entry<V> ret = new Entry<V>(nextKey, nextValue);
            findNext();
            return ret;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    public static class Entry<V> {
        private final long key;
        private final V value;
        Entry(long key, V value) {
            this.key = key;
            this.value = value;
        }
        public long key() {
            return key;
        }
        public V value() {
            return value;
        }
    }
}
