package model;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;


public class Specifications {
    public static RequestSpecification requestSpecification(String url) {
        return new RequestSpecBuilder()
                .setBaseUri(url)
                .setContentType(ContentType.JSON)
                .build();
    }

    public static ResponseSpecification responseSpecification(Integer expectedCode) {
        return new ResponseSpecBuilder()
                .expectStatusCode(expectedCode)
                .build();
    }
    public static void installSpecification(RequestSpecification requestSpecification,
                                     ResponseSpecification responseSpecification) {
        RestAssured.requestSpecification = requestSpecification;
        RestAssured.responseSpecification = responseSpecification;
    }

}
