package umm3601.user;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
//import static com.mongodb.client.model.Filters.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.javalin.validation.ValidationException;
import io.javalin.validation.Validator;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import io.javalin.json.JavalinJackson;

/**
 * Tests the logic of the UserController
 *
 * @throws IOException
 */
// The tests here include a ton of "magic numbers" (numeric constants).
// It wasn't clear to me that giving all of them names would actually
// help things. The fact that it wasn't obvious what to call some
// of them says a lot. Maybe what this ultimately means is that
// these tests can/should be restructured so the constants (there are
// also a lot of "magic strings" that Checkstyle doesn't actually
// flag as a problem) make more sense.
@SuppressWarnings({ "MagicNumber" })
public class UserControllerSpec {

  // Mock requests and responses that will be reset in `setupEach()`
  // and then (re)used in each of the tests.
  //private MockHttpServletRequest mockReq = new MockHttpServletRequest();
  //private MockHttpServletResponse mockRes = new MockHttpServletResponse();

  // An instance of the controller we're testing that is prepared in
  // `setupEach()`, and then exercised in the various tests below.
  private UserController userController;

  // A Mongo object ID that is initialized in `setupEach()` and used
  // in a few of the tests. It isn't used all that often, though,
  // which suggests that maybe we should extract the tests that
  // care about it into their own spec file?
  private ObjectId samsId;

  // The client and database that will be used
  // for all the tests in this spec file.
  private static MongoClient mongoClient;
  private static MongoDatabase db;

  // Used to translate between JSON and POJOs.
  private static JavalinJackson javalinJackson = new JavalinJackson();

  @Mock
  private Context ctx;

  @Captor
  private ArgumentCaptor<ArrayList<User>> userArrayListCaptor;

  /**
   * Sets up (the connection to the) DB once; that connection and DB will
   * then be (re)used for all the tests, and closed in the `teardown()`
   * method. It's somewhat expensive to establish a connection to the
   * database, and there are usually limits to how many connections
   * a database will support at once. Limiting ourselves to a single
   * connection that will be shared across all the tests in this spec
   * file helps both speed things up and reduce the load on the DB
   * engine.
   */
  @BeforeAll
  public static void setupAll() {
    String mongoAddr = System.getenv().getOrDefault("MONGO_ADDR", "localhost");

    mongoClient = MongoClients.create(
        MongoClientSettings.builder()
            .applyToClusterSettings(builder -> builder.hosts(Arrays.asList(new ServerAddress(mongoAddr))))
            .build()
    );
    db = mongoClient.getDatabase("test");
  }

  @AfterAll
  public static void teardown() {
    db.drop();
    mongoClient.close();
  }

  @BeforeEach
  public void setupEach() throws IOException {
    // Reset our mock request and response objects
    //mockReq.resetAll();
    //mockRes.resetAll();
    MockitoAnnotations.openMocks(this);

    // Setup database
    MongoCollection<Document> userDocuments = db.getCollection("users");
    userDocuments.drop();
    List<Document> testUsers = new ArrayList<>();
    testUsers.add(
        new Document()
            .append("name", "Chris")
            .append("age", 25)
            .append("company", "UMM")
            .append("email", "chris@this.that")
            .append("role", "admin")
            .append("avatar", "https://gravatar.com/avatar/8c9616d6cc5de638ea6920fb5d65fc6c?d=identicon"));
    testUsers.add(
        new Document()
            .append("name", "Pat")
            .append("age", 37)
            .append("company", "IBM")
            .append("email", "pat@something.com")
            .append("role", "editor")
            .append("avatar", "https://gravatar.com/avatar/b42a11826c3bde672bce7e06ad729d44?d=identicon"));
    testUsers.add(
        new Document()
            .append("name", "Jamie")
            .append("age", 37)
            .append("company", "OHMNET")
            .append("email", "jamie@frogs.com")
            .append("role", "viewer")
            .append("avatar", "https://gravatar.com/avatar/d4a6c71dd9470ad4cf58f78c100258bf?d=identicon"));

    samsId = new ObjectId();
    Document sam = new Document()
        .append("_id", samsId)
        .append("name", "Sam")
        .append("age", 45)
        .append("company", "OHMNET")
        .append("email", "sam@frogs.com")
        .append("role", "viewer")
        .append("avatar", "https://gravatar.com/avatar/08b7610b558a4cbbd20ae99072801f4d?d=identicon");

    userDocuments.insertMany(testUsers);
    userDocuments.insertOne(sam);

    userController = new UserController(db);
  }

  @Test
  public void canGetAllUsers() throws IOException {
    // When something asks the (mocked) context for the queryParamMap,
    // it will return an empty map (since there are no query params in this case where we want all users)
    when(ctx.queryParamMap()).thenReturn(Collections.emptyMap());

    // Now, go ahead and ask the userController to getUsers
    // (which will, indeed, ask the context for its queryParamMap)
    userController.getUsers(ctx);

    // We are going to capture an argument to a function, and the type of that argument will be
    // of type ArrayList<User> (we said so earlier using a Mockito annotation like this):
    // @Captor
    // private ArgumentCaptor<ArrayList<User>> userArrayListCaptor;
    // We only want to declare that captor once and let the annotation
    // help us accomplish reassignment of the value for the captor
    // We reset the values of our annotated declarations using the command
    // `MockitoAnnotations.openMocks(this);` in our @BeforeEach

    // Specifically, we want to pay attention to the ArrayList<User> that is passed as input
    // when ctx.json is called --- what is the argument that was passed? We capture it and can refer to it later
    verify(ctx).json(userArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    // Check that the database collection holds the same number of documents as the size of the captured List<User>
    assertEquals(db.getCollection("users").countDocuments(), userArrayListCaptor.getValue().size());
  }

  @Test
  public void canGetUsersWithAge37() throws IOException {
    // Add a query param map to the context that maps "age" to "37".
    Map<String, List<String>> queryParams = new HashMap<>();
    queryParams.put("age", Arrays.asList(new String[] {"37"}));
    when(ctx.queryParamMap()).thenReturn(queryParams);
    when(ctx.queryParamAsClass("age", Integer.class))
      .thenReturn(Validator.create(Integer.class, "37", "age"));

    userController.getUsers(ctx);

    verify(ctx).json(userArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);
    assertEquals(2, userArrayListCaptor.getValue().size());
  }

  /**
   * Test that if the user sends a request with an illegal value in
   * the age field (i.e., something that can't be parsed to a number)
   * we get a reasonable error code back.
   */
  @Test
  public void respondsAppropriatelyToNonNumericAge() {
    Map<String, List<String>> queryParams = new HashMap<>();
    queryParams.put("age", Arrays.asList(new String[] {"bad"}));
    when(ctx.queryParamMap()).thenReturn(queryParams);
    when(ctx.queryParamAsClass("age", Integer.class))
      .thenReturn(Validator.create(Integer.class, "bad", "age"));

    // This should now throw a `ValidationException` because
    // our request has an age that can't be parsed to a number,
    // but I don't yet know how to make the message be anything specific
    assertThrows(ValidationException.class, () -> {
      userController.getUsers(ctx);
    });
  }

  @Test
  public void canGetUsersWithCompany() throws IOException {
    Map<String, List<String>> queryParams = new HashMap<>();
    queryParams.put("company", Arrays.asList(new String[] {"OHMNET"}));
    when(ctx.queryParamMap()).thenReturn(queryParams);
    when(ctx.queryParam("company")).thenReturn("OHMNET");

    userController.getUsers(ctx);

    verify(ctx).json(userArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    // Confirm that all the users passed to `json` work for OHMNET.
    for (User user : userArrayListCaptor.getValue()) {
      assertEquals("OHMNET", user.company);
    }
  }

  @Test
  public void getUsersByRole() throws IOException {
    Map<String, List<String>> queryParams = new HashMap<>();
    queryParams.put("role", Arrays.asList(new String[] {"viewer"}));
    when(ctx.queryParamMap()).thenReturn(queryParams);
    when(ctx.queryParamAsClass("role", String.class))
      .thenReturn(Validator.create(String.class, "viewer", "role"));

    userController.getUsers(ctx);

    verify(ctx).json(userArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);
    assertEquals(2, userArrayListCaptor.getValue().size());
  }

  // @Test
  // public void getUsersByCompanyAndAge() throws IOException {
  //   mockReq.setQueryString("company=OHMNET&age=37");
  //   Context ctx = mockContext("api/users");

  //   userController.getUsers(ctx);
  //   User[] resultUsers = returnedUsers(ctx);

  //   assertEquals(HttpURLConnection.HTTP_OK, mockRes.getStatus());
  //   assertEquals(1, resultUsers.length);
  //   for (User user : resultUsers) {
  //     assertEquals("OHMNET", user.company);
  //     assertEquals(37, user.age);
  //   }
  // }

  @Test
  public void getUserWithExistentId() throws IOException {
    String id = samsId.toHexString();
    when(ctx.pathParam("id")).thenReturn(id);

    userController.getUser(ctx);

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

    verify(ctx).json(userCaptor.capture());
    verify(ctx).status(HttpStatus.OK);
    assertEquals("Sam", userCaptor.getValue().name);
    assertEquals(samsId.toHexString(), userCaptor.getValue()._id);
  }

  @Test
  public void getUserWithBadId() throws IOException {
    when(ctx.pathParam("id")).thenReturn("bad");

    Throwable exception = assertThrows(BadRequestResponse.class, () -> {
      userController.getUser(ctx);
    });

    assertEquals("The requested user id wasn't a legal Mongo Object ID.", exception.getMessage());
  }

  @Test
  public void getUserWithNonexistentId() throws IOException {
    String id = "588935f5c668650dc77df581";
    when(ctx.pathParam("id")).thenReturn(id);

    Throwable exception = assertThrows(NotFoundResponse.class, () -> {
      userController.getUser(ctx);
    });

    assertEquals("The requested user was not found", exception.getMessage());
  }

  // @Test
  // public void addUser() throws IOException {

  //   // I could use help understanding how the JsonMapper might be used
  //   BodyValidator<User> bv = new BodyValidator<>(null, User.class, new JsonMapper() {
  //   });

  //   // attempting to make a body validator, but really this is just a bunch of separate validators right now
  //   Validator<String> validateName = Validator.create(String.class, "Test User", "name");
  //   Validator<Integer> validateAge = Validator.create(Integer.class, "25", "age");
  //   Validator<String> validateCompany = Validator.create(String.class, "testers", "company");
  //   Validator<String> validateEmail = Validator.create(String.class, "test@example.com", "email");
  //   Validator<String> validateRole = Validator.create(String.class, "viewer", "role");
  //   Validator<?>[] validators= {validateName, validateAge, validateCompany, validateEmail, validateRole};

  //   when(ctx.bodyValidator(User.class)).thenReturn(bv);
  //   when(ctx.formParamMap()).thenReturn(null);

  //   userController.addNewUser(ctx);

  //   ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
  //   verify(ctx).json(userCaptor.capture());

  //   // Our status should be 201, i.e., our new user was successfully created.
  //   verify(ctx).status(HttpStatus.CREATED);

  //   // Verify that the user was added to the database with the correct ID
  //   //Document addedUser = db.getCollection("users").find(eq("_id", new ObjectId(userCaptor.getValue()._id))).first();

  //   // Successfully adding the user should return the newly generated, non-empty MongoDB ID for that user.
  //   assertNotEquals("", userCaptor.getValue()._id);
  //   assertEquals("Test User", userCaptor.getValue().name);
  //   assertEquals(25, userCaptor.getValue().age);
  //   assertEquals("testers", userCaptor.getValue().company);
  //   assertEquals("test@example.com", userCaptor.getValue().email);
  //   assertEquals("viewer", userCaptor.getValue().role);
  //   assertNotNull(userCaptor.getValue().avatar);
  // }

  // @Test
  // public void addInvalidEmailUser() throws IOException {
  //   String testNewUser = "{"
  //       + "\"name\": \"Test User\","
  //       + "\"age\": 25,"
  //       + "\"company\": \"testers\","
  //       + "\"email\": \"invalidemail\","
  //       + "\"role\": \"viewer\""
  //       + "}";
  //   mockReq.setBodyContent(testNewUser);
  //   mockReq.setMethod("POST");

  //   Context ctx = mockContext("api/users");

  //   assertThrows(ValidationException.class, () -> {
  //     userController.addNewUser(ctx);
  //   });
  // }

  // @Test
  // public void addInvalidAgeUser() throws IOException {
  //   String testNewUser = "{"
  //       + "\"name\": \"Test User\","
  //       + "\"age\": \"notanumber\","
  //       + "\"company\": \"testers\","
  //       + "\"email\": \"test@example.com\","
  //       + "\"role\": \"viewer\""
  //       + "}";
  //   mockReq.setBodyContent(testNewUser);
  //   mockReq.setMethod("POST");

  //   Context ctx = mockContext("api/users");

  //   assertThrows(ValidationException.class, () -> {
  //     userController.addNewUser(ctx);
  //   });
  // }

  // @Test
  // public void add0AgeUser() throws IOException {
  //   String testNewUser = "{"
  //       + "\"name\": \"Test User\","
  //       + "\"age\": 0,"
  //       + "\"company\": \"testers\","
  //       + "\"email\": \"test@example.com\","
  //       + "\"role\": \"viewer\""
  //       + "}";
  //   mockReq.setBodyContent(testNewUser);
  //   mockReq.setMethod("POST");

  //   Context ctx = mockContext("api/users");

  //   assertThrows(ValidationException.class, () -> {
  //     userController.addNewUser(ctx);
  //   });
  // }

  // @Test
  // public void addNullNameUser() throws IOException {
  //   String testNewUser = "{"
  //       + "\"age\": 25,"
  //       + "\"company\": \"testers\","
  //       + "\"email\": \"test@example.com\","
  //       + "\"role\": \"viewer\""
  //       + "}";
  //   mockReq.setBodyContent(testNewUser);
  //   mockReq.setMethod("POST");

  //   Context ctx = mockContext("api/users");

  //   assertThrows(ValidationException.class, () -> {
  //     userController.addNewUser(ctx);
  //   });
  // }

  // @Test
  // public void addInvalidNameUser() throws IOException {
  //   String testNewUser = "{"
  //       + "\"name\": \"\","
  //       + "\"age\": 25,"
  //       + "\"company\": \"testers\","
  //       + "\"email\": \"test@example.com\","
  //       + "\"role\": \"viewer\""
  //       + "}";
  //   mockReq.setBodyContent(testNewUser);
  //   mockReq.setMethod("POST");

  //   Context ctx = mockContext("api/users");

  //   assertThrows(ValidationException.class, () -> {
  //     userController.addNewUser(ctx);
  //   });
  // }

  // @Test
  // public void addInvalidRoleUser() throws IOException {
  //   String testNewUser = "{"
  //       + "\"name\": \"Test User\","
  //       + "\"age\": 25,"
  //       + "\"company\": \"testers\","
  //       + "\"email\": \"test@example.com\","
  //       + "\"role\": \"invalidrole\""
  //       + "}";
  //   mockReq.setBodyContent(testNewUser);
  //   mockReq.setMethod("POST");

  //   Context ctx = mockContext("api/users");

  //   assertThrows(ValidationException.class, () -> {
  //     userController.addNewUser(ctx);
  //   });
  // }

  // @Test
  // public void addNullCompanyUser() throws IOException {
  //   String testNewUser = "{"
  //       + "\"name\": \"Test User\","
  //       + "\"age\": 25,"
  //       + "\"email\": \"test@example.com\","
  //       + "\"role\": \"viewer\""
  //       + "}";
  //   mockReq.setBodyContent(testNewUser);
  //   mockReq.setMethod("POST");

  //   Context ctx = mockContext("api/users");

  //   assertThrows(ValidationException.class, () -> {
  //     userController.addNewUser(ctx);
  //   });
  // }

  // @Test
  // public void addInvalidCompanyUser() throws IOException {
  //   String testNewUser = "{"
  //       + "\"name\": \"\","
  //       + "\"age\": 25,"
  //       + "\"company\": \"\","
  //       + "\"email\": \"test@example.com\","
  //       + "\"role\": \"viewer\""
  //       + "}";
  //   mockReq.setBodyContent(testNewUser);
  //   mockReq.setMethod("POST");

  //   Context ctx = mockContext("api/users");

  //   assertThrows(ValidationException.class, () -> {
  //     userController.addNewUser(ctx);
  //   });
  // }

  // @Test
  // public void deleteUser() throws IOException {
  //   String testID = samsId.toHexString();

  //   // User exists before deletion
  //   assertEquals(1, db.getCollection("users").countDocuments(eq("_id", new ObjectId(testID))));

  //   Context ctx = mockContext("api/users/{id}", Map.of("id", testID));

  //   userController.deleteUser(ctx);

  //   assertEquals(HttpURLConnection.HTTP_OK, mockRes.getStatus());

  //   // User is no longer in the database
  //   assertEquals(0, db.getCollection("users").countDocuments(eq("_id", new ObjectId(testID))));
  // }

}
