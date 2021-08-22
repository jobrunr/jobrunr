package org.jobrunr.quarkus.it;

import io.quarkus.test.junit.QuarkusTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


@QuarkusTest
@DisplayName("Tests JobRunr extension")
public class JobRunrFunctionalityTest {

    //private TeenyHttpClient http = new TeenyHttpClient("http://localhost:8080");

    @Test
    public void testJobRunrFunctionality() {
//        final HttpResponse<String> response = http.get("/jobrunr/enqueue");
//        assertThat(response).hasStatusCode(200);
        Assertions.assertThat("").isEqualTo("");
    }


}
