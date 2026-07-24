package org.leavesmc.leavesclip.mixin;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;

public final class UrlUtil {
    public static final Path LOADER_CODE_SOURCE = getCodeSource(UrlUtil.class);

    public static Path asPath(URL url) {
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Path getCodeSource(Class<?> cls) {
        CodeSource cs = cls.getProtectionDomain().getCodeSource();
        if (cs == null) {
            return null;
        }
        return asPath(cs.getLocation());
    }
}
