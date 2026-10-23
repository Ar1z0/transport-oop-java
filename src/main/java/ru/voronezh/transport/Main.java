package ru.voronezh.transport;

import java.util.List;
import java.util.Scanner;

/**
 * Главный класс приложения с точкой входа (main) и консольным интерфейсом.
 */
public class Main {

    /**
     * Инициализация демонстрационных тестовых данных Воронежа:
     * - Создаются 7 реальных ключевых остановок.
     * - Формируются 2 маршрута (Автобус 9КА и Троллейбус 7) с интервалами хода в минутах.
     * - Добавляются утренние рейсы на будни и выходные дни.
     */
    public static TransportSystem initDemoData() {
        TransportSystem system = new TransportSystem();

        // 1. Создаем остановки
        Stop s1 = system.addStop("Вокзал Воронеж-1");
        Stop s2 = system.addStop("ЮВЖД");
        Stop s3 = system.addStop("Проспект Революции");
        Stop s4 = system.addStop("Площадь Ленина");
        Stop s5 = system.addStop("ВГУ");
        Stop s6 = system.addStop("Застава");
        Stop s7 = system.addStop("Памятник Славы");

        // 2. Маршрут 1: Автобус 9КА (Вокзал -> ЮВЖД -> Проспект Революции -> Площадь Ленина -> ВГУ)
        // Времена хода между перегонами: 6 мин, 5 мин, 4 мин, 5 мин
        Route route1 = new Route("Автобус 9КА", TransportType.BUS,
                List.of(s1, s2, s3, s4, s5), List.of(6, 5, 4, 5));

        // 2. Маршрут 2: Троллейбус 7 (Площадь Ленина -> Застава -> Памятник Славы)
        // Времена хода: 8 мин, 12 мин
        Route route2 = new Route("Троллейбус 7", TransportType.TROLLEYBUS,
                List.of(s4, s6, s7), List.of(8, 12));

        system.addRoute(route1);
        system.addRoute(route2);

        // Автобуса 9КА:
        // Будни
        system.addTrip(new Trip(route1, "08:00", false));
        system.addTrip(new Trip(route1, "08:15", false));
        system.addTrip(new Trip(route1, "08:35", false));
        // Выходные
        system.addTrip(new Trip(route1, "09:00", true));
        system.addTrip(new Trip(route1, "09:30", true));

        // Троллейбуса 7:
        // Будни
        system.addTrip(new Trip(route2, "08:20", false));
        system.addTrip(new Trip(route2, "08:40", false));
        // Выходные
        system.addTrip(new Trip(route2, "09:15", true));

        return system;
    }

    public static void main(String[] args) {
        TransportSystem system = initDemoData();
        Scanner scanner = new Scanner(System.in);

        System.out.println("============================================================");
        System.out.println(" МОДЕЛЬ МАРШРУТОВ ГОРОДСКОГО ТРАНСПОРТА ВОРОНЕЖА (JAVA)");
        System.out.println("============================================================");

        // Бесконечный цикл текстового меню
        while (true) {
            System.out.println("\nОСНОВНОЕ МЕНЮ:");
            System.out.println("1. Расписание по остановке");
            System.out.println("2. Расчёт прибытия на k-ю остановку");
            System.out.println("3. Расчёт интервалов движения ");
            System.out.println("4. Поиск поездки (прямой путь или с 1 пересадкой)");
            System.out.println("0. Выход");
            System.out.print("Выберите действие: ");

            String cmd = scanner.nextLine().trim();
            if ("0".equals(cmd)) {
                System.out.println("Работа программы завершена.");
                break;
            }

            switch (cmd) {
                case "1" -> {
                    // Вывод расписания для конкретной остановки
                    System.out.println("Остановки: " + system.getStops().keySet());
                    System.out.print("Название остановки: ");
                    String name = scanner.nextLine().trim();
                    Stop stop = system.getStops().get(name);
                    if (stop != null) {
                        System.out.print("Выходной день? (y/n): ");
                        boolean isWk = scanner.nextLine().trim().equalsIgnoreCase("y");
                        var schedule = system.getScheduleForStop(stop, isWk);
                        System.out.println("--- Расписание (" + (isWk ? "Выходные" : "Будни") + ") ---");
                        if (schedule.isEmpty()) System.out.println("  Рейсов нет.");
                        for (var e : schedule) {
                            System.out.println("  " + e.time() + " | " + e.routeNumber() + " (" + e.transportType() + ")");
                        }
                    } else {
                        System.out.println("Ошибка: остановка не найдена.");
                    }
                }
                case "2" -> {
                    // Расчет времени прибытия рейса на k-ю по счету остановку
                    System.out.println("Маршруты: " + system.getRoutes().keySet());
                    System.out.print("Номер маршрута: ");
                    String num = scanner.nextLine().trim();
                    Route r = system.getRoutes().get(num);
                    if (r != null) {
                        System.out.print("Номер остановки k (1.." + r.getStops().size() + "): ");
                        int k = Integer.parseInt(scanner.nextLine().trim());
                        System.out.print("Отправление (ЧЧ:ММ): ");
                        String dep = scanner.nextLine().trim();
                        Trip trip = new Trip(r, dep, false);
                        System.out.println("Прибытие: " + TimeUtils.timeToStr(trip.arrivalAtKthStop(k)));
                    } else {
                        System.out.println("Ошибка: маршрут не найден.");
                    }
                }
                case "3" -> {
                    //Вычисление интервалов между рейсами
                    System.out.println("Маршруты: " + system.getRoutes().keySet());
                    System.out.print("Номер маршрута: ");
                    String num = scanner.nextLine().trim();
                    Route r = system.getRoutes().get(num);
                    if (r != null) {
                        System.out.print("Выходной день? (y/n): ");
                        boolean isWk = scanner.nextLine().trim().equalsIgnoreCase("y");
                        System.out.println("Интервалы движения: " + system.calculateHeadway(r, isWk) + " мин.");
                    } else {
                        System.out.println("Ошибка: маршрут не найден.");
                    }
                }
                case "4" -> {
                    // Поиск поездки. Сначала ищет прямой маршрут, если нет — с 1 пересадкой
                    System.out.println("Остановки: " + system.getStops().keySet());
                    System.out.print("Откуда: ");
                    String sFrom = scanner.nextLine().trim();
                    System.out.print("Куда: ");
                    String sTo = scanner.nextLine().trim();

                    Stop start = system.getStops().get(sFrom);
                    Stop end = system.getStops().get(sTo);

                    if (start != null && end != null) {
                        System.out.print("Выходной день? (y/n): ");
                        boolean isWk = scanner.nextLine().trim().equalsIgnoreCase("y");

                        // 1. Проверяем прямой рейс
                        var direct = system.findDirectRoutes(start, end, isWk);
                        if (!direct.isEmpty()) {
                            System.out.println("[Прямой рейс]: " + direct.get(0));
                        } else {
                            // 2. Если прямого нет, ищем рейс с 1 пересадкой (минимум 5 мин на пересадку)
                            var transfers = system.findTransferRoutes(start, end, 5, isWk);
                            if (!transfers.isEmpty()) {
                                System.out.println("[С пересадкой]: " + transfers.get(0));
                            } else {
                                System.out.println("Маршрутов не найдено.");
                            }
                        }
                    } else {
                        System.out.println("Ошибка: остановка не найдена.");
                    }
                }
            }
        }
    }
}
