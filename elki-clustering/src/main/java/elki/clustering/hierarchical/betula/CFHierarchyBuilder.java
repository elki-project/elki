/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2021
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.clustering.hierarchical.betula;

import java.util.ArrayList;
import java.util.Map;

import elki.clustering.hierarchical.PointerHierarchyResult;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDBIDDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDArrayMIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDVar;
import elki.database.ids.DBIDs;
import elki.logging.Logging;

/**
 * Class to help building a pointer hierarchy on CFs.
 *
 * @author Andreas Lang
 *
 * 
 * @has - - - PointerHierarchyResult
 */
public class CFHierarchyBuilder {
    /**
     * Class logger.
     */
    private static final Logging LOG = Logging.getLogger(CFHierarchyBuilder.class);

    /**
     * Merge counter (for merge ordering).
     */
    protected int mergecount = 0;

    /**
     * Clustering Features
     */
    protected CFInterface[] cfs;

    /**
     * parent ids
     */
    protected int[] parent;

    /**
     * order
     */
    protected int[] order;

    protected int[] csize;

    /**
     * Distance to the parent object.
     */
    protected double[] parentDistance;

    /**
     * Constructor.
     *
     * @param ids IDs
     * @param isSquared Flag to indicate squared distances
     */
    public CFHierarchyBuilder(CFInterface[] cfs) {
        super();
        this.cfs = cfs;
        parent = new int[cfs.length];
        parentDistance = new double[cfs.length];
        order = new int[cfs.length];
        for(int i = 0; i < cfs.length; i++) {
            parentDistance[i] = Double.POSITIVE_INFINITY;
            parent[i] = i;
        }
    }

    /**
     * Test if an object is already linked.
     *
     * @param id Object
     * @return {@code true} if the object is already linked.
     */
    public boolean isLinked(int id) {
        return parentDistance[id] < Double.POSITIVE_INFINITY;
    }

    /**
     * Add an element to the pointer representation.
     * <p>
     * Important: If an algorithm does not produce links in an increasing
     * fashion,
     * a warning will be issued and the linking distance will be increased.
     * Otherwise, the hierarchy would be misinterpreted when links are executed
     * ordered by their distance.
     *
     * @param cur Current object id
     * @param distance Link distance
     * @param par Parent id
     */
    public void addCF(int cur, double distance, int par) {
        // assert prototypes == null;
        parent[cur] = par;
        assert (parentDistance[cur] == Double.POSITIVE_INFINITY) : "Object was already linked!";
        parentDistance[cur] = distance;
        cfs[par].addToStatistics(cfs[cur]);
        order[cur] = mergecount;
        ++mergecount;
    }

    /**
     * Add an element to the pointer representation.
     * <p>
     * Important: If an algorithm does not produce links in an increasing
     * fashion,
     * a warning will be issued and the linking distance will be increased.
     * Otherwise, the hierarchy would be misinterpreted when links are executed
     * ordered by their distance.
     *
     * @param cur Current object id
     * @param distance Link distance
     * @param par Parent id
     */
    public void add(int cur, double distance, int par) {
        // assert prototypes == null;
        parent[cur] = par;
        assert (parentDistance[cur] == Double.POSITIVE_INFINITY) : "Object was already linked!";
        parentDistance[cur] = distance;
        order[cur] = mergecount;
        ++mergecount;
    }

    /**
     * Get the cluster size of the current object.
     *
     * @param id Object id
     * @return Cluster size.
     */
    public int getSize(int id) {
        if(csize == null) {
            csize = new int[cfs.length];
        }
        return csize[id];
    }

    /**
     * Set the cluster size of the current object.
     *
     * @param id Object id
     * @param size new Size of current object
     */
    public void setSize(int id, int size) {
        if(csize == null) {
            csize = new int[cfs.length];
        }
        csize[id] = size;
    }

    /**
     * Finalize the result.
     *
     * @return Completed result
     */
    public PointerHierarchyResult complete(ArrayList<CFInterface> tcf, DBIDs ids, Map<CFInterface, ArrayModifiableDBIDs> idmap) {
        if(mergecount != cfs.length - 1) {
            LOG.warning(mergecount + " merges were added to the hierarchy, expected " + (cfs.length - 1));
        }

        // Map<CFInterface, Integer> cfmap = new HashMap<CFInterface,
        // Integer>(tree.leaves);
        // for(int i = 0; i < tree.leaves; i++) {
        // cfmap.put(tcf[i], i);
        // }


        WritableDBIDDataStore finalParent = DataStoreUtil.makeDBIDStorage(ids, DataStoreFactory.HINT_DB | DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC);
        WritableDoubleDataStore finalParentDistance = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB | DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Double.POSITIVE_INFINITY);
        WritableIntegerDataStore finalOrder = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_DB | DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, ids.size());

        DBIDVar[] reps = new DBIDVar[cfs.length];
        for(int i = 0; i < tcf.size(); i++) {
            ArrayModifiableDBIDs mids = idmap.get(tcf.get(i));
            double distance = tcf.get(i).variance(); // TODO solution based on
                                                     // linkage?
            DBIDArrayMIter iter = mids.iter();
            DBIDVar rep = DBIDUtil.newVar();
            rep.set(iter);
            reps[i] = rep;
            for(; iter.valid(); iter.advance()) {
                finalParent.put(iter, rep);
                finalParentDistance.put(iter, distance);
                finalOrder.put(iter, 0);
            }
        }

        for(int i = 0; i < reps.length; i++) {
            finalParent.put(reps[i], reps[parent[i]]);
            finalParentDistance.put(reps[i], parentDistance[i]);
            finalOrder.put(reps[i], order[i] + 1);// TODO check for unset order
        }
        return new PointerHierarchyResult(ids, finalParent, finalParentDistance, true, finalOrder);
    }

}
