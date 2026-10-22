package ru.voronezh.transport;

import java.util.List;
import java.util.OptionalInt;

/**
 * Класс Маршрута.
 * Описывает траекторию движения транспорта:
 * - Уникальный номер/название (number)
 * - Вид транспорта (transportType)
 * - Упорядоченный список остановок (stops)
 * - Время хода между соседними остановками (times)
 *   times[0] = время пути от A до B
 *   times[1] = время пути от B до C
 *   times[2] = время пути от C до D
 *   (поэтому размер списка times всегда ровно на 1 меньше размера списка stops).
 */
public class Route {
    private final String number;
    private final TransportType transportType;
    private final List<Stop> stops;
    private final List<Integer> times;

    public Route(String number, TransportType transportType, List<Stop> stops, List<Integer> times) {
        // маршрут не может состоять из 0 или 1 остановки
        if (stops == null || stops.size() < 2) {
            throw new IllegalArgumentException("Маршрут должен состоять минимум из двух остановок.");
        }
        // между N остановками должно быть ровно (N - 1) перегонов
        if (times == null || times.size() != stops.size() - 1) {
            throw new IllegalArgumentException("Количество интервалов хода должно быть ровно на 1 меньше числа остановок.");
        }
        this.number = number;
        this.transportType = transportType;
        // List.copyOf создаёт неизменяемуюкопию списка, чтобы никто снаружи не мог изменить остановки или интервалы после создания маршрута
        this.stops = List.copyOf(stops);
        this.times = List.copyOf(times);
    }

    public String getNumber() { return number; }
    public TransportType getTransportType() { return transportType; }
    public List<Stop> getStops() { return stops; }
    public List<Integer> getTimes() { return times; }

    //Возвращает порядковый индекс остановки в маршруте (начиная с 0). Если остановка не входит в маршрут,
    // возвращает -1.
    public int getStopIndex(Stop stop) {
        return stops.indexOf(stop);
    }

    /**
     * Вычисляет суммарное время в минутах от начала маршрута (1-я остановка)
     * до k-й остановки по счёту (нумерация остановок для пользователя: от 1 до N).
     * Пр:
     * Остановки: [A, B, C], интервалы: [10, 5]
     * - для 1-й остановки (A): timeToKthStop(1) -> 0 мин (старт)
     * - для 2-й остановки (B): timeToKthStop(2) -> 10 мин
     * - для 3-й остановки (C): timeToKthStop(3) -> 10 + 5 = 15 мин
     */
    public int timeToKthStop(int k) {
        if (k < 1 || k > stops.size()) {
            throw new IndexOutOfBoundsException("Некорректный номер остановки k: " + k);
        }
        int sum = 0;
        // Складываем времена всех перегонов до k-й остановки (от 0 до k-2)
        for (int i = 0; i < k - 1; i++) {
            sum += times.get(i);
        }
        return sum;
    }

    /**
     * Находит время пути между двумя произвольными остановками маршрута.
     * Возвращает OptionalInt:
     * - OptionalInt.of(минуты), если start идет раньше end на маршруте
     * - OptionalInt.empty(), если остановки нет или движение идет в противоположную сторону
     */
    public OptionalInt travelTimeBetween(Stop start, Stop end) {
        int idx1 = stops.indexOf(start);
        int idx2 = stops.indexOf(end);

        // Проверяем: обе остановки найдены и начальная расположена строго ДО конечной
        if (idx1 != -1 && idx2 != -1 && idx1 < idx2) {
            int sum = 0;
            // Суммируем интервалы между остановками от idx1 до idx2
            for (int i = idx1; i < idx2; i++) {
                sum += times.get(i);
            }
            return OptionalInt.of(sum);
        }
        return OptionalInt.empty();
    }
}
