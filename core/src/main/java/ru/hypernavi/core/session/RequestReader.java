package ru.hypernavi.core.session;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;


import com.google.common.net.HttpHeaders;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.Assisted;
import ru.hypernavi.core.http.HttpTools;
import ru.hypernavi.core.session.param.Param;
import ru.hypernavi.util.ArrayGeoPoint;
import ru.hypernavi.util.GeoPoint;

/**
 * Created by amosov-f on 24.10.15.
 */
public class RequestReader implements SessionInitializer {
    @NotNull
    protected final HttpServletRequest req;

    @Inject
    public RequestReader(@Assisted @NotNull final HttpServletRequest req) {
        this.req = req;
    }

    @Override
    public void initialize(@NotNull final Session session) {
        session.setIfNotNull(Property.HTTP_REQUEST_URL, HttpTools.requestURL(req));
        session.setIfNotNull(Property.HTTP_REQUEST_URI, HttpTools.requestURI(req));
        session.setIfNotNull(Property.HTTP_SERVLET_PATH, req.getServletPath());
        session.setIfNotNull(Property.HTTP_PATH_INFO, req.getPathInfo());
        session.setIfNotNull(Property.HTTP_QUERY_STRING, req.getQueryString());
        session.setIfNotNull(Property.HTTP_COOKIE, req.getHeader(HttpHeaders.COOKIE));

        setPropertyIfPresent(session, Property.TEXT, Param.TEXT);
        session.setIfNotNull(Property.GEO_LOCATION, getGeoLocation());

        setPropertyIfPresent(session, Property.DEBUG, Param.DEBUG);
    }

    @Override
    public void validate(@NotNull final Session session) throws SessionValidationException {
    }

    @Nullable
    private GeoPoint getGeoLocation() {
        final Double lon = Param.LON.getValue(req);
        final Double lat = Param.LAT.getValue(req);
        return lat != null && lon != null ? ArrayGeoPoint.of(lon, lat) : null;
    }

    protected <T> void setPropertyIfPresent(@NotNull final Session session,
                                            @NotNull final Property<T> property,
                                            @NotNull final Param<? extends T> param)
    {
        session.setIfNotNull(property, param.getValue(req));
    }

    @FunctionalInterface
    public interface Factory<T extends SessionInitializer> {
        @NotNull
        T create(@NotNull HttpServletRequest req);
    }

    @NotNull
    public static Module module() {
        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(new TypeLiteral<Factory<?>>() {}).toInstance(RequestReader::new);
            }
        };
    }
}
