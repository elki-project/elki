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

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import experimentalcode.shared.index.subspace.structures.DiskMemory;
import experimentalcode.thomas.structures.IdDistTuple;

/**
 * VAFile
 *
 * @author Thomas Bernecker
 * @created 15.09.2009
 * @date 15.09.2009
 */
public class VAFile extends AbstractVAFile
{
	//Full data representation
	DiskMemory<DoubleVector> data;
	
	//temporary, full-dimensional VA representation
	Vector<VectorApprox> vectorApprox;
	
	private static final int p = 2;
	
	int initialisations = 10, swaps = 10;
	
	int bufferSize = Integer.MAX_VALUE;
	
	private Random rand = new Random(1);
	
	private long scannedBytes, queryTime;
	
	private int issuedQueries;
	
	int pageSize;
	
	private double[][] borders;
	private double[][] lookup;
	
	
	public VAFile(int pageSize, List<DoubleVector> fullDimensionalData, int partitions)
	{	
		this.pageSize = pageSize;
		
		scannedBytes = 0;
		issuedQueries = 0;
		
		setPartitions(fullDimensionalData, partitions);
		
		data = new DiskMemory<DoubleVector>(pageSize/(8*fullDimensionalData.get(0).getDimensionality() + 4),bufferSize);
		for (DoubleVector dv: fullDimensionalData)
		{
			data.add(dv);
			VectorApprox va = new VectorApprox(dv.getID(), dv.getDimensionality());
			try { va.calculateApproximation(dv, borders); }
			catch (Exception e) { e.printStackTrace(); }
			vectorApprox.add(va);
		}
	}
	
	
	public void setPartitions(List<DoubleVector> objects, int partitions) throws IllegalArgumentException
	{
		if ((Math.log(partitions) / Math.log(2)) != (int)(Math.log(partitions) / Math.log(2)))
			throw new IllegalArgumentException("Number of partitions must be a power of 2!");
		
		int dimensions = objects.get(0).getDimensionality();
		borders = new double[dimensions][partitions+1];
		int[][] partitionCount = new int[dimensions][partitions];
		
		for (int d = 0; d < dimensions; d++)
		{
			int size = objects.size();
			int remaining = size;
			double[] tempdata = new double[size];
			int j = 0;
			for (DoubleVector o: objects)
				tempdata[j++] = o.doubleValue(d+1);
			Arrays.sort(tempdata);
			
			int bucketSize = (int)(size / (double)partitions);
			int i = 0;
			for (int b = 0; b < partitionCount.length; b++)
			{
				borders[d][b] = tempdata[i];
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
			borders[d][partitions] = tempdata[size-1] + 0.000001; // make sure that last object will be included
			
			System.out.print("dim " + (d+1) + ": ");
			for (int b=0; b<borders.length; b++)
			{
				System.out.print(borders[b] + "  ");
				if (b < borders.length-1)
				{
					System.out.print("(bucket "+(b+1)+"/"+partitionCount.length+", " + partitionCount[b] +")  ");
				}
			}
			System.out.println();
		}
	}


	/**
	 * @return the borders
	 */
	public double[][] getBorders() {
		return borders;
	}
	
	
	public double[] getMinDists(int dimension, int queryCell) {
		
		double[] result = new double[borders[dimension].length-1];
		for (int i = 0; i < result.length; i++)
		{
			if (i < queryCell) result[i] = lookup[dimension][i+1];
			else if (i > queryCell) result[i] = lookup[dimension][i];
			else result[i] = Math.min(lookup[dimension][i], lookup[dimension][i+1]);
		}
		return result;
	}
	
	
	public double[] getMaxDists(int dimension, int queryCell) {
		
		double[] result = new double[borders[dimension].length-1];
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
		
		int dimensions = borders.length;
		int bordercount = borders[0].length;
		lookup = new double[dimensions][bordercount];
		for (int d = 0; d < dimensions; d++)
		{
			for (int i = 0; i < bordercount; i++)
			{
				lookup[d][i] = Math.pow(borders[d][i] - query.doubleValue(d), p);
			}
		}		
	}
	
	
	public DBIDs knnQuery(DoubleVector query, int k)
	{
		// generate query approximation and lookup table
		
		VectorApprox queryApprox = new VectorApprox(query.getID(), query.getDimensionality());
		try { queryApprox.calculateApproximation(query, borders); }
		catch (Exception e) { e.printStackTrace(); }
		setLookupTable(query);
		
		
		// perform multi-step NN-query
		
		Vector<VectorApprox> candidates = new Vector<VectorApprox>();
		double minMaxDist = Double.POSITIVE_INFINITY;
		
		
		// filter step
		
		for (int i = 0; i < vectorApprox.size(); i++)
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
		
		Vector<IdDistTuple> result = new Vector<IdDistTuple>();
		
		// sort candidates by lower bound (minDist)
		candidates = VectorApprox.sortByMinDist(candidates);
		
		// retrieve accurate distances
		for (VectorApprox va: candidates)
		{
			if (result.size() < k || va.getPMinDist() < result.lastElement().getPDist())
			{
				DoubleVector dv = data.getObject(va.getID());
				double dist = 0;
				for (int d = 0; d < dv.getDimensionality(); d++)
				{
					dist += Math.pow(dv.doubleValue(d) - query.doubleValue(d), p);
				}
				IdDistTuple tup = new IdDistTuple(va.getID());
				tup.setPDist(dist);
				
				if (result.size() >= k)
					result.remove(result.lastElement());
					
				result.add(tup);
				IdDistTuple.sortByDist(result);
			}
		}
		
		System.out.println("\nquery = " + query);
		System.out.println("database: " + vectorApprox.size() + ", candidates: " + candidates.size() + ", results: " + result.size());
		
		ModifiableDBIDs resultIDs = DBIDUtil.newArray(result.size());
		for (IdDistTuple o: result)
		{
			System.out.println(o);
			resultIDs.add(o.getID());
		}
		
		return resultIDs;
	}
}
