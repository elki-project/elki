package de.lmu.ifi.dbs.elki.algorithm.timeseries;

import de.lmu.ifi.dbs.elki.data.LabelList;

import java.util.List;

public class ChangePoints {

    List<ChangePoint> points;

    public ChangePoints(List<ChangePoint> points){
        this.points = points;
    }

    public StringBuilder appendTo(StringBuilder buf, LabelList labels) {
        buf.append(labels.toString()).append(": ");
        for (ChangePoint pnt : points) {
            pnt.appendTo(buf);
            buf.append(",");
        }
        return buf.deleteCharAt(buf.length()-1);
    }
}
