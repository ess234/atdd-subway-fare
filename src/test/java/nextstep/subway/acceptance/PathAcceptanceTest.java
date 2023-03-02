package nextstep.subway.acceptance;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.subway.domain.PathType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashMap;
import java.util.Map;

import static nextstep.subway.acceptance.LineSteps.지하철_노선에_지하철_구간_생성_요청;
import static nextstep.subway.acceptance.MemberSteps.베어러_인증_로그인_요청;
import static nextstep.subway.acceptance.PathSteps.두_역의_경로_조회를_요청;
import static nextstep.subway.acceptance.PathSteps.회원계정으로_두_역의_경로_조회를_요청;
import static nextstep.subway.acceptance.StationSteps.지하철역_생성_요청;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("지하철 경로 검색")
class PathAcceptanceTest extends AcceptanceTest {
    private Long 교대역;
    private Long 강남역;
    private Long 양재역;
    private Long 남부터미널역;
    private Long 신논현역;
    private Long 이호선;
    private Long 신분당선;
    private Long 삼호선;
    private int 이호선_초과운임;
    private int 삼호선_초과운임;
    private int 신분당선_초과운임;
    private int 기본_운임비용;
    private double 청소년_할인_비율;
    private String YOUTH_EMAIL = "youth@email.com";
    private String YOUTH_PASSWORD = "password";

    /**
     *                          신논현역
     *                            /
     *                         *신분당선*
     *                           /
     * 교대역    --- *2호선* ---   강남역
     * |                        |
     * *3호선*                   *신분당선*
     * |                        |
     * 남부터미널역  --- *3호선* ---   양재
     */
    /**
     * 사전 세팅
     * Given 지하철역이 등록되어있음
     * And 지하철 노선이 등록되어있음
     * And 지하철 노선에 지하철역이 등록되어있음
     */
    @BeforeEach
    public void setUp() {
        super.setUp();
        기본_운임비용 = 1250;
        청소년_할인_비율 = 0.2;
        이호선_초과운임 = 0;
        신분당선_초과운임 = 900;
        삼호선_초과운임 = 500;

        교대역 = 지하철역_생성_요청("교대역").jsonPath().getLong("id");
        강남역 = 지하철역_생성_요청("강남역").jsonPath().getLong("id");
        양재역 = 지하철역_생성_요청("양재역").jsonPath().getLong("id");
        남부터미널역 = 지하철역_생성_요청("남부터미널역").jsonPath().getLong("id");
        신논현역 = 지하철역_생성_요청("신논현").jsonPath().getLong("id");
        이호선 = 지하철_노선_생성_요청("2호선", "green", 교대역, 강남역, 10, 5, 이호선_초과운임);
        신분당선 = 지하철_노선_생성_요청("신분당선", "red", 강남역, 양재역, 10, 9, 신분당선_초과운임);
        삼호선 = 지하철_노선_생성_요청("3호선", "orange", 교대역, 남부터미널역, 2, 10, 삼호선_초과운임);

        지하철_노선에_지하철_구간_생성_요청(삼호선, createSectionCreateParams(남부터미널역, 양재역, 3, 8));
        지하철_노선에_지하철_구간_생성_요청(신분당선, createSectionCreateParams(신논현역, 강남역, 5, 9));
    }

    /**
     * When 출발역에서 도착역까지의 최단 거리 경로 조회를 요청
     * Then 최단 거리 경로를 응답
     * And 총 거리와 소요 시간을 함께 응답함
     * And 지하철 이용 요금도 함께 응답함
     */
    @DisplayName("두 역의 최단 거리 경로를 조회한다.")
    @ParameterizedTest
    @EnumSource(value = PathType.class, names = { "거리", "시간" })
    void findPathByDistance(PathType pathType) {
        // when
        ExtractableResponse<Response> response = 두_역의_경로_조회를_요청(spec, 교대역, 양재역, pathType.getType());

        // then
        if (pathType.equals(PathType.거리)) {
            최단_검색응답_역_검증(response, 교대역, 남부터미널역, 양재역);
            최단_검색응답_거리_검증(response, 5);
            최단_검색응답_시간_검증(response, 18);
            최단_검색응답_이용요금_검증(response, (long) 기본_운임비용 + 삼호선_초과운임);

            return;
        }

        최단_검색응답_역_검증(response, 교대역, 강남역, 양재역);
        최단_검색응답_거리_검증(response, 20);
        최단_검색응답_시간_검증(response, 14);

        int distance = response.jsonPath().getInt("distance");
        최단_검색응답_이용요금_검증(response, (long) 기본_운임비용 + 오km마다_100원_초과운임_계산(distance) + 신분당선_초과운임);
    }

    /**
     * When 출발역에서 도착역까지의 최단 거리 경로 조회를 요청 (환승 노선 : 삼호선, 신분당선)
     * Then 노선 초과운임이 포함된 이용 요금을 응답함
     */
    @DisplayName("환승노선이 여러개인 두 역의 최단 시간 경로를 조회한다.")
    @Test
    void findPathByDurationOverFareLine() {
        // when
        ExtractableResponse<Response> response = 두_역의_경로_조회를_요청(spec, 남부터미널역, 신논현역, PathType.시간.getType());

        // then
        최단_검색응답_역_검증(response, 남부터미널역,교대역, 강남역, 신논현역);
        최단_검색응답_거리_검증(response, 17);
        최단_검색응답_시간_검증(response, 24);

        int distance = response.jsonPath().getInt("distance");
        최단_검색응답_이용요금_검증(response, (long) 기본_운임비용 + 오km마다_100원_초과운임_계산(distance) + 신분당선_초과운임);
    }

    /**
     * Given 청소년 계정으로 로그인하여 토큰을 발급
     * When 출발역에서 도착역까지의 최단 거리 경로 조회를 요청
     * Then 청소년할인된 이용요금을 응답받음
     */
    @DisplayName("청소년 계정으로 로그인 후 최단 거리 할인된 이용요금을 조회한다. ")
    @Test
    void findPathWithLogin(){
        String accessToken = 베어러_인증_로그인_요청(YOUTH_EMAIL, YOUTH_PASSWORD).jsonPath().getString("accessToken");

        ExtractableResponse<Response> response = 회원계정으로_두_역의_경로_조회를_요청(spec, accessToken, 교대역, 양재역, PathType.거리.getType());

        최단_검색응답_역_검증(response, 교대역, 남부터미널역, 양재역);
        최단_검색응답_거리_검증(response, 5);
        최단_검색응답_시간_검증(response, 18);

        Long expected = (long) ((기본_운임비용 + 삼호선_초과운임 - 350) * (1 - 청소년_할인_비율));
        최단_검색응답_이용요금_검증(response, expected);
    }

    private Long 지하철_노선_생성_요청(String name, String color, Long upStation, Long downStation, int distance, int duration, int overFare) {
        Map<String, String> lineCreateParams;
        lineCreateParams = new HashMap<>();
        lineCreateParams.put("name", name);
        lineCreateParams.put("color", color);
        lineCreateParams.put("upStationId", upStation + "");
        lineCreateParams.put("downStationId", downStation + "");
        lineCreateParams.put("distance", distance + "");
        lineCreateParams.put("duration", duration + "");
        lineCreateParams.put("overFare", overFare + "");

        return LineSteps.지하철_노선_생성_요청(lineCreateParams).jsonPath().getLong("id");
    }

    private Map<String, String> createSectionCreateParams(Long upStationId, Long downStationId, int distance, int duration) {
        Map<String, String> params = new HashMap<>();
        params.put("upStationId", upStationId + "");
        params.put("downStationId", downStationId + "");
        params.put("distance", distance + "");
        params.put("duration", duration + "");
        return params;
    }

    private void 최단_검색응답_역_검증(ExtractableResponse<Response> response, Long... stations) {
        assertThat(response.jsonPath().getList("stations.id", Long.class)).containsExactly(stations);
    }

    private void 최단_검색응답_거리_검증(ExtractableResponse<Response> response, int distance) {
        assertThat(response.jsonPath().getInt("distance")).isEqualTo(distance);
    }

    private void 최단_검색응답_시간_검증(ExtractableResponse<Response> response, int duration) {
        assertThat(response.jsonPath().getInt("duration")).isEqualTo(duration);
    }

    private void 최단_검색응답_이용요금_검증(ExtractableResponse<Response> response, Long totalFare) {
        assertThat(response.jsonPath().getLong("totalFare")).isEqualTo(totalFare);
    }

    private int 오km마다_100원_초과운임_계산(int distance) {
        int accessDistance = distance - 10;
        return (int) ((Math.ceil((accessDistance - 1) / 5) + 1) * 100);
    }
}
