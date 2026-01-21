package org.example.waypoints;

import lombok.Data;

@Data
public class WayPoint {

  private int order;
  private double lat;
  private double lon;
  private float alt;
}
