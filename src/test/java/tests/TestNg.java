package tests;


import io.qameta.allure.*;
import io.qameta.allure.testng.AllureTestNg;
import io.restassured.response.Response;
import model.Specifications;
import model.UserRequest;
import org.testng.Assert;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;


import static io.restassured.RestAssured.given;

@Listeners({AllureTestNg.class})
public class TestNg {
    private final static String BASE_URL = "https://reqres.in/";

    @Attachment(value = "Ответ API", type = "text/plain")
    private String logResponse(String responseData) {
        return responseData;
    }

    @DataProvider(name = "pageProvider")
    private Object[][] pageProvider() {
        return new Object[][] {
                {2, false},
                {99999999, true}
        };
    }

    @Test(dataProvider = "pageProvider")
    @Epic("Управление пользователями")
    @Feature("Страницы пользователей")
    @Story("Проверка списка пользователей")
    @Owner("AlexeyRDIO")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Проверка, что список пользователей на указанной странице соответствует ожиданию: пустой или нет.")
    @Parameters({"Страница", "Ожидание-пустота"})
    public void testUserListPages(int page, boolean expectEmpty) {
        // Получение результата запроса
        Response response = checkResponse(page);

        // Проверку на страницу
        checkPageNumber(response, page);

        // Проверка на размер списка пользователей
        checkSizeListUsers(response, page, expectEmpty ? "должен быть пуст" : "не должен быть пуст");
    }
    @Step("Получение страницы пользователей с номером {0}")
    public Response checkResponse(int page) {
        // Установка спецификаций запроса и ожидаемого ответа
        Specifications.installSpecification(Specifications.requestSpecification(BASE_URL),
                Specifications.responseSpecification(200));

        // Выполнение GET запроса на получение страницы пользователей
        Response response =
                given()
                        .queryParam("page", page)
                        .when()
                        .get("api/users")// get(BASE_URL + "?page=, page)
                        .then()
                        .log().all()
                        .extract().response();

        // Прикрепление ответа API к отчёту
        logResponse(response.asString());

        return response;
    }

    @Step("Проверка номера страницы")
    public void checkPageNumber(Response response, int expectedPage) {
        int actualPage = response.jsonPath().getInt("page");
        Assert.assertEquals(actualPage, expectedPage, "Запрошенная страница не соответствует полученной");
    }

    @Step("Проверка, что список пользователей {status} на странице {page}")
    public void checkSizeListUsers(Response response, int page, String status) {
        // Получаем размер списка пользователей
        int usersSize = response.jsonPath().getList("data").size();

        // Проверяем наличие или отсутствие пользователей в соответствии с expectEmpty
        if (Objects.equals(status, "должен быть пуст")) {
            Assert.assertEquals(usersSize, 0, "Список пользователей не пуст на страниц " + page);
        } else {
            Assert.assertTrue(usersSize > 0, "Список пользователей пуст на странице " + page);
        }
    }

    @DataProvider(name = "getDataProvider")
    public Object[][] getUserData() {
        return new Object[][] {
                {2, 200},
                {23, 404}
        };
    }

    @Test(dataProvider = "getDataProvider")
    @Epic("Управление пользователями")
    @Feature("Пользователь")
    @Story("Получение пользователя")
    @Owner("AlexeyRDIO")
    @Description("Проверка, что данные пользователя получены или нет.")
    @Parameters({"Идентификатор", "Ожидаемый-статус-код"})
    public void testGetUserById(int id, int expectedStatusCode) {
        // Установка спецификаций запроса и ожидаемого ответа
        Specifications.installSpecification(Specifications.requestSpecification(BASE_URL),
                Specifications.responseSpecification(expectedStatusCode));

        // Выполнение GET запроса на получения пользователя по id
        Response response = given()
                .pathParam("id", id)
                .when()
                .get("api/users/{id}")
                .then()
                .log().all()
                .extract().response();

        // Прикрепление ответа API к отчёту
        logResponse(response.asString());

        // Только для успешного статуса кода проверяем ID
        if (expectedStatusCode == 200) {
            Integer actualId = response.jsonPath().getInt("data.id");
            Assert.assertEquals(actualId, id, "Id пользователя не совпадают");
        }
    }

    @DataProvider(name = "createDataProvider")
    public Object[][] createUserData() {
        return new Object[][] {
                {new UserRequest("Alex", "Tester")}
        };
    }

    @Test(dataProvider = "createDataProvider")
    @Epic("Управление пользователями")
    @Feature("Пользователь")
    @Story("Создание пользователя")
    @Owner("AlexeyRDIO")
    @Description("Проверка, что пользователь создан.")
    @Parameters({"Пользовательские-данные"})
    public void testCreateUser(UserRequest user) {
        // Установка спецификаций запроса и ожидаемого ответа
        Specifications.installSpecification(Specifications.requestSpecification(BASE_URL),
                Specifications.responseSpecification(201));

        // Фиксируем временные рамки выполнения запроса
        OffsetDateTime beforeRequest = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1);

        // Выполнение POST запроса для создания пользователя
        Response response = given()
                .body(user)
                .when()
                .post("api/users/")
                .then()
                .log().all()
                .extract().response();

        // Прикрепление ответа API к отчёту
        logResponse(response.asString());

        // Проверка на соответствие формата
        Assert.assertTrue(response.jsonPath().getString("createdAt").matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z"),
                "Не совпадает формат времени");

        OffsetDateTime createdAt = OffsetDateTime.parse(response.jsonPath().getString("createdAt"));
        OffsetDateTime afterRequest = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1);


        // Проверка корректности метки времени создания пользователя
        Assert.assertTrue(!createdAt.isBefore(beforeRequest) && !createdAt.isAfter(afterRequest),
                "Дата создания пользователя выходит за пределы ожидаемого временного окна");

        // Проверка соответствия имени и должности пользователя отправленным данным
        Assert.assertEquals(response.jsonPath().getString("name"), user.getName(), "Имя пользователя не соответствует");
        Assert.assertEquals(response.jsonPath().getString("job"), user.getJob(), "Должность пользователя не соответствует");
    }

    @DataProvider(name = "updateDataProvider")
    public Object[][] updateUserData() {
        return new Object[][] {
                {new UserRequest("Alex", "Tester"), 2},
                {new UserRequest("Alex", "Tester2"), 2}
        };
    }

    @Test(dataProvider = "updateDataProvider")
    @Epic("Управление пользователями")
    @Feature("Пользователь")
    @Story("Изменения пользователя")
    @Owner("AlexeyRDIO")
    @Description("Проверка, что данные пользователя обновлены.")
    @Parameters({"Пользовательские-данные", "Идентификатор"})
    public void testUpdateUser(UserRequest user, int id) {
        // Установка спецификаций запроса и ожидаемого ответа
        Specifications.installSpecification(Specifications.requestSpecification(BASE_URL),
                Specifications.responseSpecification(200));

        // Фиксируем временные рамки выполнения запроса
        OffsetDateTime beforeRequest = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1);

        // Выполнение PUT запроса для обновление пользовательских данных
        Response response = given()
                .body(user)
                .pathParam("id", id)
                .when()
                .put("api/users/{id}")
                .then()
                .log().all()
                .extract().response();

        // Прикрепление ответа API к отчёту
        logResponse(response.asString());

        // Проверка на соответствие формата
        Assert.assertTrue(response.jsonPath().getString("updatedAt").matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z"),
                "Не совпадает формат времени");

        OffsetDateTime updatedAt = OffsetDateTime.parse(response.jsonPath().getString("updatedAt"));
        OffsetDateTime afterRequest = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1);

        // Проверяем, что updatedAt лежит между временем до и после запроса
        Assert.assertTrue(!updatedAt.isBefore(beforeRequest) && !updatedAt.isAfter(afterRequest),
                "Время обновления пользователя не соответствует ожидаемому временному окну запроса");

        // Проверка соответствия имени и должности пользователя отправленным данным
        Assert.assertEquals(response.jsonPath().getString("name"), user.getName(), "Имя пользователя не соответствует");
        Assert.assertEquals(response.jsonPath().getString("job"), user.getJob(), "Должность пользователя не соответствует");
    }


    @DataProvider(name = "deleteUserProvider")
    public Object[][] deleteUserData() {
        return new Object[][] {
                {2},
                {3},
                {4}
        };
    }

    @Test(dataProvider = "deleteUserProvider")
    @Epic("Управление пользователями")
    @Feature("Пользователь")
    @Story("Удаления пользователя")
    @Owner("AlexeyRDIO")
    @Description("Проверка, что данные пользователя удалены.")
    @Parameters({"Идентификатор"})
    public void testDeleteUser(int id) {
        // Установка спецификации запроса и ожидаемого ответа
        Specifications.installSpecification(Specifications.requestSpecification(BASE_URL),
                Specifications.responseSpecification(204));

        // Выполнение DELETE запроса для удаления пользовательских данных
        Response response = given()
                .pathParam("id", id)
                .when()
                .delete("api/users/{id}")
                .then()
                .log().all()
                .extract().response();

        // Прикрепление ответа API к отчёту
        logResponse(response.asString());

        // Проверяем, что тело ответа действительно пусто после удаления
        Assert.assertTrue(response.getBody().asString().isEmpty(), "Тело ответа не пустое после попытки удаления.");
    }
}


