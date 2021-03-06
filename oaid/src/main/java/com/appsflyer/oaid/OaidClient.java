package com.appsflyer.oaid;

import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.huawei.hms.ads.identifier.AdvertisingIdClient;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OaidClient {
    private final Logger logger = Logger.getLogger("AppsFlyerOaid" + BuildConfig.VERSION_NAME);
    private final Context context;
    private final long timeout;
    private final TimeUnit unit;

    public OaidClient(Context context, long timeout, TimeUnit unit) {
        this.context = context;
        this.timeout = timeout;
        this.unit = unit;
        logger.setLevel(Level.OFF);
    }

    /**
     * 1 second timeout
     */
    public OaidClient(Context context) {
        this(context, 1, TimeUnit.SECONDS);
    }

    private static boolean isHuawei() {
        try {
            return Build.BRAND.equalsIgnoreCase("huawei") ||
                    (Integer) Class.forName("com.huawei.android.os.BuildEx$VERSION")
                            .getDeclaredField("EMUI_SDK_INT")
                            .get(null) > 0;
        } catch (ClassNotFoundException ignored) {
            return false;
        } catch (NoSuchFieldException ignored) {
            return false;
        } catch (IllegalAccessException ignored) {
            return false;
        }
    }

    private static boolean isMsaAvailableAtRuntime() {
        try {
            Class.forName("com.bun.supplier.IIdentifierListener");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Blocking call. Time to fetch oaid is 10 - 1000 ms.
     */
    @Nullable
    public Info fetch() {
        try {
            long current = System.currentTimeMillis();
            Info info;
            if (isHuawei()) info = fetchHuawei();
            else if (isMsaAvailableAtRuntime())
                info = OaidMsaClient.fetchMsa(context, logger, timeout, unit);
            else info = null;
            logger.info("Fetch " + (System.currentTimeMillis() - current) + " ms");
            return info;
        } catch (Throwable t) {
            logger.info(t.getMessage());
            return null;
        }
    }

    @Nullable
    private Info fetchHuawei() {
        try {
            if (AdvertisingIdClient.isAdvertisingIdAvailable(context)) {
                AdvertisingIdClient.Info info = AdvertisingIdClient.getAdvertisingIdInfo(context);
                return new Info(info.getId(), info.isLimitAdTrackingEnabled());
            } else {
                return null;
            }
        } catch (Throwable t) {
            logger.info(t.getMessage());
            return null;
        }
    }

    public void setLogging(boolean logging) {
        logger.setLevel(logging ? null : Level.OFF);
    }

    public static class Info {
        private final Boolean lat;
        private final String id;

        @VisibleForTesting
        public Info(String id, Boolean lat) {
            this.id = id;
            this.lat = lat;
        }

        public Info(String id) {
            this(id, null);
        }

        public String getId() {
            return id;
        }

        /**
         * Available only in Huawei
         */
        @Nullable
        public Boolean getLat() {
            return lat;
        }
    }
}