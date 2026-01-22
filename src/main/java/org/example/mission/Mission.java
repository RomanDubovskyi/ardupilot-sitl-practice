package org.example.mission;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.example.waypoints.WayPoint;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Mission {

  private String id;
  private List<WayPoint> wps = new ArrayList<>();
}
