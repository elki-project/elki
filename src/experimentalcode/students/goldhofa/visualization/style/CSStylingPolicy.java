package experimentalcode.students.goldhofa.visualization.style;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.ClassStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import experimentalcode.students.goldhofa.ClusteringComparisonResult;
import experimentalcode.students.goldhofa.SegmentID;
import experimentalcode.students.goldhofa.Segments;

/** 
 * Die CS Vis will also eine eigene stylingpolicy basierend auf singleobjectstyling?
 * Genau so ist das gedacht. Das erzeugt eine eigene styling policy, und setzt diese dann im StylingResult als aktiv.
 * Dann löst sie ein "ResultChanged" für das StylingResult aus, und die anderen Visualizer zeichnen neu
 * 
 * @author Sascha Goldhofer
 *
 */
public class CSStylingPolicy implements ClassStylingPolicy {
  
  // color : integer
  TreeMap<Integer, Integer> colorToIndex;
  // unselected objects
  ArrayModifiableDBIDs unselected;
  ArrayList<ArrayModifiableDBIDs> selected;
  
  /**
   * Object IDs
   */
  ArrayList<DBIDs> ids;

  /**
   * Colors
   */
  TIntArrayList colors;
  
  /**
   * Constructor.
   * 
   * @param clustering Clustering to use.
   */
  public CSStylingPolicy(Clustering<?> clustering, StyleLibrary style, Segments segments) {
    super();
    
    
    int segmentSize = segments.getSegments(false).size();
    System.out.println("segment count: "+segmentSize);
    
    unselected    = DBIDUtil.newArray();
    selected      = new ArrayList<ArrayModifiableDBIDs>(segmentSize);
    
    for (int i=0; i<segmentSize; ++i) {
      selected.add(DBIDUtil.newArray());
    }
    
    //TreeMap<SegmentID, ArrayModifiableDBIDs> segmentToIDs = segments.getSegmentDBIDs();

    ColorLibrary colorset = style.getColorSet(StyleLibrary.PLOT);
    List<? extends Cluster<?>> clusters = clustering.getAllClusters();
    ids = new ArrayList<DBIDs>(clusters.size());
    colors = new TIntArrayList(clusters.size());

    Iterator<? extends Cluster<?>> ci = clusters.iterator();
    for(int i = 0;; i++) {

      Cluster<?> c = ci.next();
      
      unselected.addDBIDs(DBIDUtil.ensureSet(c.getIDs()));
      
      ids.add(DBIDUtil.ensureSet(c.getIDs()));
      colors.add(SVGUtil.stringToColor(colorset.getColor(i)).getRGB());
      
      if(!ci.hasNext()) {
        break;
      }
    }
  }
  
  public void deselectObject(DBID id, int index) {
    //System.out.println("removing object "+id.toString());
    this.selected.get(index).remove(id);
    this.unselected.add(id);
  }
  
  public void selectObject(DBID id, int index) {
    this.selected.get(index).add(id);
    this.unselected.remove(id);
  }
  
  public void setSelectedDBIDs(TreeMap<String, DBIDs> objectSelection) {}
  
/*
  @Override
  public int getColorForDBID(DBID id) {
    
    if (this.selection.containsKey(id)) {
      System.out.println("CSStylingPolicy distributing color: "+this.selection.get(id));
      return this.selection.get(id);
    }
    System.out.println("CSStylingPolicy distributing default color");
    return 0;
  }
*/

  @Override
  public int getStyleForDBID(DBID id) {
    System.out.println("policy: serving style");
    for(int i = 0; i < ids.size(); i++) {
      if(ids.get(i).contains(id)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public int getColorForDBID(DBID id) {
    System.out.println("policy: serving color");
    for(int i = 0; i < ids.size(); i++) {
      if(ids.get(i).contains(id)) {
        return colors.get(i);
      }
    }
    return 0;
  }

  @Override
  //-2=grau, -1=schwarz, 0+=farben
  public int getMinStyle() {
    System.out.println("policy: serving min-style");
    return -1;
  }

  @Override
  public int getMaxStyle() {
    return selected.size();//ids.size();
  }

  @Override
  public Iterator<DBID> iterateClass(int cnum) {
    
    // unselected
    if (cnum == -1) return unselected.iterator();
    // else colors
    return selected.get(cnum).iterator();
  }
}
