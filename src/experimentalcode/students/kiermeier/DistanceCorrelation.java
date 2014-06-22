package experimentalcode.students.kiermeier;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

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

/**
 * Distance correlation
 * 
 * @author Marie Kiermeier
 *
 */
public class DistanceCorrelation implements Algorithm{
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(DistanceCorrelation.class);
  
  /**
   * Constructor.
   */
  public DistanceCorrelation(){
    super();
  }
  
  /**
   * Computes the distance variance matrix of one axis.
   * 
   * @param dMatrix distance matrix of the axis
   * @param n number of points at the axis
   */
  private double[][] computeDVarMatrix(double [][] dMatrix, int n){
    double[][] result = new double[n][n];
    double[] rowSum = new double[n];
    double[] columnSum = new double[n];
    double matrixSum = .0;
    // row sum
    for(int i = 0; i < n; i++){
      for(int j = 0; j < n; j++){
        rowSum[i] = rowSum[i] + dMatrix[i][j];
      }
    }
    // column sum
    for(int k = 0; k < n; k++){
      for(int l = 0; l < n; l++){
        columnSum[k] = columnSum[k] + dMatrix[l][k];
      }
    }
    // total sum
    for(int m = 0; m < n; m++){
      matrixSum = matrixSum + rowSum[m];
    }
    
    for(int o = 0; o < n; o++){
      for(int p = 0; p < n; p++){
         result[o][p] = dMatrix[o][p] - 1/(double)n*rowSum[o] - 1/(double)n*columnSum[p] + (1/Math.pow(n, 2))*matrixSum; 
      }
    }
    return result;
    } 
  
  /**
   * 
   * Computes the distance covariance for two axis.
   * Can also be used to compute the distance variance of one axis
   * (dVarMatrixA = dVarMatrixB). 
   * 
   * @param dVarMatrixA distance variance matrix of the first axis
   * @param dVarMatrixB distance variance matrix of the second axis
   * @param n number of points
   * @return
   */
  private double computeDCovar(double[][] dVarMatrixA, double[][] dVarMatrixB, int n){
    double result = .0;
    for(int i = 0; i < n; i++){
      for(int j = 0; j < n; j++){
        result = result + dVarMatrixA[i][j] * dVarMatrixB[i][j];
      }
    }
    return (1/Math.pow(n, 2)) * result;
  }

  
  @Override
  public Result run(Database database) {
  
    Relation<? extends NumberVector> rel = database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    DBIDs dbids = rel.getDBIDs();
    ArrayDBIDs ids = DBIDUtil.ensureArray(dbids);
    
    int n = ids.size();
    DBIDArrayIter iter1 = ids.iter();
    DBIDArrayIter iter2 = ids.iter();
    
    // distance matrices (euclidean norm)
    double[][] dMatrixA = new double[n][n];
    double[][] dMatrixB = new double[n][n];
    for (iter1.seek(0); iter1.valid(); iter1.advance()){
      for (iter2.seek(0);iter2.valid();iter2.advance()){
        dMatrixA[iter1.getOffset()][iter2.getOffset()] = Math.sqrt(Math.pow(rel.get(iter1).doubleValue(0),2)+Math.pow(rel.get(iter2).doubleValue(0),2));
        dMatrixB[iter1.getOffset()][iter2.getOffset()] = Math.sqrt(Math.pow(rel.get(iter1).doubleValue(1),2)+Math.pow(rel.get(iter2).doubleValue(1),2));
      }
    }
    
    // distance variance matrices
    double[][] dVarMatrixA =  computeDVarMatrix(dMatrixA, n);
    double[][] dVarMatrixB =  computeDVarMatrix(dMatrixB, n);
    
    // distance variance
    double dVarA = computeDCovar(dVarMatrixA, dVarMatrixA, n);
    double dVarB = computeDCovar(dVarMatrixB, dVarMatrixB, n);
    LOG.verbose("dVarA: " + dVarA);
    LOG.verbose("dVarB: " + dVarB);

    // distance covariance
    double dCovar = computeDCovar(dVarMatrixA, dVarMatrixB, n);
    LOG.verbose("dCovar: " + dCovar);

    // distance correlation
    double dCor = .0;
    if(dVarA*dVarB > 0){
      dCor = dCovar/Math.sqrt(dVarA*dVarB);
    }
    LOG.verbose("dCor: " + dCor);
    return null;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  
  /**
   * 
   * Parameterization class
   * 
   * @author Marie Kiermeier
   *
   */
  public static class Parameterizer extends AbstractParameterizer{

    @Override
    protected Object makeInstance() {
      return new DistanceCorrelation();
  }
}
}
