package org.example.plane;

import io.mavsdk.System;
import jakarta.validation.constraints.NotNull;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Plane {

  @ToString.Include
  @EqualsAndHashCode.Include
  private String id;
  @NotNull
  private System mavPointer;

  public void postCurrentPosAndFlightMode(int millis) {
    if (mavPointer == null) {
      throw new IllegalStateException("No valid connection through mavlink, can't post data");
    }

    mavPointer.getTelemetry().getPosition().throttleLatest(millis, TimeUnit.MILLISECONDS)
        .subscribe((pos) -> {
          java.lang.System.out.printf(
              "Current pos: Lat=%.6f; Lon=%.6f Alt=%.1fm%n",
              pos.getLatitudeDeg(),
              pos.getLongitudeDeg(),
              pos.getRelativeAltitudeM());
        });

    mavPointer.getTelemetry().getFlightMode().throttleLatest(millis, TimeUnit.MILLISECONDS)
        .subscribe((mode) -> {
          java.lang.System.out.printf("Current FlightMode is %s", mode);
        });
  }
}

