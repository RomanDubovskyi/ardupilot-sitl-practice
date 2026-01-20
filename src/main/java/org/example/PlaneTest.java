package org.example;


import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class PlaneTest {

  public static void main(String[] args) {
    var drone = new io.mavsdk.System();
    Scanner scanner = new Scanner(System.in);

    System.out.println("========== Plane commander =========");
    System.out.println("============ Waiting for connection ============");

    drone.getTelemetry().getFlightMode()
        .take(1)
        .subscribe((mode) -> {
          System.out.println("Connection ok!, current flight mode is " + mode);
        });

    drone.getTelemetry().getPosition()
        .throttleLatest(5, TimeUnit.SECONDS)
        .subscribe(pos -> {
          System.out.printf(
              "Curr pos: Lat=%.6f ; Lon=%.6f ; Alt=%.1fm%n",
              pos.getLatitudeDeg(),
              pos.getLongitudeDeg(),
              pos.getRelativeAltitudeM()
          );
        });

    while (true) {
      System.out.println("\nCommands: ");
      System.out.println("1. ARM");
      System.out.println("2. TAKEOFF");
      System.out.println("3. FLY TO POINT ");
      System.out.println("4. FLY to Saved point");
      System.out.println("5. LAND");
      System.out.println("6. EXIT");
      System.out.print("Choose: ");

      int choice = scanner.nextInt();

      switch (choice) {
        case 1 -> arm(drone);
        case 2 -> takeoff(drone);
        case 3 -> flyToPoint(drone, scanner);
        case 4 -> flyeToSavedPoint(drone);
        case 5 -> land(drone);
        case 6 -> {
          System.out.println("Exiting...");
          System.exit(0);
        }
      }
    }
  }

  private static void flyeToSavedPoint(io.mavsdk.System drone) {
    System.out.println("Flying to saved point");
    drone.getAction().gotoLocation(
        -35.36038425,
        149.15558299,
        800.0f,
        0.0f
    ).subscribe(
        () -> System.out.println("sent fly to saved point command successfully!"),
        error -> System.err.println("ERROR: shit happened: " + error.getMessage())
    );
  }

  private static void arm(io.mavsdk.System drone) {
    System.out.println("Arming plane");
    drone.getAction().arm()
        .subscribe(
            () -> System.out.println("Armed successfully!"),
            error -> System.err.println("ERROR: shit happened: " + error.getMessage())
        );
  }

  private static void takeoff(io.mavsdk.System drone) {
    System.out.println("Taking off to 50m Alt");
    drone.getAction().setTakeoffAltitude(50.0f)
        .andThen(drone.getAction().takeoff())
        .subscribe(
            () -> System.out.println("Takeoff command sent"),
            (error) -> System.err.println("Couldn't execute takeoff command: " + error.getMessage())
        );
  }


  private static void flyToPoint(io.mavsdk.System drone, Scanner scanner) {
    System.out.println("Enter lat: ");
    double lat = scanner.nextDouble();
    System.out.println("Enter long: ");
    double lon = scanner.nextDouble();
    System.out.println("Enter Alt: ");
    float alt = scanner.nextFloat();

    System.out.printf(
        "Sending command to fly to: Lan: %.6f, Long: %.6f, Alt: %.1fm %n", lat, lon, alt
    );
    drone.getAction().gotoLocation(lat, lon, alt, 0.f)
        .subscribe(
            () -> System.out.println("Successfully sent the command to fly to location"),
            (error) -> System.err.println("Couldn't execute takeoff command: " + error.getMessage())
        );
  }

  private static void land(io.mavsdk.System drone) {
    System.out.println("Landing...");
    drone.getAction().land()
        .subscribe(
            () -> System.out.println("✓ Landing!"),
            error -> System.err.println("✗ Land failed: " + error.getMessage())
        );
  }
}
