package ru.practicum.main.util;

public class Updater {
    public static void update(Object o, Runnable runnable) {
        if (o != null) runnable.run();
    }
}
