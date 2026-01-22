package org.example.plane;

import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

public interface PlaneMissionService {

  void startMission(Plane plane, Action onSuccess, Consumer<Throwable> onError);
}
