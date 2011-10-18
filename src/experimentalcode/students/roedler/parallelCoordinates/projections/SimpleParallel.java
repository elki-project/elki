package experimentalcode.students.roedler.parallelCoordinates.projections;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.visualization.projections.AbstractProjection;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;

public class SimpleParallel extends AbstractProjection implements ProjectionParallel {
  
  /**
   * Number of dimensions
   */
  final protected int dims;
  
  /**
   * margin
   */
  final double[] margin;
  
  /**
   * space between two axis
   */
  double dist;
  
  /**
   * viewbox size
   */
  final double[] size;
  
  /**
   * axis size
   */
  final double axisHeight;
  
  /**
   * visible dimensions
   */
  int visDims;
  
  /**
   * which dimensions are visible
   */
  boolean[] isVisible;
  
  /**
   * dimension order
   */
  int[] dimOrder;
  
  /**
   * Constructor.
   * 
   * @param scales Scales to use
   */
  
  public SimpleParallel(LinearScale[] scales, int dims, double[] margin, double[] size, double axisHeight){
    super(scales);
    this.dims = dims;
    dist = (size[0] - 2 * margin[0]) / (double)(dims - 1);
    this.margin = margin;
    this.size = size;
    this.axisHeight = axisHeight;
    this.visDims = dims;
    isVisible = new boolean[dims];
    for (int i = 0; i < isVisible.length; i++){
      isVisible[i] = true;
    }
    dimOrder = new int[dims];
    for (int i = 0; i < dimOrder.length; i++){
      dimOrder[i] = i;
    }
  }
  
  public int getFirstVisibleDimension(){
    for (int i = 0; i < dims; i++){
      if (isVisible(dimOrder[i])){
        return i;
      }
    }
    return 0;
  }
  
  public double getSizeX(){
    return size[0];
  }
  
  public double getSizeY(){
    return size[1];
  }
  
  public int getVisibleDimensions(){
    return visDims;
  }
  
  private void calcAxisPositions(){
    dist = (size[0] - 2 * margin[0]) / (double)(dims -(dims - (visDims - 1)));
  }
  
  @Override
  public LinearScale getScale(int d) {
    return scales[d];
  }
  
  public double getAxisHeight(){
    return axisHeight;
  }
  
  public double getDist(){
    return dist;
  }
  
  public double getXpos(int dim){
    int notvis = 0;
    if (isVisible[dimOrder[dim]] == false){ return -1.0; }
    for(int i = 0; i < dim; i++){
      if(isVisible[dimOrder[i]] == false){
        notvis++;
      }
    }
    return margin[0] + (dim - notvis) * dist;
  }
  
  public boolean isVisible(int dim){
    return isVisible[dimOrder[dim]];
  }
  
  public double getMarginX(){
    return margin[0];
  }
  
  public double getMarginY(){
    return margin[1];
  }
  
  public void setVisible(boolean vis, int dim){
    isVisible[dimOrder[dim]] = vis;
    if (vis == false) {
      visDims--;
    }
    else {
      visDims++;
    }
    calcAxisPositions();
  }

  @Override
  public Vector projectScaledToRender(Vector v) {
    Vector ret = new Vector(v.getDimensionality());
    //ret.set(1, v.get(1));
    for (int i = 0; i < v.getDimensionality(); i++) {
      ret.set(i, (axisHeight + margin[1]) - v.get(i) * axisHeight);
    }
    return sortDims(ret);
  }

  @Override
  public Vector projectRenderToScaled(Vector v) {
    Vector ret = new Vector(v.getDimensionality());
    for (int i = 0; i < v.getDimensionality(); i++){
      ret.set(i, ((v.get(i) - margin[1]) + axisHeight) / axisHeight);
    }
    return sortDims(ret);
  }

  @Override
  public Vector projectRelativeScaledToRender(Vector v) {
    Vector ret = new Vector(v.getDimensionality());
    for (int i = 0; i < v.getDimensionality(); i++){
      ret.set(i, -v.get(i) * axisHeight);
    }
    return sortDims(ret);
  }

  @Override
  public Vector projectRelativeRenderToScaled(Vector v) {
    Vector ret = new Vector(v.getDimensionality());
    for (int i = 0; i < v.getDimensionality(); i++){
      ret.set(i, v.get(i) / axisHeight);
    }
    return sortDims(ret);
  }

/*  @Override
  public Vector projectDataToScaledSpace(NumberVector<?, ?> data) {
    return null;
  }

  @Override
  public Vector projectDataToScaledSpace(Vector data) {
    return null;
  }

  @Override
  public Vector projectRelativeDataToScaledSpace(NumberVector<?, ?> data) {
    return null;
  }

  @Override
  public Vector projectRelativeDataToScaledSpace(Vector data) {
    return null;
  } */

  @Override
  public Vector projectDataToRenderSpace(NumberVector<?, ?> data) {
    return projectScaledToRender(projectDataToScaledSpace(data));
  }

  @Override
  public Vector projectDataToRenderSpace(Vector data) {
    return projectScaledToRender(projectDataToScaledSpace(data));
  }

/*  @Override
  public <NV extends NumberVector<NV, ?>> NV projectScaledToDataSpace(Vector v, NV factory) {
    return null;
  }*/

  @Override
  public <NV extends NumberVector<NV, ?>> NV projectRenderToDataSpace(Vector v, NV prototype) {
    final int dim = v.getDimensionality();
    Vector vec = projectRenderToScaled(v);
    double[] ds = vec.getArrayRef();
    // Not calling {@link #projectScaledToDataSpace} to avoid extra copy of
    // vector.
    for(int d = 0; d < dim; d++) {
      ds[d] = scales[d].getUnscaled(ds[d]);
    }
    return prototype.newInstance(vec.getArrayRef());
  }

  @Override
  public Vector projectRelativeDataToRenderSpace(NumberVector<?, ?> data) {
    return projectRelativeScaledToRender(projectRelativeDataToScaledSpace(data));
  }

  @Override
  public Vector projectRelativeDataToRenderSpace(Vector data) {
    return projectRelativeScaledToRender(projectRelativeDataToScaledSpace(data));
  }

/*  @Override
  public <NV extends NumberVector<NV, ?>> NV projectRelativeScaledToDataSpace(Vector v, NV prototype) {
    return null;
  }*/

  @Override
  public <NV extends NumberVector<NV, ?>> NV projectRelativeRenderToDataSpace(Vector v, NV prototype) {
    final int dim = v.getDimensionality();
    Vector vec = projectRelativeRenderToScaled(v);
    double[] ds = vec.getArrayRef();
    // Not calling {@link #projectScaledToDataSpace} to avoid extra copy of
    // vector.
    for(int d = 0; d < dim; d++) {
      ds[d] = scales[d].getRelativeUnscaled(ds[d]);
    }
    return prototype.newInstance(vec.getArrayRef());
  }

  @Override
  public double projectDimension(int dim, double value) {
    double temp = scales[dimOrder[dim]].getScaled(value);
    return (axisHeight + margin[1]) - temp * axisHeight;
  }

  @Override
  public int getLastVisibleDimension() {
    for (int i = (isVisible.length - 1); i >= 0; i--){
      if (isVisible[dimOrder[i]] == true){ return i; }
    }
    return -1;
  }
  
  @Override
  public int getLastVisibleDimension(int dim) {
    for (int i = dim - 1; i >= 0; i--){
      if (isVisible[dimOrder[i]] == true){ return i; }
    }
    return -1;
  }

  @Override
  public void swapDimensions(int a, int b) {
    int temp = dimOrder[a];
    dimOrder[a] = dimOrder[b];
    dimOrder[b] = temp;
    
  }

  @Override
  public void shiftDimension(int dim, int rn) {
    if (dim > rn){
      int temp = dimOrder[dim];
      
      for (int i = dim; i > rn; i--){
        dimOrder[i] = dimOrder[i - 1];
      }
      dimOrder[rn] = temp;
    }
    else {
      int temp = dimOrder[dim];
     
      for (int i = dim; i < rn - 1; i++){
        dimOrder[i] = dimOrder[i + 1];
      }
      dimOrder[rn - 1] = temp;
    }
  }

  @Override
  public int getDimensionNumber(int pos) {
    return dimOrder[pos];
  } 
  
  public Vector sortDims(Vector s){
    Vector ret = new Vector(s.getDimensionality());
    for (int i = 0; i < s.getDimensionality(); i++){
      ret.set(i, s.get(dimOrder[i]));
    }
    return ret;
  }

  @Override
  public int getNextVisibleDimension(int dim) {
    for (int i = dim + 1; i < dims; i++){
      if (isVisible[dimOrder[i]] == true){
        return i;
      }
    }
    return dim;
  }
  
}
