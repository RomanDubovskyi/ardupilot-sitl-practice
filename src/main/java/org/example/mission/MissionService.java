package org.example.mission;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.example.waypoints.WayPoint;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class MissionService {

  private final ResourceLoader resourceLoader;
  private final JsonMapper jsonMapper;

  public Mission createDefaultMission() {
    Mission defaultMission = new Mission();
    defaultMission.setId("Mission-mock-id");
    defaultMission.setWps(readDefaultWpsFromResources());

    return defaultMission;
  }

  @SneakyThrows
  private List<WayPoint> readDefaultWpsFromResources() {
    var defaultMission = resourceLoader.getResource("classpath:missions/default_mission_wps.json");
    return jsonMapper.readValue(
        defaultMission.getContentAsByteArray(),
        new TypeReference<List<WayPoint>>() {
        }
    );
  }
}
