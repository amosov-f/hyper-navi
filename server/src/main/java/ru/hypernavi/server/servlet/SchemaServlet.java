package ru.hypernavi.server.servlet;

import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;


import ru.hypernavi.core.Hypermarket;
import ru.hypernavi.core.SchemaBuilder;
import ru.hypernavi.util.GeoPoint;

/**
 * Created by Константин on 19.07.2015.
 */
@WebServlet(name = "schema", value = "/schema")
public final class SchemaServlet extends AbstractHttpService {
    @NotNull
    private final List<Hypermarket> hypernavis;
    private static final double MIN_DISTANCE = 40.0;

    public SchemaServlet()
    {
        final SchemaBuilder reader = new SchemaBuilder();
        hypernavis = reader.read("/hypernavi_list.txt");
    }


    @Override
    public void process(@NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response) throws IOException {
        final Map<String, String[]> parameterMap = request.getParameterMap();
        if (!parameterMap.containsKey("lon") || !parameterMap.containsKey("lat"))
        {
            response.sendError(HttpServletResponse.SC_BAD_GATEWAY);
            return;
        }

        final Double longitude = Double.parseDouble(request.getParameter("lon"));
        final Double latitude = Double.parseDouble(request.getParameter("lat"));
        final GeoPoint currentPosition = new GeoPoint(latitude, longitude);

        final int bestHypernavi = closestHypernavi(currentPosition);
        if (bestHypernavi == -1 || GeoPoint.distance(hypernavis.get(bestHypernavi).getLocation(), currentPosition) > MIN_DISTANCE)
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
        else {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("image/jpeg");
            final OutputStream out = response.getOutputStream();
            ImageIO.write(hypernavis.get(bestHypernavi).getSchema(), "jpg", out);
        }
    }

    private int closestHypernavi(final GeoPoint position) {
        double minDistance = Double.POSITIVE_INFINITY;
        int bestHypernavi = -1;
        for (int i = 0; i < hypernavis.size(); ++i) {
            if (GeoPoint.distance(hypernavis.get(i).getLocation(), position) < minDistance) {
                minDistance = GeoPoint.distance(hypernavis.get(i).getLocation(), position);
                bestHypernavi = i;
            }
        }
        return bestHypernavi;
    }
}
