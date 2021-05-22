package io.prestosql.jdbc;

import io.prestosql.jdbc.$internal.okhttp3.OkHttpClient;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.prestosql.jdbc.$internal.guava.base.MoreObjects.firstNonNull;
import static io.prestosql.jdbc.$internal.client.OkHttpUtil.setupChannelSocket;
import static io.prestosql.jdbc.$internal.client.OkHttpUtil.userAgent;
import static java.lang.Integer.parseInt;

public class PrestoDriver2
        implements Driver, Closeable
{
    static final String DRIVER_NAME = "Presto JDBC Driver";
    static final String DRIVER_VERSION;
    static final int DRIVER_VERSION_MAJOR;
    static final int DRIVER_VERSION_MINOR;

    private final OkHttpClient httpClient = newHttpClient();

    static {
        String implementationVersion = PrestoDriver2.class.getPackage().getImplementationVersion();
        DRIVER_VERSION = implementationVersion == null ? "unknown" : implementationVersion;
        Matcher matcher = Pattern.compile("^(\\d+)(\\.(\\d+))?($|[.-])").matcher(DRIVER_VERSION);
        if (!matcher.find()) {
            DRIVER_VERSION_MAJOR = 0;
            DRIVER_VERSION_MINOR = 0;
        }
        else {
            DRIVER_VERSION_MAJOR = parseInt(matcher.group(1));
            DRIVER_VERSION_MINOR = parseInt(firstNonNull(matcher.group(3), "0"));
        }

        try {
            DriverManager.registerDriver(new PrestoDriver2());
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close()
    {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    @Override
    public Connection connect(String url, Properties info)
            throws SQLException
    {
        if (!acceptsURL(url)) {
            return null;
        }

        PrestoDriverUri uri = PrestoDriverUri.create(url, info);

        OkHttpClient.Builder builder = httpClient.newBuilder();
        uri.setupClient(builder);
        QueryExecutor executor = new QueryExecutor(builder.build());

        return new PrestoConnection(uri, executor);
    }

    @Override
    public boolean acceptsURL(String url)
            throws SQLException
    {
        return PrestoDriverUri.acceptsURL(url);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
            throws SQLException
    {
        Properties properties = PrestoDriverUri.create(url, info).getProperties();

        return ConnectionProperties.allProperties().stream()
                .map(property -> property.getDriverPropertyInfo(properties))
                .toArray(DriverPropertyInfo[]::new);
    }

    @Override
    public int getMajorVersion()
    {
        return DRIVER_VERSION_MAJOR;
    }

    @Override
    public int getMinorVersion()
    {
        return DRIVER_VERSION_MINOR;
    }

    @Override
    public boolean jdbcCompliant()
    {
        // TODO: pass compliance tests
        return false;
    }

    @Override
    public Logger getParentLogger()
            throws SQLFeatureNotSupportedException
    {
        // TODO: support java.util.Logging
        throw new SQLFeatureNotSupportedException();
    }

    private OkHttpClient newHttpClient()
    {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .addInterceptor(userAgent(DRIVER_NAME + "/" + DRIVER_VERSION));

        // Enable socket factory only for pre JDK 11
        setupChannelSocket(builder);
        return builder.build();
    }
}
