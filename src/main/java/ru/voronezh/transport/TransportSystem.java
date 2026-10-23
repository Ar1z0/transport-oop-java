package ru.voronezh.transport;

import java.util.*;

/**
 * Диспетчерская система управления всей транспортной сетью Воронежа.
 *
 * Хранит списки всех остановок, маршрутов и запланированных рейсов,
 * а также предоставляет главную бизнес-логику:
 * 1. Формирование расписания по конкретной остановке.
 * 2. Расчет интервалов движения транспорта.
 * 3. Поиск прямого беспересадочного рейса между пунктами A и B.
 * 4. Поиск оптимального маршрута с 1 пересадкой с учётом минимального запаса времени.
 */
public class TransportSystem {
    // Хранилище всех остановок: ключ — имя остановки, значение — объект Stop
    private final Map<String, Stop> stops = new LinkedHashMap<>();

    // Хранилище всех маршрутов: ключ — номер маршрута, значение — объект Route
    private final Map<String, Route> routes = new LinkedHashMap<>();

    // Список всех рейсов всех маршрутов города
    private final List<Trip> trips = new ArrayList<>();

    /**
     Упаковщик
     */
    public record ScheduleEntry(String time, String routeNumber, String transportType) {}


     // Содержит номер маршрута, тип транспорта, время отправления/прибытия и длительность.
    public record DirectRoute(String route, String type, String departure, String arrival, int durationMin) {
        @Override
        public String toString() {
            return String.format("%s (%s): отправление %s, прибытие %s, время в пути %d мин.",
                    route, type, departure, arrival, durationMin);
        }
    }

    /**
     * упаковщик для найденного маршрута с одной пересадкой.
     */
    public record TransferRoute(
            String trip1Route, String trip1Dep,
            String transferStop, int waitMin,
            String trip2Route, String trip2Dep,
            String finalArr, int totalDurationMin
    ) {
        @Override
        public String toString() {
            return String.format("1) %s (отпр. %s) -> пересадка на «%s» (ожидание %d мин.) -> 2) %s (отпр. %s, приб. %s). Общее время: %d мин.",
                    trip1Route, trip1Dep, transferStop, waitMin, trip2Route, trip2Dep, finalArr, totalDurationMin);
        }
    }

    /**
     * Добавляет остановку в систему по имени (если ещё не существует)
     * и возвращает существующий или новый экземпляр Stop.
     */
    public Stop addStop(String name) {
        return stops.computeIfAbsent(name, Stop::new);
    }

    /**
     * Регистрирует маршрут в системе.
     */
    public void addRoute(Route route) {
        routes.put(route.getNumber(), route);
    }

    /**
     * Добавляет запланированный рейс.
     */
    public void addTrip(Trip trip) {
        trips.add(trip);
    }

    public Map<String, Stop> getStops() {
        return Collections.unmodifiableMap(stops);
    }

    public Map<String, Route> getRoutes() {
        return Collections.unmodifiableMap(routes);
    }

    /**
     * 1. РАСПИСАНИЕ ПО ОСТАНОВКЕ:
     * Проверяет все рейсы системы, отбирает те, что идут через указанную остановку
     * в нужный день (будни/выходной), и сортирует их по времени прибытия.
     */
    public List<ScheduleEntry> getScheduleForStop(Stop stop, boolean isWeekend) {
        List<ScheduleEntry> schedule = new ArrayList<>();
        for (Trip trip : trips) {
            // Фильтруем по дню недели (будни или выходные)
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
        // Сортируем расписание по возрастанию времени прибытия
        schedule.sort(Comparator.comparing(ScheduleEntry::time));
        return schedule;
    }

    /**
     * 2. РАСЧЁТ ИНТЕРВАЛОВ ДВИЖЕНИЯ (HEADWAY):
     * пр:
     * если автобусы отправляются в 08:00, 08:15 и 08:35,
     * то интервалы составят [15, 20] минут.
     */
    public List<Integer> calculateHeadway(Route route, boolean isWeekend) {
        // Фильтруем только рейсы заданного маршрута и дня недели, сортируем по времени отправления
        List<Trip> routeTrips = trips.stream()
                .filter(t -> t.getRoute().equals(route) && t.isWeekend() == isWeekend)
                .sorted(Comparator.comparingInt(Trip::getDepMin))
                .toList();

        List<Integer> intervals = new ArrayList<>();
        // Вычисляем разницу во времени между соседними отправлениями: trips[i] - trips[i-1]
        for (int i = 1; i < routeTrips.size(); i++) {
            intervals.add(routeTrips.get(i).getDepMin() - routeTrips.get(i - 1).getDepMin());
        }
        return intervals;
    }

    /**
     * 3. ПОИСК ПРЯМЫХ РЕЙСОВ:
     * Ищет рейсы без пересадок от остановки.
     */
    public List<DirectRoute> findDirectRoutes(Stop start, Stop end, boolean isWeekend) {
        List<DirectRoute> results = new ArrayList<>();
        for (Trip trip : trips) {
            if (trip.isWeekend() != isWeekend) continue;

            OptionalInt dep = trip.arrivalAtStop(start);
            OptionalInt arr = trip.arrivalAtStop(end);

            // если транспорт проходит обе остановки и старт идёт раньше финиша
            if (dep.isPresent() && arr.isPresent() && dep.getAsInt() < arr.getAsInt()) {
                int d = dep.getAsInt();
                int a = arr.getAsInt();
                results.add(new DirectRoute(
                        trip.getRoute().getNumber(),
                        trip.getRoute().getTransportType().getTitle(),
                        TimeUtils.timeToStr(d),
                        TimeUtils.timeToStr(a),
                        a - d // длительность поездки в минутах
                ));
            }
        }
        // Сортируем результаты по минимальному времени в пути (самый быстрый рейс первым)
        results.sort(Comparator.comparingInt(DirectRoute::durationMin));
        return results;
    }

    /**
     * 4. ПОИСК МАРШРУТОВ С 1 ПЕРЕСАДКОЙ:
     * Алгоритм:
     * 1. Берём первый рейс (trip1), который останавливается на начальной остановке start.
     * 2. Смотрим все последующие остановки этого рейса (потенциальные точки пересадки: transferStop).
     * 3. Ищем второй рейс (trip2), который останавливается на transferStop и едет до целевой остановки end.
     * 4. Проверяем условие пересадки: время между прибытием trip1 и отправлением trip2
     *    должно быть не меньше заданного запаса minTransfer минут (чтобы успеть дойти до платформы).
     * 5. Сортируем все найденные варианты по минимальному общему времени в пути.
     */
    public List<TransferRoute> findTransferRoutes(Stop start, Stop end, int minTransfer, boolean isWeekend) {
        List<TransferRoute> results = new ArrayList<>();
        List<Trip> activeTrips = trips.stream()
                .filter(t -> t.isWeekend() == isWeekend)
                .toList();

        // Перебираем первый отрезок пути
        for (Trip trip1 : activeTrips) {
            OptionalInt dep1 = trip1.arrivalAtStop(start);
            if (dep1.isEmpty()) continue; // trip1 не проходит через start

            // Получаем список остановок после посадки на start
            int startIdx = trip1.getRoute().getStopIndex(start);
            List<Stop> nextStops = trip1.getRoute().getStops().subList(startIdx + 1, trip1.getRoute().getStops().size());

            // Каждую последующую остановку рассматриваем как кандидата на пересадку
            for (Stop transferStop : nextStops) {
                int arr1 = trip1.arrivalAtStop(transferStop).getAsInt(); // Время прибытия 1-го рейса на пересадку

                // Ищем второй рейс, забирающий с этой пересадки
                for (Trip trip2 : activeTrips) {
                    if (trip1 == trip2) continue; // Не пересаживаемся в тот же самый рейс

                    OptionalInt dep2 = trip2.arrivalAtStop(transferStop); // Время отправления 2-го рейса
                    OptionalInt arr2 = trip2.arrivalAtStop(end);          // Время прибытия 2-го рейса на финиш

                    // trip2 должен забирать с transferStop и ехать на end позже
                    if (dep2.isPresent() && arr2.isPresent() && dep2.getAsInt() < arr2.getAsInt()) {
                        int wait = dep2.getAsInt() - arr1; // Время ожидания на остановке пересадки
                        
                        // Пассажир успевает пересесть только если время ожидания >= minTransfer
                        if (wait >= minTransfer) {
                            results.add(new TransferRoute(
                                    trip1.getRoute().getNumber(),
                                    TimeUtils.timeToStr(dep1.getAsInt()),
                                    transferStop.getName(),
                                    wait,
                                    trip2.getRoute().getNumber(),
                                    TimeUtils.timeToStr(dep2.getAsInt()),
                                    TimeUtils.timeToStr(arr2.getAsInt()),
                                    arr2.getAsInt() - dep1.getAsInt() // Общее время поездки (включая ожидание)
                            ));
                        }
                    }
                }
            }
        }
        // Сортируем: сверху маршруты с наименьшим общим временем в дороге
        results.sort(Comparator.comparingInt(TransferRoute::totalDurationMin));
        return results;
    }
}
