package de.lmu.ifi.dbs.elki.algorithm.projection;


import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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



public class tSNE<O> extends AbstractDistanceBasedAlgorithm<O, Relation<DoubleVector>> {
  
  int iteration;
  static double perplexity;
  static int learning_rate;
  static double momentum;
  private int dim = 2;
  private static int size;
  private static final Logging LOG = Logging.getLogger(tSNE.class);
  
  public tSNE(DistanceFunction<? super O> distanceFunction, double [][] points, 
                double momentum, int learning_rate, int iteration, double perplexity) {
    super(distanceFunction);
    this.iteration = iteration;
    this.perplexity = perplexity;
    this.learning_rate = learning_rate;
    this.momentum = momentum;
    
  }
  
  public Relation<DoubleVector> run(Relation<O> relation){
    DistanceQuery<O> dq = relation.getDistanceQuery(getDistanceFunction());
    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    DBIDArrayIter ix = ids.iter(), iy = ids.iter();
    size = ids.size();
    double [] [] pij = new double [size] [size];
    double [][] squared_distances = new double[size][size];
    initialize_pijs(pij, dq, ix, iy, squared_distances);
    symmetrize(pij);
    
    WritableDataStore<DoubleVector> proj = DataStoreFactory.FACTORY.makeStorage(ids, DataStoreFactory.HINT_DB | DataStoreFactory.HINT_SORTED, DoubleVector.class);
    VectorFieldTypeInformation<DoubleVector> otype = new VectorFieldTypeInformation<>(DoubleVector.FACTORY, dim);
    Random r = new Random();
    for(ix.seek(0);ix.valid();ix.advance()){ //oder 10^-4?
      double [] v = new double[]{r.nextGaussian()*1e-2, r.nextGaussian()*1e-2};
      DoubleVector dv = new DoubleVector(v);
      proj.put(ix, dv);
    }
    
    double [][] qij = new double[size][size];
    double [][] gradient = new double [size][dim];
    double z = compute_qij_distance_sum(squared_distances);
    double[][] solution_2_steps_ago = new double[size][dim];
    for(int i = 0;i<iteration;i++){
      compute_qij(squared_distances,qij);
      compute_gradient(pij, qij, gradient, proj, ix, iy,z);
      update_solution(proj, ix,  gradient,i,solution_2_steps_ago);
    }
    
//    // Beim >Result: it.seek(off)
//    for (ix.seek(0); ix.valid(); ix.advance()){
//      
////      // Proizierten Vektor berechnen
////      double[] darray = new double[]{Math.random(), Math.random()};
////      DoubleVector dv = DoubleVector.copy(darray /* TODO */);
////      proj.put(ix, dv);
//    }
    
    return new MaterializedRelation<>("tSNE", "t-SNE", otype, proj, ids);
  }
  



  //protected und <O>,Wozu bei Agnes static ??
  protected static <O> void initialize_pijs(double[][] pij, DistanceQuery<O> dq, DBIDArrayIter ix, DBIDArrayIter iy, double[][] squared_distances) {
    double log2_perp = Math.log(perplexity)/Math.log(2.0);
    double error = 1e-5;
    double difference;
    computeDistances(squared_distances, dq, ix,iy);
    for(ix.seek(0);ix.valid();ix.advance()){
      int pos = 0;
      int tries = 0;
      double squaredSigma = 1.0;
      computePij(squaredSigma, squared_distances, pij,pos);
      double h = computeH(pij,pos);
      difference = h -log2_perp;
      double last = 0.0;
      while(tries<50 && Math.abs(difference) > error){
        double help = squaredSigma;
        if(difference > 0){
          squaredSigma = (squaredSigma +last)/2.0;
        }
        else{
          squaredSigma = squaredSigma * 2.0;
        }
        last = help;
        computePij(squaredSigma, squared_distances, pij,pos);
        h = computeH(pij, pos);
        difference = h - log2_perp; //je größer sigma, desto größer perplexity?
        tries++;
      }
      pos++;
    }
  }
  


  protected static <O> double computeH(double[][] pij, int point) {
    double h = 0.0;
    for(int j = 0;j<pij.length;j++){
      h = pij[point][j] * Math.log(pij[point][j]) / Math.log(2);
    }
    return -h;
  }

  protected static <O> void computeDistances(double[][] squared_distances, DistanceQuery<O> dq, DBIDArrayIter ix, DBIDArrayIter iy) {
    int pos_1 = 0;
    for(ix.seek(0);ix.valid();ix.advance()){
      int pos_2 = 0;
      for(iy.seek(0);iy.getOffset()<ix.getOffset();iy.advance()){ //double default 0.0
        double dist = dq.distance(ix, iy);
        dist = dist*dist;
        squared_distances[pos_1][pos_2] = dist;
        squared_distances[pos_2][pos_1] = dist;
        pos_2++;
      }
      pos_1++;
    }
  }
  
  //schreibt pi|j'S zeilenweise in die nicht symmetrische Matrix
  protected static <O> void computePij(double squaredSigma, double[][] squared_distances, double[][] pij, int point) {
    double sum = computeDistanceSum(squared_distances, point, squaredSigma);
    for(int i = 0;i<size;i++){
      if(i == point)
         pij[point][i] = 0.0;
      else
        pij[point][i] = Math.exp(-squared_distances[point][i]/(2*squaredSigma))/sum;
    }
  }
  
//  private static int[] getIndicesForSpecificPoint(double[] triangle, int i) {
//    ArrayList<Integer> indices = new ArrayList<>();
//    int start = triangleSize(i);
//    for(int j = 0;j<=i-1 ;j++) //waagrechte Einträge
//      indices.add(start+j);
//    
//    return null;
//  }

  //Berechnet Nenner der pijs, Call by value/call by refernce mit iy?
  protected static <O> double computeDistanceSum(double [][] squaredDistanceMatrix, int row, double squaredSigma){
    double sum = 0.0;
    for(int j = 0; j < squaredDistanceMatrix.length; j++) {
      if(row!=j)
        sum = sum + Math.exp(-squaredDistanceMatrix[row][j]/(2*squaredSigma));
    }
    return sum;
  }

  //Überschreibt pi|j Matrix mit symmetrischer pij Matrix
  protected static <O> void symmetrize(double[][] pij) {
    for(int i = 0;i<pij.length;i++){
      for(int j = 0;j<i;j++){
        double p = pij[i][j];
        double q = pij[j][i];
        double n = (p+q)/(2.0*size);
        pij[i][j] = n;
        pij[j][i] = n;
      }
    }
  }
  
  protected static <O> void compute_qij(double[][] squared_distances, double[][] qij) {
    double sum = compute_qij_distance_sum(squared_distances);
    for(int i = 0;i<qij.length;i++){
      for(int j = 0; j<i;j++ ){
        double s = (1/(1+squared_distances[i][j]))/(sum);
        qij[i][j] = s;
        qij[j][i] = s;
      }
    }
  }
  
  protected static <O> double compute_qij_distance_sum(double[][] squared_distances) {
    double sum = 0.0;
    for(int i = 0;i<squared_distances.length;i++){
      for(int j = 0;j<i;j++){
          sum = sum + 1/(1+squared_distances[i][j]);
          sum = sum + 1/(1+squared_distances[j][i]); //wegen Symmetrie
      }
    }
    return sum;
  }
  
  //Im Gradienten-Array stehen die Vektoren zeilenweise
  protected static <O> void compute_gradient(double[][] pij, double[][] qij, double[][] gradient, WritableDataStore<DoubleVector> proj, DBIDArrayIter ix, DBIDArrayIter iy, double z) {
    for(ix.seek(0);ix.valid();ix.advance()){
      int row = 0;
      double product = 0.0;
      for(iy.seek(0);iy.valid();iy.advance()){
        int entry = 0;
       // if(ix.getOffset()!=iy.getOffset()){ notwendig!
        double[] difference = minus(proj.get(ix).toArray(), proj.get(iy).toArray());
        product = product + (pij[row][entry]-qij[row][entry])*qij[row][entry]*z*difference[entry];
        gradient[row][entry] = 4.0 * product;
        entry++;
      }
      row++;
    }
  }
  
  protected static <O> void update_solution(WritableDataStore<DoubleVector> proj, DBIDArrayIter ix, double[][] gradient, int i, double[][] solution_2_steps_ago) {
    int pos = 0;
    if(i==0){
      for(ix.seek(0);ix.valid();ix.advance()){
        solution_2_steps_ago[pos] = proj.get(ix).toArray().clone();
        double [] solution = proj.get(ix).toArray().clone();
        double [] new_solution = plus(solution,mal(learning_rate, gradient[pos])).clone();
        proj.delete(ix);
        proj.put(ix, new DoubleVector(new_solution));
        pos++;
      }
    }
    
    else{
      for(ix.seek(0);ix.valid();ix.advance()){
        solution_2_steps_ago[pos] = proj.get(ix).toArray().clone();
        double [] solution = proj.get(ix).toArray().clone();
        double [] new_solution = plus(solution,mal(learning_rate, gradient[pos])).clone();
        double [] difference = minus(proj.get(ix).toArray(), solution_2_steps_ago[pos]);
        new_solution = plus(new_solution, mal(momentum, difference)).clone();
        proj.delete(ix);
        proj.put(ix, new DoubleVector(new_solution));
        pos++;
      }
    }
  }
  
  
//  protected static int triangleSize(int size) { //Ohne Diagonalen
//    return (size*(size-1))>>>1;
//  }

  protected static <O> double[] minus(double[] array, double[] array2) {
    double [] diff = new double[array.length];
    for(int i = 0;i<array.length;i++){
      diff[i] = array[i]-array2[i];
    }
    return diff;
  }
  
  protected static <O> double[] plus(double[] array, double[] array2) {
    double [] sum = new double[array.length];
    for(int i = 0;i<array.length;i++){
      sum[i] = array[i]+array2[i];
    }
    return sum;
  }
  
  protected static <O> double[] mal(double v, double [] vec){
    double [] m = new double[vec.length];
    for(int i = 0; i < m.length; i++) {
      m[i] = v * vec[i];
    }
    return m;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }
  
  

  @Override
  protected Logging getLogger() {
    return LOG;
  }
  
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O>{
    
    double momentum;
    double perplexity;
    int learning_rate;
    int iteration;
    double [][] points; 
    public static final OptionID MOMENTUM_ID = new OptionID("tSNE.momentum", "The value for the momentum");
    public static final OptionID LEARNING_RATE_ID = new OptionID("tSNE.learning_rate", "");
    public static final OptionID ITERATION_ID = new OptionID("tSNE.iteration", "");
    public static final OptionID PERPLEXITY_ID = new OptionID("tSNE.perplexity", "");
    
    @Override
    protected void makeOptions(Parameterization config) { 
      //super.makeOptions(config);
      ObjectParameter<DistanceFunction<O>> distanceFunctionP = makeParameterDistanceFunction(SquaredEuclideanDistanceFunction.class, DistanceFunction.class);
      if(config.grab(distanceFunctionP)){
        distanceFunction = distanceFunctionP.instantiateClass(config);
      }
      
      DoubleParameter p_momentum = new DoubleParameter(MOMENTUM_ID).setDefaultValue(0.8);
      if(config.grab(p_momentum)){
        momentum = p_momentum.getValue();
      }
      
      DoubleParameter p_perplexity = new DoubleParameter(PERPLEXITY_ID).setDefaultValue(40.0);
      if(config.grab(p_perplexity))
        perplexity = p_perplexity.getValue();
      
      IntParameter p_learning_rate = new IntParameter(LEARNING_RATE_ID).setDefaultValue(100);
      if(config.grab(p_learning_rate))
        learning_rate = p_learning_rate.getValue();
      
      IntParameter p_iteration = new IntParameter(ITERATION_ID).setDefaultValue(100);
      if(config.grab(p_iteration))
        iteration = p_iteration.getValue();
      
    }
    
    @Override
    protected tSNE<O> makeInstance() {
      return new tSNE<>(distanceFunction, points, momentum, learning_rate, iteration, perplexity);
    }
    
  }

}
