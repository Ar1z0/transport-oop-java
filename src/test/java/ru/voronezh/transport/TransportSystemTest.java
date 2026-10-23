package ru.voronezh.transport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Модульные тесты (Unit-тесты) на базе JUnit 5.
 * Покрывают все основные требования учебного задания по ООП:
 * - Утилиты времени (TimeUtils)
 * - Свойства Enum (TransportType)
 * - Валидацию маршрута (Route)
 * - Расчёт прибытия на остановку (Trip)
 * - Расписание, расчет интервалов и поиск прямых/пересадочных маршрутов (TransportSystem)
 */
public class TransportSystemTest {

    private Stop stopStation;
    private Stop stopLenina;
    private Stop stopVsu;
    private Stop stopSlava;
    private Route route1;
    private Route route2;
    private TransportSystem system;

    /**
     * Выполняется перед каждым тестом (@BeforeEach), гарантируя чистые данные.
     */
    @BeforeEach
    void setUp() {
        stopStation = new Stop("Вокзал Воронеж-1");
        stopLenina = new Stop("Площадь Ленина");
        stopVsu = new Stop("ВГУ");
        stopSlava = new Stop("Памятник Славы");

        // route1: Вокзал -> (15 мин) -> Площадь Ленина -> (5 мин) -> ВГУ
        route1 = new Route("Автобус 9КА", TransportType.BUS,
                List.of(stopStation, stopLenina, stopVsu), List.of(15, 5));

        // route2: Площадь Ленина -> (20 мин) -> Памятник Славы
        route2 = new Route("Троллейбус 7", TransportType.TROLLEYBUS,
                List.of(stopLenina, stopSlava), List.of(20));

        system = new TransportSystem();
        system.addRoute(route1);
        system.addRoute(route2);
    }

    @Test
    void test01_strToTime() {
        // Проверка перевода строкового времени в минуты: 8*60 + 30 = 510
        assertEquals(510, TimeUtils.strToTime("08:30"));
        assertEquals(0, TimeUtils.strToTime("00:00"));
    }

    @Test
    void test02_timeToStr() {
        // Проверка обратного перевода минут в строку формата ЧЧ:ММ
        assertEquals("08:30", TimeUtils.timeToStr(510));
        assertEquals("23:59", TimeUtils.timeToStr(1439));
    }

    @Test
    void test03_transportTypeProperties() {
        // Проверка характеристик видов транспорта (скорость, вместимость)
        assertEquals(24, TransportType.BUS.getSpeed());
        assertEquals(85, TransportType.BUS.getPlaces());
        assertEquals(95, TransportType.TROLLEYBUS.getPlaces());
        assertEquals(40, TransportType.BUS2.getPlaces());
    }

    @Test
    void test04_routeInvalidTravelTimes() {
        // Если остановок 2, интервал должен быть ровно 1. Передача 2 интервалов должна бросить исключение.
        assertThrows(IllegalArgumentException.class, () ->
                new Route("Err", TransportType.BUS, List.of(stopStation, stopLenina), List.of(5, 10)));
    }

    @Test
    void test05_routeTooFewStops() {
        // Маршрут из 1 остановки недопустим
        assertThrows(IllegalArgumentException.class, () ->
                new Route("Err", TransportType.BUS, List.of(stopStation), List.of()));
    }

    @Test
    void test06_timeToKthStop() {
        // До 1-й остановки: 0 мин
        assertEquals(0, route1.timeToKthStop(1));
        // До 2-й: 15 мин
        assertEquals(15, route1.timeToKthStop(2));
        // До 3-й: 15 + 5 = 20 мин
        assertEquals(20, route1.timeToKthStop(3));
    }

    @Test
    void test07_timeToInvalidK() {
        // Запрос остановки с номером 0 или больше количества остановок (4) должен бросить IndexOutOfBoundsException
        assertThrows(IndexOutOfBoundsException.class, () -> route1.timeToKthStop(0));
        assertThrows(IndexOutOfBoundsException.class, () -> route1.timeToKthStop(4));
    }

    @Test
    void test08_tripArrivalAtKthStop() {
        // Рейс стартовал в 08:00 (480 мин). До 3-й остановки 20 мин хода -> прибытие в 500 мин (08:20)
        Trip trip = new Trip(route1, "08:00", false);
        assertEquals(500, trip.arrivalAtKthStop(3));
    }

    @Test
    void test09_tripArrivalAtStop() {
        Trip trip = new Trip(route1, "10:00", false);
        // Прибытие на Площадь Ленина через 15 мин -> 10:15
        assertEquals(TimeUtils.strToTime("10:15"), trip.arrivalAtStop(stopLenina).orElse(-1));
        // Памятник Славы отсутствует на этом маршруте -> Optional.empty
        assertTrue(trip.arrivalAtStop(stopSlava).isEmpty());
    }

    @Test
    void test10_scheduleWeekday() {
        system.addTrip(new Trip(route1, "08:00", false));
        system.addTrip(new Trip(route1, "08:30", false));
        var sched = system.getScheduleForStop(stopLenina, false);
        // Ожидаем 2 рейса, отсортированных по времени: 08:15 и 08:45
        assertEquals(2, sched.size());
        assertEquals("08:15", sched.get(0).time());
        assertEquals("08:45", sched.get(1).time());
    }

    @Test
    void test11_scheduleWeekendSeparation() {
        // Проверка разделения будних и выходных рейсов
        system.addTrip(new Trip(route1, "08:00", false));
        system.addTrip(new Trip(route1, "09:00", true));

        var wk = system.getScheduleForStop(stopStation, true);
        var bd = system.getScheduleForStop(stopStation, false);
        assertEquals(1, wk.size());
        assertEquals("09:00", wk.get(0).time());
        assertEquals(1, bd.size());
        assertEquals("08:00", bd.get(0).time());
    }

    @Test
    void test12_calculateHeadway() {
        // Рейсы в 08:00, 08:15, 08:35 -> интервалы 15 мин и 20 мин
        system.addTrip(new Trip(route1, "08:00", false));
        system.addTrip(new Trip(route1, "08:15", false));
        system.addTrip(new Trip(route1, "08:35", false));
        assertEquals(List.of(15, 20), system.calculateHeadway(route1, false));
    }

    @Test
    void test13_directRouteFound() {
        // Прямой рейс: Вокзал -> ВГУ (отправление 08:00, прибытие 08:20, в пути 20 мин)
        system.addTrip(new Trip(route1, "08:00", false));
        var direct = system.findDirectRoutes(stopStation, stopVsu, false);
        assertEquals(1, direct.size());
        assertEquals(20, direct.get(0).durationMin());
        assertEquals("08:00", direct.get(0).departure());
        assertEquals("08:20", direct.get(0).arrival());
    }

    @Test
    void test14_directRouteNone() {
        // Попытка поехать в обратную сторону: ВГУ -> Вокзал (маршрут односторонний, прямых рейсов нет)
        system.addTrip(new Trip(route1, "08:00", false));
        var direct = system.findDirectRoutes(stopVsu, stopStation, false);
        assertEquals(0, direct.size());
    }

    @Test
    void test15_transferRouteSuccess() {
        // Поездка с пересадкой с route1 на route2:
        // trip1 (route1): отпр с Вокзала 08:00 -> приб на пл. Ленина 08:15
        // trip2 (route2): отпр с пл. Ленина 08:25 -> приб на Памятник Славы 08:45
        // Ожидание: 08:25 - 08:15 = 10 мин (>= 5 мин) -> маршрут валиден, общее время 45 мин
        system.addTrip(new Trip(route1, "08:00", false));
        system.addTrip(new Trip(route2, "08:25", false));

        var transfers = system.findTransferRoutes(stopStation, stopSlava, 5, false);
        assertEquals(1, transfers.size());
        var best = transfers.get(0);
        assertEquals("Площадь Ленина", best.transferStop());
        assertEquals(10, best.waitMin());
        assertEquals(45, best.totalDurationMin());
    }

    @Test
    void test16_transferRouteInsufficientTime() {
        // Ожидание 08:18 - 08:15 = 3 мин. Требуется минимум 5 мин -> пассажир не успеет, маршрут отсекается
        system.addTrip(new Trip(route1, "08:00", false));
        system.addTrip(new Trip(route2, "08:18", false));

        var transfers = system.findTransferRoutes(stopStation, stopSlava, 5, false);
        assertEquals(0, transfers.size());
    }
}
