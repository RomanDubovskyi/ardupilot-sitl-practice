package org.example.plane;

import io.mavsdk.System;
import io.mavsdk.mission_raw.MissionRaw.MissionItem;
import io.mavsdk.telemetry.Telemetry.Position;
import io.reactivex.functions.Action;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.example.waypoints.WayPoint;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutoPilotPlaneMissionService implements PlaneMissionService {

  // TODO: find out if this kind of constants are already present in the lib
  private static final int MAV_FRAME_GLOBAL_RELATIVE_ALT_INT = 6;
  private static final int MAV_CMD_NAV_TAKEOFF = 22;
  private static final int MAV_CMD_NAV_LAND = 21;
  private static final int MAV_CMD_NAV_WAYPOINT = 16;

  @Override
  public void startMission(
      Plane plane,
      Action onSuccess,
      io.reactivex.functions.Consumer<Throwable> onError
  ) {
    Objects.requireNonNull(plane.getActiveMission(),
        "AutoPilotMode requires active mission to be present");
    List<MissionItem> missionItems = new ArrayList<>();
    missionItems.add(initHomePointFromPlane(plane));
    plane.getActiveMission().getWps().forEach(parseWpToMissionItem(plane, missionItems));
    System mavPointer = plane.getMavPointer();

    mavPointer.getMissionRaw().clearMission()
        .andThen(mavPointer.getMissionRaw().uploadMission(missionItems))
        .andThen(mavPointer.getParam().setParamFloat("TKOFF_THR_MINACC", 0.0f))
        .andThen(mavPointer.getParam().setParamFloat("TKOFF_THR_MINSPD", 0.0f))
        .andThen(mavPointer.getParam().setParamInt("TKOFF_THR_DELAY", 0))
        .delay(1, TimeUnit.SECONDS)
        .andThen(mavPointer.getAction().arm())
        .andThen(mavPointer.getMissionRaw().startMission())
        .subscribe(onSuccess, onError);
  }

  private MissionItem initHomePointFromPlane(Plane plane) {
    Position pos = plane.getMavPointer().getTelemetry().getPosition().blockingFirst();
    return new MissionItem(
        0,
        MAV_FRAME_GLOBAL_RELATIVE_ALT_INT,
        MAV_CMD_NAV_WAYPOINT,
        0,
        1,
        0.0f,
        0.0f,
        0.0f,
        0.0f,
        (int) (pos.getLatitudeDeg() * 1e7),
        (int) (pos.getLongitudeDeg() * 1e7),
        0.0f,
        0
    );
  }

  private static Consumer<WayPoint> parseWpToMissionItem(
      Plane plane,
      List<MissionItem> missionItems
  ) {

    return (wp) -> {
      // index 0 is for the home point
      int commandCode = wp.getOrder() == 1
          ? MAV_CMD_NAV_TAKEOFF
          : wp.getOrder() == plane.getActiveMission().getWps().size()
              ? MAV_CMD_NAV_LAND : MAV_CMD_NAV_WAYPOINT;

      MissionItem missionItem = new MissionItem(
          wp.getOrder(),
          MAV_FRAME_GLOBAL_RELATIVE_ALT_INT,
          commandCode,
          // The current code to execute is the takeoff for now,
          // later we can improve and pick curr dynamically
          commandCode == MAV_CMD_NAV_TAKEOFF ? 1 : 0,
          1,
          // For takeoff this param would be min pitch, ignore for other wp
          commandCode == MAV_CMD_NAV_TAKEOFF ? 15.0f : 0.0f,
          0.0f,
          0.0f,
          0.0f,
          (int) (wp.getLat() * 1e7),
          (int) (wp.getLon() * 1e7),
          wp.getAlt(),
          0
      );

      missionItems.add(missionItem);
    };
  }
}
