package experimentalcode.students.roedler.utils.convexhull;

import java.lang.Math;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;


/**
 * 
 * @author Robert Rödler, 
 * a good portion of this code is taken from Computational Geometry in C, second Edition, by Joseph O'Rourke
 * and his website: http://maven.smith.edu/~orourke/  
 *
 */
public class ConvexHull2D {
  
  private static final int X = 0;
  private static final int Y = 1;
  
  private Vector[] points;
  private VertexList list, top;
  private int ndelete = 0;
  private int i = 0;
  
  public ConvexHull2D (Vector[] points){
    if (points.length < 3) { return; }
    this.points = points;
    list = new VertexList();
    boolean toSmall = false;
    double maxX, maxY;
    double factor = 1.0;
    maxX = Math.abs(points[0].get(X));
    maxY = Math.abs(points[0].get(Y));
    
    for (i = 0; i < points.length; i++){
      if (maxX < Math.abs(points[i].get(X))){maxX = Math.abs(points[i].get(X));}
      if (maxY < Math.abs(points[i].get(Y))){maxY = Math.abs(points[i].get(Y));}
    }
    if (maxX < 10.0 || maxY < 10.0){
      toSmall = true;
      factor = 10 / maxX;
      if (10 / maxY > factor){factor = 10 / maxY;}
    }
    
    for (i = 0; i < points.length; i++) {
      Vertex v = list.makeNullVertex();
      if (!toSmall) {
        v.v[X] = points[i].get(X);
        v.v[Y] = points[i].get(Y);
      }
      else {
        v.v[X] = points[i].get(X) * factor;
        v.v[Y] = points[i].get(Y) * factor;
      }
      v.vnum = i;
    }
  }
  
  public ConvexHull2D (VertexList list){
    this.list = list;
  }
  
  public void clearHull(){
    top = new VertexList();
  }
  
  public Vector[] start() {
    if (list == null) { return null; }
    VertexList res = makeHull();
    Vertex it = res.head;
    
    Vector[] resv = new Vector[res.n];
    for (int i = 0; i < resv.length; i++) {
 //     resv[i] = it.getVector2D();
      resv[i] = points[it.vnum];
      it = it.next;
    }
    return resv;
  }
  
  public VertexList makeHull(){
    Vertex v = new Vertex();
    v = list.head;
    for (i = 0; i < list.n; i++){
      v.vnum = i;
      v = v.next;
    }
    
    findLowest();
    qSort(list);
    if (ndelete > 0) { squash(); }
    top = Graham();
    return top;
  }
  
  private VertexList Graham() {
    int i;
    
    top = new VertexList();
    Vertex v1 = new Vertex(list.head.v[0], list.head.v[1]);
    v1.vnum = list.head.vnum;
    v1.mark = list.head.mark;
    
    Vertex v2 = new Vertex(list.head.next.v[0], list.head.next.v[1]);
    v2.vnum = list.head.next.vnum;
    v2.mark = list.head.next.mark;
    
    Push (v1, top);
    Push (v2, top);
    
    i = 2;
    
    while ( i < list.n) {
      Vertex v3 = new Vertex(list.getElement(i).v[0], list.getElement(i).v[1]);
      v3.mark = list.getElement(i).mark;
      v3.vnum = list.getElement(i).vnum;
      
      if (v1.left(top.head.prev.v, top.head.prev.prev.v, v3.v)){
        Push (v3, top);
        i++;
      }
      else {
        if (top.n > 2) {
          Pop(top);
        }
      }
    }
    return top;
  }
  
  private void squash() {
    Vertex v = new Vertex();
    v = list.head;
    for (i = 0; i < list.n; i++) {
      if (v.mark) {
        list.delete(v);
      }
      v = v.next;
    }
  }
  
  private void sort(VertexList a, int lo0, int hi0) {
    if (lo0 >= hi0) {
      return;
    }
    Vertex mid = new Vertex();
    mid = a.getElement(hi0);
    int lo = lo0;
    int hi = hi0 - 1;
    
    while (lo <= hi) {
      while (lo <= hi && ((compare(a.getElement(lo), mid) == 1) || (compare(a.getElement(lo), mid) == 0))) {
        lo++;
      }
      while (lo <= hi && ((compare(a.getElement(hi), mid) == -1) || (compare(a.getElement(hi), mid) == 0))) {
        hi--;
      }
      
      if (lo < hi) {
        swap(a.getElement(lo), a.getElement(hi));
      }
    }
    swap(a.getElement(lo), a.getElement(hi0));
    sort(a, lo0, lo - 1);
    sort(a, lo + 1, hi0);
  }
  
  private void qSort(VertexList a) {
    sort(a, 1, a.n -1);
  }
  
  private int compare(Vertex tpi, Vertex tpj) {
    int a; 
    double x, y;
    Vertex pi, pj;
    pi = tpi;
    pj = tpj;
    
    Vertex myhead = new Vertex();
    myhead = list.head;
    
    a = myhead.areaSign(myhead.v, pi.v, pj.v);
    
    if (a > 0) { return -1; }
    else if (a < 0) { return 1; }
    else {
      x = Math.abs(pi.v[0] - list.head.v[0]) - Math.abs(pj.v[0] - list.head.v[0]);
      y = Math.abs(pi.v[1] - list.head.v[1]) - Math.abs(pj.v[1] - list.head.v[1]);
      ndelete++;
      
      if ((x < 0) || (y < 0)) {
        pi.mark = true;
        return -1;
      }
      else if ((x > 0) || (y > 0)) {
        pj.mark = true;
        return 1;
      }
      else {
        if (pi.vnum > pj.vnum) {
          pj.mark = true;
        }
        else {
          pi.mark = true;
        }
        return 0;
      }
    }
  }
  
  private void findLowest() {
    int i;
    
    Vertex v1 = list.head.next;
    
    for (i = 1; i < list.n; i++) {
      if ((list.head.v[1] < v1.v[1]) || ((v1.v[1] == list.head.v[1]) && (v1.v[0] > list.head.v[0]))) {
        swap(list.head, v1);
      }
      v1 = v1.next;
    }
  }
  
  private void swap (Vertex first, Vertex second) {
    Vertex temp = new Vertex(first.v[0], first.v[1]);
    temp.vnum = first.vnum;
    temp.mark = first.mark;
    
    list.resetVertex(first, second.v[0], second.v[1], second.vnum, second.mark);
    list.resetVertex(second, temp.v[0], temp.v[1], temp.vnum, temp.mark);
  }
    
  private void Push (Vertex p, VertexList top) {
    top.insertBeforeHead(p);
  }
  
  private void Pop(VertexList top) {
    top.delete(top.head.prev);
  }
}
