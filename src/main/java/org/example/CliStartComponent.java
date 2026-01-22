package org.example;

import lombok.RequiredArgsConstructor;
import org.example.mission.MissionService;
import org.example.plane.Plane;
import org.example.plane.PlaneInitializer;
import org.example.plane.PlaneMissionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CliStartComponent implements CommandLineRunner {

  private final PlaneInitializer planeInitializer;
  private final PlaneMissionService planeMissionService;
  private final MissionService missionService;

  @Override
  public void run(String... args) throws Exception {
    Plane plane = planeInitializer.initMavlinkSitlPlane();
    plane.setActiveMission(missionService.createDefaultMission());

    plane.postCurrentPosAndFlightMode(1000);
    planeMissionService.startMission(
        plane,
        () -> System.out.println("Mission start command was sent successfully"),
        (e) -> System.err.println("Can't start the mission, error occurred: " + e.getMessage())
    );
  }
}
