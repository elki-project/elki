package experimentalcode.shared.index.subspace.vafile;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Logger;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DoubleDistanceResultPair;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import experimentalcode.shared.index.subspace.structures.DiskMemory;

/**
 * VAFile
 *
 * @author Thomas Bernecker
 * @created 15.09.2009
 * @date 15.09.2009
 */
public class VAFile extends AbstractVAFile
{
  Logger log = Logger.getLogger(VAFile.class.getName());
  //Full data representation
	DiskMemory<DoubleVector> data;
	
	//temporary, full-dimensional VA representation
	private List<VectorApprox> vectorApprox;
	
	private static final int p = 2;
	
	int initialisations = 10, swaps = 10;
	
	int bufferSize = Integer.MAX_VALUE;
	
	private Random rand = new Random(1);
	
	private long scannedBytes, queryTime;
	
	private int issuedQueries;
	
	int pageSize;
	
	private double[][] splitPositions;
	private double[][] lookup;
	
	
	public VAFile(int pageSize, Relation<DoubleVector> fullDimensionalData, int partitions)
	{	
		this.pageSize = pageSize;
		
		scannedBytes = 0;
		issuedQueries = 0;
		
		setPartitions(fullDimensionalData, partitions);
		
		DBID sampleID = fullDimensionalData.getDBIDs().iterator().next();
	    int dimensions = fullDimensionalData.get(sampleID).getDimensionality();
		data = new DiskMemory<DoubleVector>(pageSize/(8*dimensions + 4),bufferSize);
		vectorApprox = new ArrayList<VectorApprox>();
		for (DBID id: fullDimensionalData.getDBIDs()) {
			DoubleVector dv = fullDimensionalData.get(id);
			data.add(id, dv);
			VectorApprox va = new VectorApprox(id, dv.getDimensionality());
			try { va.calculateApproximation(dv, splitPositions); }
			catch (Exception e) { e.printStackTrace(); }
			vectorApprox.add(va);
		}
	}
	
	
	public void setPartitions(Relation<DoubleVector> objects, int partitions) throws IllegalArgumentException
	{
		if ((Math.log(partitions) / Math.log(2)) != (int)(Math.log(partitions) / Math.log(2)))
			throw new IllegalArgumentException("Number of partitions must be a power of 2!");
		
		DBID sampleID = objects.getDBIDs().iterator().next();
	    int dimensions = objects.get(sampleID).getDimensionality();
		splitPositions = new double[dimensions][partitions+1];
		int[][] partitionCount = new int[dimensions][partitions];
		
		for (int d = 0; d < dimensions; d++)
		{
			int size = objects.size();
			int remaining = size;
			double[] tempdata = new double[size];
			int j = 0;
			for (DBID id : objects.getDBIDs()) {
				tempdata[j++] = objects.get(id).doubleValue(d+1);
			}
			Arrays.sort(tempdata);
			
			int bucketSize = (int)(size / (double)partitions);
			int i = 0;
			for (int b = 0; b < partitions; b++)
			{
				assert i <= tempdata.length : "i out ouf bounds "+i+" <> "+tempdata.length;
				splitPositions[d][b] = tempdata[i];
				remaining -= bucketSize;
				i += bucketSize;
				
				// test: are there remaining objects that have to be put in the first buckets?
				if (remaining > (bucketSize * (partitionCount.length - b - 1)))
				{
					i++;
					remaining--;
					partitionCount[d][b]++;
				}
				
				partitionCount[d][b] += bucketSize;
			}
			splitPositions[d][partitions] = tempdata[size-1] + 0.000001; // make sure that last object will be included
			
			log.fine("dim " + (d+1) + ": ");
			for (int b=0; b<splitPositions[d].length; b++)
			{
				log.fine(splitPositions[d][b] + "  ");
				if (b < splitPositions[d].length-1)
				{
					log.fine("(bucket "+(b+1)+"/"+partitions+", " + partitionCount[d][b] +")  ");
				}
			}
			log.fine(null);
		}
	}


	/**
	 * @return the split positions
	 */
	public double[][] getSplitPositions() {
		return splitPositions;
	}
	
	
	public double[] getMinDists(int dimension, int queryCell) {
		
		double[] result = new double[splitPositions[dimension].length-1];
		for (int i = 0; i < result.length; i++)
		{
			if (i < queryCell) result[i] = lookup[dimension][i+1];
			else if (i > queryCell) result[i] = lookup[dimension][i];
			else result[i] = Math.min(lookup[dimension][i], lookup[dimension][i+1]);
		}
		return result;
	}
	
	
	public double[] getMaxDists(int dimension, int queryCell) {
		
		double[] result = new double[splitPositions[dimension].length-1];
		for (int i = 0; i < result.length; i++)
		{
			if (i < queryCell) result[i] = lookup[dimension][i];
			else if (i > queryCell) result[i] = lookup[dimension][i+1];
			else result[i] = Math.max(lookup[dimension][i], lookup[dimension][i+1]);
		}
		return result;
	}


	/**
	 * @param query
	 */
	public void setLookupTable(DoubleVector query) {
		
		int dimensions = splitPositions.length;
		int bordercount = splitPositions[0].length;
		lookup = new double[dimensions][bordercount];
		for (int d = 0; d < dimensions; d++)
		{
			for (int i = 0; i < bordercount; i++)
			{
				lookup[d][i] = Math.pow(splitPositions[d][i] - query.doubleValue(d+1), p);
			}
		}		
	}
	
	
	public DBIDs knnQuery(DoubleVector query, int k)
	{
		// generate query approximation and lookup table
		
		VectorApprox queryApprox = new VectorApprox(query.getDimensionality());
		try { queryApprox.calculateApproximation(query, splitPositions); }
		catch (Exception e) { e.printStackTrace(); }
		setLookupTable(query);
		
		
		// perform multi-step NN-query
		
		Vector<VectorApprox> candidates = new Vector<VectorApprox>();
		double minMaxDist = Double.POSITIVE_INFINITY;
		
		
		// filter step
		
		for (int i=0; i<vectorApprox.size(); i++)
		{
			VectorApprox va = vectorApprox.get(i);
			double minDist = 0;
			double maxDist = 0;
			for (int d = 0; d < va.getApproximationSize(); d++)
			{
				int queryApproxDim = queryApprox.getApproximation(d);
				int vectorApproxDim = va.getApproximation(d);
				minDist += getMinDists(d, queryApproxDim)[vectorApproxDim];
				maxDist += getMaxDists(d, queryApproxDim)[vectorApproxDim];
			}
			if (minDist < minMaxDist) // object has to be refined
			{
				va.increasePMinDist(minDist);
				va.increasePMaxDist(maxDist);
				candidates.add(va);
			}
			minMaxDist = Math.min(minMaxDist, maxDist);
		}
		
		
		// refinement step
		
		List<DoubleDistanceResultPair> result = new ArrayList<DoubleDistanceResultPair>();
		
		// sort candidates by lower bound (minDist)
		candidates = VectorApprox.sortByMinDist(candidates);
		
		// retrieve accurate distances
		for (int i=0; i<candidates.size(); i++)
		{
			VectorApprox va = candidates.get(i);
			DoubleDistanceResultPair lastElement = null;
			if (!result.isEmpty()) lastElement = result.get(result.size()-1);
			if (result.size() < k || va.getPMinDist() < lastElement.getDoubleDistance())
			{
				DoubleVector dv = data.getObject(va.getId());
				double dist = 0;
				for (int d = 0; d < dv.getDimensionality(); d++)
				{
					dist += Math.pow(dv.doubleValue(d+1) - query.doubleValue(d+1), p);
				}
				DoubleDistanceResultPair dp = new DoubleDistanceResultPair(dist, va.getId());
				
				if (result.size() >= k)
					result.remove(lastElement);
					
				result.add(dp);
				Collections.sort(result, new DoubleDistanceResultPairComparator());
			}
		}
		
		log.fine("\nquery = (" + query + ")");
		log.fine("database: " + vectorApprox.size() + ", candidates: " + candidates.size() + ", results: " + result.size());
		
		ModifiableDBIDs resultIDs = DBIDUtil.newArray(result.size());
		for (DoubleDistanceResultPair dp: result)
		{
			log.fine(dp.toString());
			resultIDs.add(dp.getDBID());
		}
		
		return resultIDs;
	}
}
