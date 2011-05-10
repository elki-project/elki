package experimentalcode.frankenb.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import experimentalcode.frankenb.model.DistanceList;


/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class DistanceListTest {

  @Test
  public void moreThenKTest() {
    DistanceList distanceList = new DistanceList(0, 3);
    distanceList.addDistance(0, 0.0);
    distanceList.addDistance(1, 1.0);
    distanceList.addDistance(2, 2.0);
    distanceList.addDistance(3, 2.0);
    
    assertEquals(4, distanceList.getSize());
  }
  
  @Test
  public void kTest() {
    DistanceList distanceList = new DistanceList(0, 3);
    distanceList.addDistance(0, 0.0);
    distanceList.addDistance(4, 5.0);
    distanceList.addDistance(1, 1.0);
    distanceList.addDistance(2, 2.0);
    distanceList.addDistance(3, 4.0);
    
    assertEquals(3, distanceList.getSize());
  }
  
  @Test
  public void lessThenKTest() {
    DistanceList distanceList = new DistanceList(0, 3);
    distanceList.addDistance(0, 0.0);
    distanceList.addDistance(2, 2.0);
    
    assertEquals(2, distanceList.getSize());
  }  
}
