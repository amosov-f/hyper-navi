package ru.hypernavi.server.servlet.admin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.annotation.WebServlet;
import java.util.Collections;


import ru.hypernavi.core.session.Property;
import ru.hypernavi.core.session.Session;
import ru.hypernavi.server.servlet.HtmlPageHttpService;

/**
 * User: amosov-f
 * Date: 15.05.16
 * Time: 15:05
 */
@WebServlet(name = "scheme markup", value = "/admin/markup")
public final class MarkupService extends HtmlPageHttpService {
    @NotNull
    @Override
    public String getPathInBundle(@NotNull final Session session) {
        return "markup.ftl";
    }

    @Nullable
    @Override
    public Object toDataModel(@NotNull final Session session) {
        return Collections.singletonMap("link", session.get(Property.LINK));
    }
}
