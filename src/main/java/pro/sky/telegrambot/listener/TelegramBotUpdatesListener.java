package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private static final DateTimeFormatter DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("d.M.yyyy HH:mm");
    private static final Pattern PATTERN = Pattern.compile("([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)");

    private final TelegramBot bot;
    private final NotificationRepository repository;

    public TelegramBotUpdatesListener(TelegramBot bot, NotificationRepository repository) {
        this.bot = bot;
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        bot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        for (Update update : updates) {
            String text = update.message().text();
            var chatId = update.message().chat().id();
            if ("/start".equals(text)) {
                bot.execute(new SendMessage(chatId, "Добро пожаловать в ReminderBot!"));
            } else {
                var matcher = PATTERN.matcher(text);
                if (matcher.matches()) {
                    LocalDateTime dateTime = parseTime(matcher.group(1));
                    if (dateTime == null) {
                        bot.execute(new SendMessage(chatId, "Формат даты указан неверно!"));
                        continue;
                    }
                    var taskText = matcher.group(3);
                    NotificationTask task = new NotificationTask();
                    task.setChatId(chatId);
                    task.setText(taskText);
                    task.setDateTime(dateTime);
                    NotificationTask saved = repository.save(task);
                    sendMessage(chatId, "Задача запланирована!");
                    logger.info("Уведомление о задаче сохранено: {}", saved);
                }
            }
        }
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void sendMessage(long chatId, String text) {
        bot.execute(new SendMessage(chatId, text));
    }

    private LocalDateTime parseTime(String text) {
        try {
            return LocalDateTime.parse(text, DATE_TIME_PATTERN);
        } catch (DateTimeParseException e) {
            logger.error("Невозможно проанализировать дату и время: {}", text);
        }
        return null;
    }
}