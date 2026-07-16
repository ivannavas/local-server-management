package io.github.ivannavas.local_server_management.tools;

import io.github.ivannavas.local_server_management.model.HardwareStatus;
import io.github.ivannavas.local_server_management.repository.HardwareStatusRepository;
import io.github.ivannavas.sprout.annotation.Tool;
import io.github.ivannavas.sprout.mcp.annotation.Mcp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Mcp(name = "local-server-management")
@RequiredArgsConstructor
public class SystemTools {

    private static final String DATABASE_SIZE_QUERY =
            "SELECT datname, pg_database_size(datname) FROM pg_database " +
                    "WHERE datistemplate = false ORDER BY datname";

    private final HardwareStatusRepository hardwareStatusRepository;

    private final JdbcTemplate jdbcTemplate;

    @Tool(name = "getHardwareStatus", description = "Get the hardware status information")
    public HardwareStatus getHardwareStatus() {
        Map<String, Long> databasesSize = getDatabasesSize();
        return hardwareStatusRepository.findTopByOrderByRecordedAtDesc()
                .map(record -> new HardwareStatus(
                        record.getCpuTemperature().doubleValue(),
                        record.isBoostEnabled(),
                        databasesSize))
                .orElse(new HardwareStatus(0.0, false, databasesSize));
    }

    /**
     * Returns the on-disk size in bytes of every non-template database on the
     * PostgreSQL server, keyed by database name.
     */
    private Map<String, Long> getDatabasesSize() {
        Map<String, Long> sizes = new LinkedHashMap<>();
        jdbcTemplate.query(DATABASE_SIZE_QUERY, rs -> {
            sizes.put(rs.getString(1), rs.getLong(2));
        });
        return sizes;
    }
}
