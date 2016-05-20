package ru.hypernavi.core.telegram;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpGet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.hypernavi.commons.Image;
import ru.hypernavi.commons.Index;
import ru.hypernavi.commons.SearchResponse;
import ru.hypernavi.commons.Site;
import ru.hypernavi.commons.hint.Hint;
import ru.hypernavi.commons.hint.Picture;
import ru.hypernavi.commons.hint.Plan;
import ru.hypernavi.core.http.HttpClient;
import ru.hypernavi.core.http.URIBuilder;
import ru.hypernavi.core.telegram.api.Message;
import ru.hypernavi.core.telegram.api.TelegramApi;
import ru.hypernavi.core.telegram.api.Update;
import ru.hypernavi.core.telegram.api.entity.BotCommand;
import ru.hypernavi.core.telegram.api.inline.InlineQuery;
import ru.hypernavi.core.telegram.api.inline.InlineQueryResult;
import ru.hypernavi.core.telegram.api.inline.InlineQueryResultPhoto;
import ru.hypernavi.core.telegram.api.markup.KeyboardButton;
import ru.hypernavi.core.telegram.api.markup.ReplyKeyboardMarkup;
import ru.hypernavi.core.telegram.api.markup.ReplyMarkup;
import ru.hypernavi.core.telegram.update.UpdatesSource;
import ru.hypernavi.util.GeoPoint;
import ru.hypernavi.util.MoreReflectionUtils;
import ru.hypernavi.util.awt.ImageUtils;
import ru.hypernavi.util.concurrent.LoggingThreadFactory;
import ru.hypernavi.util.json.GsonUtils;
import ru.hypernavi.util.stream.MoreStreamSupport;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by amosov-f on 17.10.15.
 */
public final class HyperNaviBot {
    private static final Log LOG = LogFactory.getLog(HyperNaviBot.class);

    static {
        MoreReflectionUtils.load(Index.class);
        MoreReflectionUtils.load(Site.class);
    }

    @NotNull
    private final ExecutorService service = Executors.newCachedThreadPool(new LoggingThreadFactory("SENDER"));

    @Inject
    private TelegramApi api;
    @Inject
    @Named("hypernavi.telegram.bot.search_host")
    private String searchHost;
    @Inject
    private HttpClient httpClient;

    @Inject
    private UpdatesSource updatesSource;

    public void start(final boolean inBackground) {
        if (inBackground) {
            service.submit((Runnable) this::start);
        } else {
            start();
        }
    }

    public void start() {
        while (true) {
            LOG.info("Waiting for next update...");
            final Update update = updatesSource.next();
            service.submit(() -> {
                try {
                    LOG.debug("Update processing started: '" + update + "'");
                    processUpdate(update);
                } catch (RuntimeException e) {
                    LOG.error("Error while processing update: '" + update + "'", e);
                }
            });
        }
    }

    private void processUpdate(@NotNull final Update update) {
        Optional.ofNullable(update.getInlineQuery()).ifPresent(this::processInlineQuery);
        Optional.ofNullable(update.getMessage()).ifPresent(this::processMessage);
    }

    private void processInlineQuery(@NotNull final InlineQuery inlineQuery) {
        final GeoPoint location = inlineQuery.getLocation();
        final String query = inlineQuery.getQuery();
        final SearchResponse searchResponse;
        if (!StringUtils.isBlank(query)) {
            searchResponse = search(query);
            if (searchResponse == null) {
                LOG.error("Server not respond by query: '" + query + "'");
                return;
            }
        } else if (location != null) {
            searchResponse = search(location);
            if (searchResponse == null) {
                LOG.error("Server not respond by location: " + location);
                return;
            }
        } else {
            return;
        }
        final InlineQueryResult[] results = searchResponse.getData().getSites().stream()
                .map(Index::get)
                .map(Site::getHints)
                .flatMap(Arrays::stream)
                .filter(Picture.class::isInstance)
                .map(Picture.class::cast)
                .map(Picture::getImage)
                .map(image -> new InlineQueryResultPhoto(image.getLink(), image.getLink(), image.getThumbOrFull()))
                .toArray(InlineQueryResult[]::new);
        api.answerInlineQuery(inlineQuery.getId(), results);
    }

    private void processMessage(@NotNull final Message message) {
        final int chatId = message.getChat().getId();
        final GeoPoint location = message.getLocation();
        final String text = message.getText();
        final boolean startCommand = MoreStreamSupport.instances(message.getEntities(), BotCommand.class)
                .anyMatch(command -> command.getCommand(Objects.requireNonNull(text)).equals("/start"));
        if (startCommand) {
            final KeyboardButton button = new KeyboardButton("Отправить геопозицию", false, true);
            final ReplyMarkup replyMarkup = new ReplyKeyboardMarkup(new KeyboardButton[]{button});
            api.sendMessage(chatId, "Здравствуйте! Отправьте мне свою геопозицию, и я что-нибудь покажу =)", replyMarkup);
            return;
        }
        final SearchResponse searchResponse;
        if (location != null) {
            searchResponse = search(location);
            if (searchResponse == null) {
                api.sendMessage(chatId, "Простите, наш сервер не работает");
                return;
            }
        } else if (!StringUtils.isBlank(text)) {
            searchResponse = search(text);
            if (searchResponse == null) {
                api.sendMessage(chatId, "Простите, я не знаю такого места");
                return;
            }
        } else {
            return;
        }
        searchResponse.getData().getSites().stream().map(Index::get).forEach(site -> respond(chatId, site, location));

    }

    private void respond(final int chatId, @NotNull final Site site, @Nullable final GeoPoint location) {
        api.sendMessage(chatId, "Адрес: " + site.getPlace().getAddress());
        for (final Hint hint : site.getHints()) {
            if (hint instanceof Plan) {
                final Plan plan = (Plan) hint;
                if (location != null) {
                    final BufferedImage image = LocationMapper.INSTANCE.mapLocation(plan, location);
                    if (image != null) {
                        final Image.Format format = Optional.ofNullable(ImageUtils.format(image))
                                .map(Image.Format::parse)
                                .orElse(plan.getImage().getFormat(Image.Format.JPG));
                        api.sendPhoto(chatId, image, format, hint.getDescription());
                        continue;
                    }
                }
            }
            if (hint instanceof Picture) {
                api.sendPhoto(chatId, ((Picture) hint).getImage(), hint.getDescription());
            }
        }
    }

    @Nullable
    private SearchResponse search(@NotNull final GeoPoint location) {
        final URI uri = new URIBuilder("http://" + searchHost + "/search")
                .add("lon", location.getLongitude())
                .add("lat", location.getLatitude())
                .add("ns", 1)
                .build();
        return httpClient.execute(new HttpGet(uri), SearchResponse.class, GsonUtils.gson());
    }

    @Nullable
    private SearchResponse search(@NotNull final String text) {
        final URI uri = new URIBuilder("http://" + searchHost + "/search")
                .add("text", text)
                .add("ns", 1)
                .build();
        return httpClient.execute(new HttpGet(uri), SearchResponse.class, GsonUtils.gson());
    }
}
