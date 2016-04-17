package ru.hypernavi.server.servlet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;


import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.log4j.MDC;
import org.eclipse.jetty.server.Request;
import ru.hypernavi.core.http.HttpTools;
import ru.hypernavi.core.session.*;
import ru.hypernavi.server.servlet.dump.DumpWriters;
import ru.hypernavi.util.json.GsonUtils;

/**
 * User: amosov-f
 * Date: 06.08.15
 * Time: 1:33
 */
public abstract class AbstractHttpService extends HttpServlet {
    private static final Log LOG = LogFactory.getLog(AbstractHttpService.class);

    @NotNull
    private final RequestReader.Factory<?> initFactory;
    @Inject
    @Nullable
    private Provider<Session> sessionFactory;

    @Inject
    private Provider<DumpWriters> dumpWritersProvider;

    protected AbstractHttpService(@NotNull final RequestReader.Factory<?> initFactory) {
        this.initFactory = initFactory;
    }

    @NotNull
    protected WebServlet getServiceConfig() {
        return Objects.requireNonNull(
                getClass().getAnnotation(WebServlet.class),
                "Annotate " + getClass().getSimpleName() + " as @WebServlet!"
        );
    }

    @Override
    protected final void service(@NotNull final HttpServletRequest req, @NotNull final HttpServletResponse resp) throws IOException {
        final long startTime = System.currentTimeMillis();

        final Session session = Objects.requireNonNull(sessionFactory).get();
        MDC.put("reqid", session.getId());

        final DumpWriters dumpWriters = dumpWritersProvider.get();
        dumpWriters.enable(session, req);

        LOG.info("Started processing: " + HttpTools.curl(req));

        final SessionInitializer initializer = initFactory.create(req);

        initializer.initialize(session);
        if (validate(initializer, session, resp)) {
            service(session, resp);
        }

        req.setAttribute(HttpTools.SERVICE, getServiceConfig().name());
        LOG.info(String.format(
                "Finished processing in [service: %d, jetty: %d] ms",
                System.currentTimeMillis() - startTime,
                System.currentTimeMillis() - ((Request) req).getTimeStamp()
        ));

        dumpWriters.dump(session, resp);
    }

    private boolean validate(@NotNull final SessionInitializer initializer,
                             @NotNull final Session session,
                             @NotNull final HttpServletResponse resp) throws IOException
    {
        try {
            initializer.validate(session);
            return true;
        } catch (SessionValidationException e) {
            switch (e.getError()) {
                case BAD_REQUEST:
                    LOG.info("Bad request: " + e.getMessage());
                    resp.sendError(HttpStatus.SC_BAD_REQUEST, e.getMessage());
                    break;
                case UNAUTHORIZED:
                    LOG.info("Unauthorized: " + e.getMessage());
                    resp.sendRedirect("/auth?url=" + session.get(Property.HTTP_REQUEST_URI));
                    break;
                case FORBIDDEN:
                    LOG.info("Forbidden: " + e.getMessage());
                    resp.sendError(HttpStatus.SC_FORBIDDEN, e.getMessage());
            }
            return false;
        }
    }

    public abstract void service(@NotNull final Session session, @NotNull final HttpServletResponse resp) throws IOException;

    protected final void write(@NotNull final Object obj, @NotNull final HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpStatus.SC_OK);
        resp.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        GsonUtils.gson().toJson(obj, resp.getWriter());
    }
}
