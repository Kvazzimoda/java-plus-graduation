package ru.practicum.stats.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import ru.practicum.stats.analyzer.processor.SimilarityProcessor;
import ru.practicum.stats.analyzer.processor.UserActionProcessor;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Analyzer {
    public static void main(String[] args) {

        // Запуск Spring Boot приложения при помощи вспомогательного класса SpringApplication
        // метод run возвращает назад настроенный контекст, который мы можем использовать для
        // получения настроенных бинов
        ConfigurableApplicationContext context = SpringApplication.run(Analyzer.class, args);

        final UserActionProcessor userActionProcessor = context.getBean(UserActionProcessor.class);
        SimilarityProcessor similarityProcessor = context.getBean(SimilarityProcessor.class);

        // Запуск обработчика событий в отдельном потоке от пользователей
        Thread userActionsThread = new Thread(userActionProcessor);
        userActionsThread.setName("UserActionHandlerThread");
        userActionsThread.start();

        // В текущем потоке начинаем обработку сходств
        similarityProcessor.start();
    }
}
