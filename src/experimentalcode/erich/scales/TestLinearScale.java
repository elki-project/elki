package experimentalcode.erich.scales;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestLinearScale {

  @Test
  public final void testLinearScale() {
    LinearScale a = new LinearScale(3,97);
    assertEquals("Minimum for scale 3-97 not as expected.", 0.0, a.getMin(), Double.MIN_VALUE);
    assertEquals("Maximum for scale 3-97 not as expected.", 100.0, a.getMax(), Double.MIN_VALUE);
    
    LinearScale b = new LinearScale(-97, -3);
    assertEquals("Minimum for scale -97 : -3 not as expected.", -100.0, b.getMin(), Double.MIN_VALUE);
    assertEquals("Maximum for scale -97 : -3 not as expected.", 0.0, b.getMax(), Double.MIN_VALUE);

    LinearScale c = new LinearScale(-3, 37);
    assertEquals("Minimum for scale -3 : 37 not as expected.", -10.0, c.getMin(), Double.MIN_VALUE);
    assertEquals("Maximum for scale -3 : 37 not as expected.", 40.0, c.getMax(), Double.MIN_VALUE);
  
    LinearScale d = new LinearScale(-37, 3);
    assertEquals("Minimum for scale -37 : 3 not as expected.", -40.0, d.getMin(), Double.MIN_VALUE);
    assertEquals("Maximum for scale -37 : 3 not as expected.", 10.0, d.getMax(), Double.MIN_VALUE);
  }

}
