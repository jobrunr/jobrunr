package org.jobrunr.dashboard.server.http.client;

import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.net.http.HttpResponse;

import static org.jobrunr.JobRunrAssertions.contentOfResource;

public class HttpResponseAssert extends AbstractAssert<HttpResponseAssert, HttpResponse> {

    private HttpResponseAssert(HttpResponse httpResponse) {
        super(httpResponse, HttpResponseAssert.class);
    }

    public static HttpResponseAssert assertThat(HttpResponse httpResponse) {
        return new HttpResponseAssert(httpResponse);
    }

    public HttpResponseAssert hasStatusCode(int statusCode) {
        Assertions.assertThat(actual.statusCode()).isEqualTo(statusCode);
        return this;
    }

    public HttpResponseAssert hasSameJsonBodyAsResource(String resourceName) {
        hasJsonBody(contentOfResource(resourceName));
        return this;
    }

    public HttpResponseAssert hasJsonBody(String bodyAsString) {
        final String actualResponseAsString = actual.body().toString();
        JsonAssertions.assertThatJson(actualResponseAsString).isEqualTo(bodyAsString);
        return this;
    }

    public HttpResponseAssert hasJsonBody(JsonAssertions.JsonAssertionCallback... callbacks) {
        final String actualResponseAsString = actual.body().toString();
        JsonAssertions.assertThatJson(actualResponseAsString, callbacks);
        return this;
    }

    public HttpResponseAssert hasBodyStartingWith(String expected) {
        final String actualResponseAsString = actual.body().toString();
        Assertions.assertThat(actualResponseAsString).startsWith(expected);
        return this;
    }

    public HttpResponseAssert hasBodyContaining(String... expected) {
        final String actualResponseAsString = actual.body().toString();
        for (String string : expected) {
            Assertions.assertThat(actualResponseAsString).contains(string);
        }
        return this;
    }
}
