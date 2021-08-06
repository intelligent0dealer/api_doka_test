import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

import static io.restassured.RestAssured.given;


public class ApiMethods {
    RequestSpecification requestSpec = new RequestSpecBuilder()
            .setBaseUri("https://regions-test.2gis.com/1.0/regions") // proto host
            .setContentType(ContentType.URLENC)
            .build();

    private void checkResponse(Response response) {
        String body = response.getBody().asString();

        Assert.assertEquals(response.statusCode(), 200, "eq");
        Assert.assertTrue(isJSONValid(body), String.format("Response body is not json: `%s`.", body));
    }

    private boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    @Test
    void notEmptyError() {
        Response response =
                given(requestSpec)
                        .queryParam("q", "1")
                        .when()
                        .get();
        response.then().body("error", Matchers.notNullValue());
        this.checkResponse(response);
    }

    @Test
    void pageSize() {
        int pageSize = 5;
        Response response =
                given(requestSpec)
                        .queryParam("page_size", pageSize)
                        .when()
                        .get();
        List<Object> items = response.body().jsonPath().getList("items");
        Assert.assertEquals(items.size(), pageSize);
    }

    @Test
    void defaultPageSize() {
        int defaultPageSize = 15;
        Response response =
                given(requestSpec)
                        .when()
                        .get();
        List<Object> items = response.body().jsonPath().getList("items");
        Assert.assertEquals(items.size(), defaultPageSize);
    }

    @Test
    void countryCode() {
        String ruCountryCode = "ru";
        Response response =
                given(requestSpec)
                        .queryParam("country_code", ruCountryCode)
                        .when()
                        .get();
        List<String> codes = response.body().jsonPath().getList("items.country.code.");
        Assert.assertTrue(codes.stream().allMatch(s -> s.equals(ruCountryCode)));
    }

    @Test
    void totalElements() {
        int totalElements = 22;
        int pageSize = 10;
        Response responseOne =
                given(requestSpec)
                        .queryParam("page_size", pageSize)
                        .queryParam("page", 1)
                        .when()
                        .get();
        checkResponse(responseOne);
        Response responseTwo =
                given(requestSpec)
                        .queryParam("page_size", pageSize)
                        .queryParam("page", 2)
                        .when()
                        .get();
        checkResponse(responseTwo);
        Response responseThree =
                given(requestSpec)
                        .queryParam("page_size", pageSize)
                        .queryParam("page", 3)
                        .when()
                        .get();
        checkResponse(responseThree);
        int firstPageSize = responseOne.body().jsonPath().getList("items").size();
        int secondPageSize = responseTwo.body().jsonPath().getList("items").size();
        int thirdPageSize = responseThree.body().jsonPath().getList("items").size();
        Assert.assertEquals(firstPageSize + secondPageSize + thirdPageSize, totalElements);
    }
}
