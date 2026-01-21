package org.example.plane;

import io.reactivex.functions.Action;
import jakarta.validation.Valid;
import java.util.function.Consumer;

public interface PlaneService {

  void takeOff(@Valid Plane drone, float takeOffAlt, Action onSuccess, Consumer<Throwable> onError);
}
