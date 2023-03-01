package nextstep.subway.unit;

import nextstep.subway.domain.Fare;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class FareTest {

    @DisplayName("10km이내로 기본요금")
    @Test
    void basicfare(){
        Fare fare = new Fare(1);
        assertThat(fare.getFare()).isEqualTo(new BigDecimal(1250));
    }

    @DisplayName("10km이내로 기본요금")
    @Test
    void basicfare2(){
        Fare fare = new Fare(10);
        assertThat(fare.getFare()).isEqualTo(new BigDecimal(1250));
    }

    @DisplayName("예외 케이스 - 거리 0인 경우 에러 발생")
    @Test
    void fareException(){
        assertThatThrownBy(() -> new Fare(0).getFare()).isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("10km이상 50km이내로 기본요금+초과요금 (5km당 100원)")
    @Test
    void overfare(){
        Fare fare = new Fare(11);
        assertThat(fare.getFare()).isEqualTo(new BigDecimal(1350));
    }

    @DisplayName("10km이상 50km이내로 기본요금+초과요금 (5km당 100원)")
    @Test
    void overfare2(){
        Fare fare = new Fare(50);
        assertThat(fare.getFare()).isEqualTo(new BigDecimal(2050));
    }

    @DisplayName("50km초과로 기본요금+초과요금 (8km당 100원)")
    @Test
    void overfare3(){
        Fare fare = new Fare(51);
        assertThat(fare.getFare()).isEqualTo(new BigDecimal(1850));
    }
}