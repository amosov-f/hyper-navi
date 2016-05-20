package ru.hypernavi.server.servlet.admin;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import ru.hypernavi.core.auth.AdminRequestReader;
import ru.hypernavi.core.session.Property;
import ru.hypernavi.core.session.Session;
import ru.hypernavi.core.session.SessionInitializer;
import ru.hypernavi.server.servlet.HtmlPageHttpService;
import ru.hypernavi.util.ArrayGeoPoint;
import ru.hypernavi.util.GeoPoint;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Created by Константин on 02.09.2015.
 */
@WebServlet(name = "adminka", value = "/admin")
public final class AdminService extends HtmlPageHttpService {
    private static final GeoPoint DEFAULT_CENTER = ArrayGeoPoint.of(30.1466, 59.796);
    private static final int DEFAULT_ZOOM = 9;

    @NotNull
    private final LocalDateTime initTime = LocalDateTime.now(ZoneId.of("Europe/Moscow"));

    @NotNull
    @Override
    protected SessionInitializer createReader(@NotNull final HttpServletRequest req) {
        return new AdminRequestReader(req);
    }

    @NotNull
    @Override
    public String getPathInBundle(@NotNull final Session session) {
        return "admin.ftl";
    }

    @NotNull
    @Override
    public Object toDataModel(@NotNull final Session session) {
        final ImmutableMap.Builder<String, Object> dataModel = new ImmutableMap.Builder<>();
        dataModel.put("server_starts", initTime);
        dataModel.put("lang", session.demand(Property.LANG));
        dataModel.put("center", session.get(Property.GEO_LOCATION, DEFAULT_CENTER));
        dataModel.put("zoom", session.get(Property.ZOOM, DEFAULT_ZOOM));
        return dataModel.build();
    }
}
