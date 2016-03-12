package ru.hypernavi.server.servlet.example;

import org.jetbrains.annotations.NotNull;


import javax.servlet.annotation.WebServlet;


import com.google.inject.Inject;
import ru.hypernavi.core.session.RequestReader;
import ru.hypernavi.core.session.Session;
import ru.hypernavi.server.servlet.HtmlPageHttpService;

/**
 * User: amosov-f
 * Date: 05.03.16
 * Time: 23:31
 */
@WebServlet(name = "example page", value = "/example")
public final class ExampleService extends HtmlPageHttpService {
    @Inject
    public ExampleService(@NotNull final RequestReader.Factory<?> init) {
        super(init);
    }

    @NotNull
    @Override
    public String getPathInBundle(@NotNull final Session session) {
        return "katya/bootstrap.html";
    }
}
