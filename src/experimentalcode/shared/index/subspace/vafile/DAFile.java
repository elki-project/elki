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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import experimentalcode.franz.utils.ArrayUtils;


/**
 * DAFile
 * 
 * @author Thomas Bernecker
 * @created 22.09.2009
 * @date 22.09.2009
 */
public class DAFile
{
	private int dimension;

	private double[] borders;
	private double[] lookup;

	private int selectivityCoeff;

	private static int p = 2;

	double[] maxDists, minDists;

	
	public DAFile(int dimension)
	{
		this.dimension = dimension;
		selectivityCoeff = -1;
	}


	public void setPartitions(Collection<DoubleVector> objects, int partitions)
	{
		long start = System.currentTimeMillis();

		borders = new double[partitions + 1];
		int[] partitionCount = new int[partitions];

		int size = objects.size();
		int remaining = size;
		double[] tempdata = new double[size];
		int j = 0;
		for (DoubleVector dv : objects)
			tempdata[j++] = dv.doubleValue(dimension + 1);
		Arrays.sort(tempdata);
		tempdata = ArrayUtils.unique(tempdata, 1 / (100 * partitions));

		int bucketSize = (int) (size / (double) partitions);
		int i = 0;
		for (int b = 0; b < partitionCount.length; b++)
		{
			borders[b] = tempdata[i];
			remaining -= bucketSize;
			i += bucketSize;

			// test: are there remaining objects that have to be put in the
			// first buckets?
			if (remaining > (bucketSize * (partitionCount.length - b - 1)))
			{
				i++;
				remaining--;
				partitionCount[b]++;
			}

			partitionCount[b] += bucketSize;
		}
		borders[partitions] = tempdata[size - 1] + 0.000001; // make sure that
															 // last object will
															 // be included

		System.out.println("dimension " + dimension + " finished! (time: "
		        + (System.currentTimeMillis() - start) + " ms)");
		
		assert borders != null : "borders are null";
	}


	public void setPartitions(double[] borders)
	{
		this.borders = borders;
	}


	/**
	 * @return the borders
	 */
	public double[] getBorders()
	{
		return borders;
	}


	public double[] getMinDists(int queryCell)
	{
    return minDists;
	}


	public double[] getMaxDists(int queryCell)
	{
		return maxDists;
	}


	public double getMaxMaxDist(int queryCell)
	{
                return ArrayUtils.maxValue(getMaxDists(queryCell));
//		double[] maxDists = getMaxDists(queryCell);
//		double result = Double.NEGATIVE_INFINITY;
//		for (int i = 0; i < maxDists.length; i++)
//		{
//			result = Math.max(result, maxDists[i]);
//		}
//		return result;
	}


	public void setLookupTable(DoubleVector query)
	{
		int bordercount = borders.length;
		lookup = new double[bordercount];
		for (int i = 0; i < bordercount; i++)
		{
			lookup[i] = Math.pow(borders[i] - query.doubleValue(dimension + 1), p);
		}
		
		int queryCellGlobal = -1;
		for(int i = 0; i< borders.length;i++){
		  if(query.doubleValue(dimension+1) < borders[i])
		    break;
		  else
		    queryCellGlobal++;
		}
		//maxdists
		maxDists = new double[borders.length - 1];
    for (int i = 0; i < maxDists.length; i++)
    {
      if (i < queryCellGlobal)
        maxDists[i] = lookup[i];
      else if (i > queryCellGlobal)
        maxDists[i] = lookup[i + 1];
      else
        maxDists[i] = Math.max(lookup[i], lookup[i + 1]);
    }
    
    //mindists
    minDists = new double[borders.length - 1];
    for (int i = 0; i < minDists.length; i++)
    {
      if (i < queryCellGlobal)
        minDists[i] = lookup[i + 1];
      else if (i > queryCellGlobal)
        minDists[i] = lookup[i];
      else
        minDists[i] = 0;
    }
		
	}


	/**
	 * @return the dimension
	 */
	public int getDimension()
	{
		return dimension;
	}


	/**
	 * @param dimension the dimension to set
	 */
	public void setDimension(int dimension)
	{
		this.dimension = dimension;
	}


	/**
	 * @return the selectivityCoeff
	 */
	public int getSelectivityCoeff()
	{
		return selectivityCoeff;
	}


	public void setSelectivityCoeff(int val)
	{
		selectivityCoeff = val;
	}


	public int getIOCosts()
	{
		return borders.length * 8 + 4;
	}


	/**
	 * @param selectivityCoeff the selectivityCoeff to set
	 */
	public static void calculateSelectivityCoeffs(List<DAFile> daFileList,
	        DoubleVector query, double epsilon)
	{
		DAFile[] daFiles = new DAFile[daFileList.size()];
		for (DAFile da : daFileList)
			daFiles[da.getDimension()] = da;

		int dimensions = query.getDimensionality();
		double[] lowerVals = new double[dimensions];
		double[] upperVals = new double[dimensions];

		VectorApprox queryApprox = new VectorApprox(query.getID(), dimensions);
		queryApprox.calculateApproximation(query, daFiles);

		for (int i = 0; i < dimensions; i++)
		{
			lowerVals[i] = query.doubleValue(i + 1) - epsilon;
			upperVals[i] = query.doubleValue(i + 1) + epsilon;
		}

		DoubleVector lowerEpsilon = new DoubleVector(lowerVals);
		VectorApprox lowerEpsilonPartitions = new VectorApprox(null, dimensions);
		lowerEpsilonPartitions.calculateApproximation(lowerEpsilon, daFiles);

		DoubleVector upperEpsilon = new DoubleVector(upperVals);
		VectorApprox upperEpsilonPartitions = new VectorApprox(null, dimensions);
		upperEpsilonPartitions.calculateApproximation(upperEpsilon, daFiles);

		for (int i = 0; i< daFiles.length;i++)
		{
			int coeff = (queryApprox.getApproximation(i) - lowerEpsilonPartitions
			        .getApproximation(i))
			        + (upperEpsilonPartitions.getApproximation(i) - queryApprox
			                .getApproximation(i)) + 1;
			daFiles[i].setSelectivityCoeff(coeff);
		}
	}


	public static List<DAFile> sortBySelectivity(List<DAFile> daFiles)
	{
		Collections.sort(daFiles, new DAFileSelectivityComparator());
		return daFiles;
	}

}


class DAFileSelectivityComparator implements Comparator<DAFile>
{

	/*
	 * (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(DAFile a, DAFile b)
	{
		return Double.compare(a.getSelectivityCoeff(), b.getSelectivityCoeff());
	}

}
