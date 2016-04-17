package ru.hypernavi.core.auth;

import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;


import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.util.Types;
import org.apache.commons.lang3.ArrayUtils;
import ru.hypernavi.core.server.Platform;
import ru.hypernavi.core.session.Property;
import ru.hypernavi.core.session.RequestReader;
import ru.hypernavi.core.session.Session;
import ru.hypernavi.core.session.SessionValidationException;

/**
 * Created by amosov-f on 14.11.15.
 */
public class AdminRequestReader extends VkAuthRequestReader {
    @NotNull
    private final int[] adminUids;
    @Inject
    private Platform platform;

    @Inject
    public AdminRequestReader(@Assisted @NotNull final HttpServletRequest req) {
        super(req);
        final InputStream in = getClass().getResourceAsStream("/auth/vk/admins.txt");
        adminUids = new BufferedReader(new InputStreamReader(in)).lines()
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    @Override
    public void validate(@NotNull final Session session) throws SessionValidationException {
        if (platform.isLocal()) {
            return;
        }
        super.validate(session);
        if (!ArrayUtils.contains(adminUids, session.demand(Property.VK_USER).getUid())) {
            throw new SessionValidationException.Forbidden("Only admins have access to this page!");
        }
    }

    @NotNull
    public static Module module() {
        return new FactoryModuleBuilder().build(Key.get(Types.newParameterizedTypeWithOwner(RequestReader.class, Factory.class, AdminRequestReader.class)));
    }
}
