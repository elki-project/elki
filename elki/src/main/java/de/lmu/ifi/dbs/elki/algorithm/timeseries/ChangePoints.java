package de.lmu.ifi.dbs.elki.algorithm.timeseries;

import java.util.List;

public class ChangePoints {

    List<ChangePoint> points;
    String label;

    public ChangePoints(List<ChangePoint> points, String label){
        this.points = points;
        this.label = label;
    }

    public StringBuilder appendTo(StringBuilder buf) {
        buf.append(label).append(": ");
        for (ChangePoint pnt : points) {
            pnt.appendTo(buf);
            buf.append(",");
        }
        return buf.deleteCharAt(buf.length()-1);
    }
}
