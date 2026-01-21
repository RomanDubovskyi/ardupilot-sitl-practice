package org.example;

import lombok.RequiredArgsConstructor;
import org.example.plane.Plane;
import org.example.plane.PlaneInitializer;
import org.example.plane.PlaneService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CliStartComponent implements CommandLineRunner {

  private final PlaneInitializer planeInitializer;
  private final PlaneService planeService;

  @Override
  public void run(String... args) throws Exception {
    Plane plane = planeInitializer.initMavlinkSitlPlane();
    plane.postCurrentPosAndFlightMode(5000);
    planeService.takeOff(
        plane,
        500.0f,
        () -> System.out.println("Takeoff command has been sent successfully"),
        (err) -> System.err.println("Error during takeoff command dispatch: " + err.getMessage())
    );
  }
}
