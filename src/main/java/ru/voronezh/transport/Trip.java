package ru.voronezh.transport;

import java.util.OptionalInt;

/**
 * Конкретный рейс по маршруту (Trip).
 *
 * Если Route — схема (например, Маршрут 9КА от Вокзала до ВГУ),
 * то Trip — реализация (например, автобус 9КА, который выехал в 08:15 по будням).
 */
public class Trip {
    private final Route route;         // Маршрут, по которому едет рейс
    private final int depMin;          // Время отправления с 1-й остановки
    private final boolean isWeekend;   // Флаг выходного дня (true = рейс выходного дня, false = будний)

    public Trip(Route route, String departureTime, boolean isWeekend) {
        this.route = route;
        // Сразу преобразуем "08:15" в число 495 минут для быстрых расчетов
        this.depMin = TimeUtils.strToTime(departureTime);
        this.isWeekend = isWeekend;
    }

    public Route getRoute() { return route; }
    public int getDepMin() { return depMin; }
    public boolean isWeekend() { return isWeekend; }

    /**
     * Вычисляет точное время прибытия на k-ю остановку маршрута.
     * Формула: (время старта с первой остановки) + (время хода до k-й остановки).
     */
    public int arrivalAtKthStop(int k) {
        return depMin + route.timeToKthStop(k);
    }

    /**
     * Вычисляет время прибытия на конкретную остановку по объекту Stop.
     * Возвращает OptionalInt:
     */
    public OptionalInt arrivalAtStop(Stop stop) {
        int idx = route.getStopIndex(stop); // Индекс от 0 до N-1
        if (idx != -1) {
            // Номер k в timeToKthStop нумеруется с 1, поэтому передаем idx + 1
            return OptionalInt.of(arrivalAtKthStop(idx + 1));
        }
        return OptionalInt.empty();
    }
}
