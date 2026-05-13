package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * V8~V10(SQL PREPARE/EXECUTE 방식) 실패 최종 보정.
 *
 * <p>SQL 방식은 Flyway/MySQL JDBC 환경에서 다중 문장 실행 및 user-variable 세션 유지
 * 문제로 불안정하다. Java Migration은 JDBC를 직접 사용하므로 이 문제가 없다.
 *
 * <p>동작:
 * <ol>
 *   <li>information_schema.columns 조회로 level 컬럼 존재 여부 확인</li>
 *   <li>컬럼 없음 → ALTER TABLE 실행</li>
 *   <li>컬럼 있음 → 아무 것도 하지 않음 (멱등)</li>
 * </ol>
 */
public class V11__EnsureLevelColumn extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM information_schema.columns"
                    + " WHERE table_schema = DATABASE()"
                    + "   AND table_name = 'users'"
                    + "   AND column_name = 'level'"
            );
            rs.next();
            boolean columnExists = rs.getInt(1) > 0;
            rs.close();

            if (!columnExists) {
                stmt.execute("ALTER TABLE users ADD COLUMN level VARCHAR(20) NULL");
            }
        }
    }
}
