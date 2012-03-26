package experimentalcode.students.goldhofa.visualization.style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeMap;

import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.visualization.style.ClassStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import experimentalcode.students.goldhofa.SegmentID;
import experimentalcode.students.goldhofa.Segments;
import gnu.trove.list.array.TIntArrayList;

/**
 * Die CS Vis will also eine eigene stylingpolicy basierend auf
 * singleobjectstyling? Genau so ist das gedacht. Das erzeugt eine eigene
 * styling policy, und setzt diese dann im StylingResult als aktiv. Dann löst
 * sie ein "ResultChanged" für das StylingResult aus, und die anderen Visualizer
 * zeichnen neu
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

  // all segments
  Segments segments;

  // selection
  protected ArrayList<SegmentID> selectedSegments;

  protected ArrayList<SegmentID> unselectedSegments;

  protected ArrayModifiableDBIDs unselectedObjects;

  protected boolean changed = false;

  // unselected segments

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
  public CSStylingPolicy(Segments segments, StyleLibrary style) {
    super();

    this.segments = segments;

    // get all selectable segments
    TreeMap<SegmentID, Integer> allObjectSegments = segments.getSegments(false);
    unselectedSegments = new ArrayList<SegmentID>(allObjectSegments.size());
    unselectedObjects = DBIDUtil.newArray();
    for(SegmentID segmentID : allObjectSegments.keySet()) {
      // store segmentID
      if(!segmentID.isUnpaired()) {
        unselectedSegments.add(segmentID);
        // and store their get all objects
        unselectedObjects.addDBIDs(segments.getSegmentDBIDs(segmentID));
      }
    }

    selectedSegments = new ArrayList<SegmentID>();
  }

  public void selectObjects(SegmentID segment) {
    if(selectedSegments.contains(segment)) {
      return;
    }
    selectedSegments.add(segment);
    unselectedSegments.remove(segment);
    unselectedObjects.removeDBIDs(segments.getSegmentDBIDs(segment));
  }

  public boolean hasSegmentSelected(SegmentID segment) {
    return selectedSegments.contains(segment);
  }

  public ArrayList<SegmentID> getSelectedSegments() {
    return selectedSegments;
  }

  public void deselectObjects(SegmentID segment) {
    if(unselectedSegments.contains(segment)) {
      return;
    }
    selectedSegments.remove(segment);
    unselectedSegments.add(segment);
    unselectedObjects.addDBIDs(segments.getSegmentDBIDs(segment));
  }

  public void deselectAllObjects() {
    for(int i = 0; i < selectedSegments.size(); ++i) {
      SegmentID id = selectedSegments.get(i);
      unselectedSegments.add(id);
      unselectedObjects.addDBIDs(segments.getSegmentDBIDs(id));
    }
    selectedSegments.clear();
  }

  @Override
  public int getStyleForDBID(DBID id) {
    /*
     * System.out.println("policy: serving style"); for(int i = 0; i <
     * ids.size(); i++) { if(ids.get(i).contains(id)) { return i; } }
     */
    return -2;
  }

  @Override
  public int getColorForDBID(DBID id) {
    /*
     * System.out.println("policy: serving color"); for(int i = 0; i <
     * ids.size(); i++) { if(ids.get(i).contains(id)) { return colors.get(i); }
     * }
     */
    return 0;
  }

  @Override
  // -2=grau, -1=schwarz, 0+=farben
  public int getMinStyle() {
    return -2;
  }

  @Override
  public int getMaxStyle() {
    return selectedSegments.size();// ids.size();
  }

  @Override
  public Iterator<DBID> iterateClass(int cnum) {
    // unselected
    if(cnum == -2) {
      return unselectedObjects.iterator();
    }
    else if(cnum == -1) {
      return Collections.emptyIterator();
    }
    // colors
    return segments.getSegmentDBIDs(selectedSegments.get(cnum)).iterator();
  }
}