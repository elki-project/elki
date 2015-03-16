package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.mst.utils.uf;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

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

import gnu.trove.map.hash.TIntObjectHashMap;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

public class UnionFindWeightedQuickUnion implements UnionFind<DBIDRef> {

  private int[] mappingToComponent;

  private int[] height;

  private WritableIntegerDataStore fromElementsToItsIndex;

  private TIntObjectHashMap<DBID> fromIndexToElement;

  private int numberOfElements;

  public UnionFindWeightedQuickUnion(DBIDs elements) {
    numberOfElements = elements.size();
    mappingToComponent = new int[elements.size()];
    for(int i = 0; i < mappingToComponent.length; i++) {
      mappingToComponent[i] = i;
    }
    height = new int[elements.size()];
    createMappings(elements);
  }

  private void createMappings(DBIDs elements) {
    fromElementsToItsIndex = DataStoreUtil.makeIntegerStorage(elements, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC);
    fromIndexToElement = new TIntObjectHashMap<>(elements.size());
    int counter = 0;
    for(DBIDIter iter = elements.iter(); iter.valid(); iter.advance()) {
      DBID currentElement = DBIDUtil.deref(iter);
      fromIndexToElement.put(counter, currentElement);
      fromElementsToItsIndex.put(iter, counter);
      counter++;
    }
  }

  @Override
  public int find(DBIDRef element) {
    int componentNumber = getElementIndex(element);
    return find(componentNumber);
  }

  private int find(int componentNumber) {
    while(componentNumber != mappingToComponent[componentNumber]) {
      componentNumber = mappingToComponent[componentNumber];
    }
    return componentNumber;
  }

  private int getElementIndex(DBIDRef element) {
    Integer elementIndex = fromElementsToItsIndex.intValue(element);
    if(elementIndex > numberOfElements - 1) {
      throw new RuntimeException(element + " element  is not known");
    }

    return elementIndex;
  }

  @Override
  public int union(DBIDRef first, DBIDRef second) {
    int firstIndex = getElementIndex(first);
    int secondIndex = getElementIndex(second);
    int firstComponent = find(firstIndex);
    int secondComponent = find(secondIndex);
    if(firstComponent == secondComponent) {
      return firstComponent;
    }
    int newComponentNumber = -1;
    if(height[firstComponent] > height[secondComponent]) {
      mappingToComponent[secondComponent] = firstComponent;
      height[firstComponent] = Math.max(height[firstComponent], height[secondComponent] + 1);
      // to find the roots
      height[secondComponent] = 0;
      newComponentNumber = firstComponent;
    }
    else {
      mappingToComponent[firstComponent] = secondComponent;
      height[secondComponent] = Math.max(height[secondComponent], height[firstComponent] + 1);
      // to find the roots
      height[firstComponent] = 0;
      newComponentNumber = secondComponent;
    }
    return newComponentNumber;
  }

  @Override
  public boolean isConnected(DBIDRef first, DBIDRef second) {
    return find(first) == find(second);
  }

  public int maxTreeHeight() {
    int maxTreeHeight = 0;
    for(int i = 0; i < height.length; i++) {
      if(height[i] > maxTreeHeight) {
        maxTreeHeight = height[i];
      }
    }
    return maxTreeHeight;
  }

  @Override
  public DBIDs getRoots() {
    ArrayModifiableDBIDs roots = DBIDUtil.newArray();
    for(int i = 0; i < mappingToComponent.length; i++) {
      // roots or one element in component
      if(mappingToComponent[i] == i) {
        roots.add(fromIndexToElement.get(i));
      }
    }
    return roots;
  }
}
