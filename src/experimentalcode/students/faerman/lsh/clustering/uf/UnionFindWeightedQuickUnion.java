package experimentalcode.students.faerman.lsh.clustering.uf;

import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Collection;
import java.util.LinkedList;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

public class UnionFindWeightedQuickUnion<K> implements UnionFind<K> {

  private int[] mappingToComponent;

  private int[] height;

  private int nextElementIndex;

  private THashMap<K, Integer> fromElementToItsIndex;
  private TIntObjectHashMap<K> fromIndexToElement;

  @Override
  public void init(int numberOfElements) {
    fromElementToItsIndex = new THashMap<>(numberOfElements);
    fromIndexToElement=new TIntObjectHashMap<>(numberOfElements);
    nextElementIndex = 0;
    mappingToComponent = new int[numberOfElements];
    for(int i = 0; i < mappingToComponent.length; i++) {
      mappingToComponent[i] = i;
    }
    height = new int[numberOfElements];
  }

  private int find(K element) {
    int componentNumber = getElementIndex(element);
    return find(componentNumber);
  }

  private int find(int componentNumber) {
    while(componentNumber != mappingToComponent[componentNumber]) {
      componentNumber = mappingToComponent[componentNumber];
    }
    return componentNumber;
  }

  private int getElementIndex(K element) {
    Integer elementIndex = fromElementToItsIndex.get(element);
    if(elementIndex == null) {
      if(nextElementIndex > mappingToComponent.length - 1) {
        throw new RuntimeException("exceeded number of allowed elements");
      }
      elementIndex = nextElementIndex++;
      fromElementToItsIndex.put(element, elementIndex);
      fromIndexToElement.put(elementIndex, element);
    }
    return elementIndex;
  }

  @Override
  public void union(K first, K second) {
    int firstIndex = getElementIndex(first);
    int secondIndex = getElementIndex(second);
    int firstComponent = find(firstIndex);
    int secondComponent = find(secondIndex);
    if(firstComponent == secondComponent) {
      return;
    }
    if(height[firstComponent]>height[secondComponent])
    {
      mappingToComponent[secondComponent]=firstComponent;
      height[firstComponent]=Math.max(height[firstComponent], height[secondComponent]+1);
      //to find the roots
      height[secondComponent]=0;
    }
    else{
      mappingToComponent[firstComponent]=secondComponent;
      height[secondComponent]=Math.max(height[secondComponent], height[firstComponent]+1);
      //to find the roots
      height[firstComponent]=0;
    }
  }

  @Override
  public boolean isConnected(K first, K second) {
    return find(first) == find(second);
  }

  public int maxTreeHeight(){
    int maxTreeHeight=0;
    for(int i = 0; i < height.length; i++) {
      if(height[i]>maxTreeHeight)
      {
        maxTreeHeight=height[i];
      }
    }
    return maxTreeHeight;
  }

  @Override
  public Collection<K> getRoots() {
    LinkedList<K> roots=new LinkedList<>();
    for(int i = 0; i < nextElementIndex; i++) {
      //roots or one element in component
      if(height[i]!=0||mappingToComponent[i]==i)
      {
        roots.add(fromIndexToElement.get(i));
      }
    }
  return roots;
  }
  
}
