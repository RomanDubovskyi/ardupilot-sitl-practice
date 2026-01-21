package org.example.plane;

import io.mavsdk.System;
import io.mavsdk.mission_raw.MissionRaw;
import io.reactivex.functions.Action;
import jakarta.validation.Valid;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class PlaneServiceImpl implements PlaneService {

  @Override
  public void takeOff(
      @Valid Plane drone,
      float takeOffAlt,
      Action onSuccess,
      Consumer<Throwable> onError
  ) {
    System mavPointer = drone.getMavPointer();

    mavPointer.getTelemetry().getPosition()
        .take(1)
        .timeout(10, TimeUnit.SECONDS)
        .flatMapCompletable(pos -> {

          List<MissionRaw.MissionItem> missionItems = List.of(
              // Item 0: Dummy Home (Required for MAVLink Sequence)
              new MissionRaw.MissionItem(
                  0, 6, 16, 0, 1, 0f, 0f, 0f, 0f,
                  (int) (pos.getLatitudeDeg() * 1E7), (int) (pos.getLongitudeDeg() * 1E7), 0f, 0
              ),
              // Item 1: NAV_TAKEOFF (Command 22)
              new MissionRaw.MissionItem(
                  1, 6, 22, 1, 1, 15.0f, 0f, 0f, 0f,
                  (int) (pos.getLatitudeDeg() * 1E7), (int) (pos.getLongitudeDeg() * 1E7),
                  takeOffAlt, 0
              ),
              // Item 2: Next Waypoint
              new MissionRaw.MissionItem(
                  2, 3, 16, 0, 1, 0f, 0f, 0f, 0f,
                  (int) ((pos.getLatitudeDeg() + 0.001) * 1E7),
                  (int) ((pos.getLongitudeDeg() + 0.001) * 1E7), takeOffAlt, 0
              ),
              new MissionRaw.MissionItem(
                  3,    // Sequence index
                  0,    // Frame (0 for global/misc commands)
                  177,  // Command: MAV_CMD_DO_JUMP
                  0,    // Current (not used)
                  1,    // Autocontinue
                  2f,   // Param 1: Jump to Waypoint #2
                  -1f,  // Param 2: Repeat count (-1 for infinite loop)
                  0f, 0f, 0, 0, 0f, 0
              )
          );

          return mavPointer.getMissionRaw().clearMission().timeout(5, TimeUnit.SECONDS)
              .andThen(mavPointer.getMissionRaw().uploadMission(missionItems)
                  .timeout(10, TimeUnit.SECONDS))
              .andThen(mavPointer.getParam().setParamFloat("TKOFF_THR_MINACC", 0.0f)
                  .timeout(3, TimeUnit.SECONDS))
              .andThen(mavPointer.getParam().setParamFloat("TKOFF_THR_MINSPD", 0.0f)
                  .timeout(3, TimeUnit.SECONDS))
              .andThen(mavPointer.getParam().setParamInt("TKOFF_THR_DELAY", 0)
                  .timeout(3, TimeUnit.SECONDS))
              .andThen(mavPointer.getAction().arm().timeout(5, TimeUnit.SECONDS))
              .delay(2, TimeUnit.SECONDS)
              .andThen(mavPointer.getMission().startMission().timeout(5, TimeUnit.SECONDS));
        })
        .subscribe(onSuccess, onError::accept);
  }

  @Override
  public void forceLand(@Valid Plane drone, Action onSuccess, Consumer<Throwable> onError) {
    System mavPointer = drone.getMavPointer();
    mavPointer.getAction().land().subscribe(onSuccess, onError::accept);
  }
}
