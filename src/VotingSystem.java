import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class VotingSystem {
    private static final String DB_SERVER = System.getenv("DB_SERVER");
    private static final String NAME = "VotingSystem";
    private static final String URL = DB_SERVER + NAME;
    private static final String USER = System.getenv("DB_USER");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");
    private static final Map<String, Integer> votes = new HashMap<>(); //MAP<Key, Value>

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String input;

        // 초기항목 설정
        votes.put("영화", 0);
        votes.put("음악", 0);
        votes.put("게임", 0);

        System.out.println("투표할 항목을 입력하세요 (영화, 음악, 게임). 종료하려면 '종료' 입력:");

        while (!(input = scanner.nextLine()).equals("종료")) {
            // containsKey(Object key): 지정된 키가 맵에 있는지 확인. 맵 내에 해당 키가 존재하면 true를 반환하고, 그렇지 않으면 false를 반환
            if (votes.containsKey(input)) {
                // .get key에 대응하는 value가져옴, .put key-value 저장하거나 업데이트
                votes.put(input, votes.get(input) + 1);
                System.out.println(input + "에 투표하셨습니다.");
            } else {
                System.out.println("유효하지 않은 항목입니다. 다시 입력하세요.");
            }
        }
        // 데이터베이스 결과 저장
        saveResults();
    }

    private static void saveResults() {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
            createTable(connection);
            // votes.entrySet() 메서드는 맵의 키-값 쌍을 Map.Entry<String, Integer> 형태의 Set으로 반환
            for (Map.Entry<String, Integer> entry : votes.entrySet()) {
                String item = entry.getKey();
                int voteCount = entry.getValue();

                // 데이터베이스에 결과 업데이트
                // ON DUPLICATE KEY UPDATE votes =? : INSERT하려는 item이 이미 테이블에 존재한다면(즉, 기본 키 위반이 발생한다면), votes 컬럼의 값을 업데이트
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO voting (item, voteCount) VALUES (?,?) ON DUPLICATE KEY UPDATE voteCount =?");
                statement.setString(1, item);
                statement.setInt(2, voteCount);
                statement.setInt(3, voteCount);
                statement.executeUpdate();
            }
            System.out.println("투표 결과가 데이터베이스에 저장되었습니다.");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("데이터베이스 저장 중 오류가 발생했습니다.");
        }
    }

    private static void createTable(Connection connection) throws SQLException {
        // IF NOT EXISTS : 테이블이 이미 존재하지 않을 경우에만 생성 (IF NOT EXISTS 구문 사용)
        String createTableSQL = "CREATE TABLE IF NOT EXISTS voting (" +
                                "item VARCHAR(255) PRIMARY KEY," +
                                "voteCount Int DEFAULT 0" +
                                ")";
        try (PreparedStatement pstmt = connection.prepareStatement(createTableSQL)) {
            pstmt.executeUpdate();
        }
    }
}
