package ru.voronezh.transport;

import java.util.*;

/**
 * Диспетчерская система управления всей транспортной сетью Воронежа.
 */
public class TransportSystem {
    private final Map<String, Stop> stops = new LinkedHashMap<>();
    private final Map<String, Route> routes = new LinkedHashMap<>();
    private final List<Trip> trips = new ArrayList<>();

    public record ScheduleEntry(String time, String routeNumber, String transportType) {}

    public record DirectRoute(String route, String type, String departure, String arrival, int durationMin) {}

    public record TransferRoute(
            String trip1Route, String trip1Dep,
            String transferStop, int waitMin,
            String trip2Route, String trip2Dep,
            String finalArr, int totalDurationMin
    ) {}

    public Stop addStop(String name) {
        return stops.computeIfAbsent(name, Stop::new);
    }

    public void addRoute(Route route) {
        routes.put(route.getNumber(), route);
    }

    public void addTrip(Trip trip) {
        trips.add(trip);
    }

    public Map<String, Stop> getStops() {
        return Collections.unmodifiableMap(stops);
    }

    public Map<String, Route> getRoutes() {
        return Collections.unmodifiableMap(routes);
    }

    public List<ScheduleEntry> getScheduleForStop(Stop stop, boolean isWeekend) {
        List<ScheduleEntry> schedule = new ArrayList<>();
        for (Trip trip : trips) {
            if (trip.isWeekend() == isWeekend) {
                OptionalInt arr = trip.arrivalAtStop(stop);
                if (arr.isPresent()) {
                    schedule.add(new ScheduleEntry(
                            TimeUtils.timeToStr(arr.getAsInt()),
                            trip.getRoute().getNumber(),
                            trip.getRoute().getTransportType().getTitle()
                    ));
                }
            }
        }
        schedule.sort(Comparator.comparing(ScheduleEntry::time));
        return schedule;
    }

    public List<Integer> calculateHeadway(Route route, boolean isWeekend) {
        List<Trip> routeTrips = trips.stream()
                .filter(t -> t.getRoute().equals(route) && t.isWeekend() == isWeekend)
                .sorted(Comparator.comparingInt(Trip::getDepMin))
                .toList();

        List<Integer> intervals = new ArrayList<>();
        for (int i = 1; i < routeTrips.size(); i++) {
            intervals.add(routeTrips.get(i).getDepMin() - routeTrips.get(i - 1).getDepMin());
        }
        return intervals;
    }

    public List<DirectRoute> findDirectRoutes(Stop start, Stop end, boolean isWeekend) {
        List<DirectRoute> results = new ArrayList<>();
        for (Trip trip : trips) {
            if (trip.isWeekend() != isWeekend) continue;

            OptionalInt dep = trip.arrivalAtStop(start);
            OptionalInt arr = trip.arrivalAtStop(end);

            if (dep.isPresent() && arr.isPresent() && dep.getAsInt() < arr.getAsInt()) {
                int d = dep.getAsInt();
                int a = arr.getAsInt();
                results.add(new DirectRoute(
                        trip.getRoute().getNumber(),
                        trip.getRoute().getTransportType().getTitle(),
                        TimeUtils.timeToStr(d),
                        TimeUtils.timeToStr(a),
                        a - d
                ));
            }
        }
        results.sort(Comparator.comparingInt(DirectRoute::durationMin));
        return results;
    }

    public List<TransferRoute> findTransferRoutes(Stop start, Stop end, int minTransfer, boolean isWeekend) {
        List<TransferRoute> results = new ArrayList<>();
        List<Trip> activeTrips = trips.stream()
                .filter(t -> t.isWeekend() == isWeekend)
                .toList();

        for (Trip trip1 : activeTrips) {
            OptionalInt dep1 = trip1.arrivalAtStop(start);
            if (dep1.isEmpty()) continue;

            int startIdx = trip1.getRoute().getStopIndex(start);
            List<Stop> nextStops = trip1.getRoute().getStops().subList(startIdx + 1, trip1.getRoute().getStops().size());

            for (Stop transferStop : nextStops) {
                int arr1 = trip1.arrivalAtStop(transferStop).getAsInt();

                for (Trip trip2 : activeTrips) {
                    if (trip1 == trip2) continue;

                    OptionalInt dep2 = trip2.arrivalAtStop(transferStop);
                    OptionalInt arr2 = trip2.arrivalAtStop(end);

                    if (dep2.isPresent() && arr2.isPresent() && dep2.getAsInt() < arr2.getAsInt()) {
                        int wait = dep2.getAsInt() - arr1;
                        if (wait >= minTransfer) {
                            results.add(new TransferRoute(
                                    trip1.getRoute().getNumber(),
                                    TimeUtils.timeToStr(dep1.getAsInt()),
                                    transferStop.getName(),
                                    wait,
                                    trip2.getRoute().getNumber(),
                                    TimeUtils.timeToStr(dep2.getAsInt()),
                                    TimeUtils.timeToStr(arr2.getAsInt()),
                                    arr2.getAsInt() - dep1.getAsInt()
                            ));
                        }
                    }
                }
            }
        }
        results.sort(Comparator.comparingInt(TransferRoute::totalDurationMin));
        return results;
    }
}
