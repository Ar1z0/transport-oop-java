package ru.voronezh.transport;

import java.util.Objects;

/**
 * Класс, представляющий остановки
 */
public final class Stop {
    private final String name; // Уникальное название остановки

    public Stop(String name) {
        // Objects.requireNonNull выбрасывает NullPointerException с понятным сообщением,
        // если передать null вместо названия остановки.
        this.name = Objects.requireNonNull(name, "Имя остановки не может быть null");
    }

    public String getName() {
        return name;
    }

    /**
     * Переопределение equals: две остановки считаются одинаковыми,
     * если у них совпадают названия
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // Ссылка на один и тот же объект в памяти
        if (o == null || getClass() != o.getClass()) return false; // Проверка на null и соответствие классов
        Stop stop = (Stop) o;
        return Objects.equals(name, stop.name);
    }

    /**
     * hashCode должен быть согласован с equals: если объекты равны по equals,
     * их хэш-коды также обязаны совпадать
     */
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    /**
     * Чтобы при выводе объекта в консоль или лог отображалось название, а белиберда.
     */
    @Override
    public String toString() {
        return name;
    }
}
