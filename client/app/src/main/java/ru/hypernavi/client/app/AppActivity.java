package ru.hypernavi.client.app;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Display;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ZoomControls;
import ru.hypernavi.client.app.util.CacheWorker;
import ru.hypernavi.client.app.util.GeoPointsUtils;
import ru.hypernavi.client.app.util.InfoRequestHandler;
import ru.hypernavi.client.app.util.SafeLoader;
import ru.hypernavi.util.Config;
import ru.hypernavi.util.GeoPoint;

public final class AppActivity extends Activity {
    private static final Logger LOG = Logger.getLogger(AppActivity.class.getName());
    public static final String PROPERTIES_SCHEME = "classpath:/app-common.properties";

    private Bitmap originScheme;
    private int displayWidth;
    private int displayHeight;

    private ImageView imageView;

    private ExecutorService executorService;

    private LocationManager locationManager;

    private OrientationEventListener orientationEventListener;

    private InfoRequestHandler handler;
    private SafeLoader loader;
    private CacheWorker cache;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LOG.info("onCreate start");

        setContentView(R.layout.main);
        imageView = (ImageView) findViewById(R.id.imageView);

        executorService = Executors.newFixedThreadPool(getNThread(PROPERTIES_SCHEME));

        cache = new CacheWorker(this);
        loader = new SafeLoader(executorService, cache);
        handler = new InfoRequestHandler(this, loader, cache);

        getParametersDisplay();
        drawDisplayImage(imageView);

        orientationEventListener = new OrientationEventListener(imageView, this);

        registerGPSListeners(imageView);
        registerZoomListeners(imageView);
        registerTouchListeners(imageView);
    }

    private void getParametersDisplay() {
        final Display display = getWindowManager().getDefaultDisplay();
        final Point displaySize = new Point();
        display.getSize(displaySize);
        displayWidth = displaySize.x;
        displayHeight = displaySize.y;
    }

    private void registerZoomListeners(final ImageView imageView) {
        final ZoomControls zoom = (ZoomControls) findViewById(R.id.zoomControls1);

        final ZoomInClickListener zoomInClickListener = new ZoomInClickListener(imageView);
        final ZoomOutClickListener zoomOutClickListener = new ZoomOutClickListener(imageView);

        zoom.setOnZoomInClickListener(zoomInClickListener);
        zoom.setOnZoomOutClickListener(zoomOutClickListener);
    }

    private void registerTouchListeners(final ImageView imageView) {
        final ViewOnTouchListener viewOnTouchListener = new ViewOnTouchListener(imageView, orientationEventListener);
        imageView.setOnTouchListener(viewOnTouchListener);
    }

    private void registerGPSListeners(final ImageView imageView) {
        locationManager = ((LocationManager) getSystemService(Context.LOCATION_SERVICE));
        if (!isGPSProviderEnabled()) {
            writeWarningMessage("GPS disabled!");
            LOG.warning("No GPS module finded.");
            return;
        }
        final Location cashLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if ((cashLocation != null) && (PositionUpdater.isActual(cashLocation, 0L))) {
            LOG.info("cashLocation is actual");
            processInfo(GeoPointsUtils.makeGeoPoint(cashLocation), imageView);
        } else {
            LOG.info("cashLocation is not actual");
        }
        sendRequest();
    }

    private boolean isGPSProviderEnabled() {
        // TODO amosov-f: remove intent checking
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || getIntent().getBooleanExtra("enabled", false);
    }

    public void sendRequest() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
            new PositionUpdater(locationManager, imageView, (Button) findViewById(R.id.button), this));
    }

    private void drawDisplayImage(final ImageView imageView) {
        originScheme = cache.loadCachedOrDefaultScheme();
        imageView.setImageBitmap(originScheme);
        LOG.info("Image XScale " + imageView.getScaleX());
        LOG.info("Display width " + displayWidth);
        LOG.info("Display high " + displayHeight);
    }

    public void processInfo(final GeoPoint geoPosition, final ImageView imageView) {
        originScheme = handler.getSchemaResponce(geoPosition);

        if (originScheme == null) {
            originScheme = cache.loadCachedOrDefaultScheme();
            LOG.warning("Problems with scheme above");
        }

        imageView.setImageBitmap(originScheme);
        cache.saveSchemeToCache(originScheme);

        LOG.info("GeoPosition " + geoPosition);
        writeWarningMessage("GeoPosition " + geoPosition);
    }

    private int getNThread(@NotNull final String path) {
        try {
            final Config config = Config.load(path);
            return config.getInt("app.request.pool.size");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeWarningMessage(@NotNull final String message) {
        Toast.makeText(AppActivity.this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // for the system's orientation sensor registered listeners
        orientationEventListener.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // to stop the sensor listener and save battery
        orientationEventListener.onPause();
    }
}