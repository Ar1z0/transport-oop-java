package ru.voronezh.transport;

/**
 * Перечисление (enum) доступных видов городского транспорта.
 *
 * Хранит характеристики для каждого типа:
 * 1. Человекочитаемое название (title).
 * 2. Среднюю скорость движения в км/ч (speed).
 * 3. Вместимость пассажиров в салоне (places).
 */
public enum TransportType {
    BUS("Автобус", 24, 85),
    TROLLEYBUS("Троллейбус", 20, 95),
    BUS2("Маршрутка", 28, 40);

    private final String title;   // Название (например, "Автобус")
    private final int speed;      // Скорость (км/ч)
    private final int places;     // Вместимость (чел.)

    /**
     * Конструктор элемента перечисления. Вызывается автоматически при инициализации enum.
     */
    TransportType(String title, int speed, int places) {
        this.title = title;
        this.speed = speed;
        this.places = places;
    }

    public String getTitle() { return title; }
    public int getSpeed() { return speed; }
    public int getPlaces() { return places; }
}
