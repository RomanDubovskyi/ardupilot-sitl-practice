package org.example.plane;

import io.mavsdk.System;
import io.mavsdk.info.Info.Identification;
import org.springframework.stereotype.Component;

@Component
public class ArduPilotMavLinkPlaneInitializer implements PlaneInitializer {
  private static System plane;

  @Override
  public Plane initMavlinkSitlPlane() {
    System drone = getPlaneMavLink();
    Plane plane = new Plane();
    Identification identification = drone.getInfo().getIdentification().blockingGet();
    plane.setId(identification.getHardwareUid());
    plane.setMavPointer(drone);

    return plane;
  }

  private System getPlaneMavLink() {
    if (plane == null) {
      plane = new System();
    }

    return plane;
  }
}
