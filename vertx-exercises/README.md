3.4. Exercise 1 - Vert.x applications are Java applications
In this first exercise, let’s start from the very beginning:

Create an instance of Vert.x

Start an HTTP server sending greetings

Open the vertx-exercises/src/main/java/io/vertx/workshop/exercise/Exercise1.java file. Notice that this exercise is just a main method.

Follow the instruction located in the class. Use your IDE to run the application (by running the main method). If your code is right, you should see a "Hello" message when you open your browser on http://localhost:8080.

Don’t forget to stop the application between runs.
    // 1 - Create the Vert.x instance using Vertx.vertx (use io.vertx.core.Vertx)
    Vertx vertx = Vertx.vertx();

    // 2 - Create a HTTP server using the `createHttpServer` method. Set a request handler doing:
    // `req.response().end("hello")`
    // Call the listen method with `8080` as parameter.

    vertx.createHttpServer()
        .requestHandler(req -> req.response().end("hello"))
        .listen(8080);
3.5. Exercise 2 - Using verticles
While using a main method is nice and simple, it does not necessarily scale. When your code base grows, you need a better way to structure your code. For this, Vert.x provides verticles - a simple agent-like model. Verticles are single-threaded classes interacting using asynchronous messages.

Open the vertx-exercises/src/main/java/io/vertx/workshop/exercise/Exercise2.java file. In the main method, deploy the Exercise2Verticle. Then, implement the missing functionnality in the verticle class (Exercise2Verticle.java).

Run and check the result as in the previous exercise. Emit the request several time in a row to check that your verticle is always executed by the same thread.

    // Exercise2.java
    // --------------

    // 1 - Create the Vert.x instance using Vertx.vertx (use io.vertx.core.Vertx)
    Vertx vertx = Vertx.vertx();

    // 2 - Deploy the `Exercise2Verticle` verticle using: vertx.deployVerticle(className);
    vertx.deployVerticle(Exercise2Verticle.class.getName());

    // -----------------------------------------------------

    // Exercise2Verticle.java
    // ----------------------

    vertx.createHttpServer()
        .requestHandler(req -> req.response().end(Thread.currentThread().getName()))
        .listen(8080);
3.6. Exercise 3 - Do not block the event loop
In this exercise, we are going to voluntarily break the golden rule - block the event loop.

In the Exercise2Verticle class, call sleep before writing the result into the response.

When running this code and calling the server, you can see that the requests are not served in a timely fashion anymore. With the thread being blocked, it can’t serve the subsequent requests before completing the first one.

Also notice the output in the console, Vert.x detects that the event loop has been blocked and starts yelling …​

vertx.createHttpServer()
    .requestHandler(req -> {
        sleep();
        req.response().end(Thread.currentThread().getName());
    })
    .listen(8080);
You may wonder how you will be able to call blocking code. Don’t worry, Vert.x provides several ways to do so. A construct named executeBlocking and a type of verticle named worker are not executed on the event loop.

3.7. Exercise 4 - Sending and receiving messages
Verticles are a great way to structure your code, but how do verticles interact? They use the event bus to send and receive messages. Let’s see how it works. Exercise 4 is composed of 2 verticles: a sender and a receiver. The sender emits a greeting message periodically. The receiver prints this message to the console. As JSON is a very common format in Vert.x applications, this exercise also introduces the JsonObject, a facility to create and manipulate JSON structures.

First, open the vertx-exercises/src/main/java/io/vertx/workshop/exercise/Exercise4SenderVerticle.java file and follow the instructions to send a greeting message every 2 seconds. This message is a JSON structure: {"message":"hello"}. Do not hesitate to extend it if you want.

// Retrieve the event bus
EventBus eventBus = vertx.eventBus();

// Execute the given handler every 2000 ms
vertx.setPeriodic(2000, l -> {
    // Use the eventBus() method to retrieve the event bus and send a "{"message":hello"} JSON message on the
    // "greetings" address.

    // 1 - Create the JSON object using the JsonObject class, and `put` the 'message':'hello' entry
    JsonObject json = new JsonObject().put("message", "hello");

    // 2 - Use the `send` method of the event bus to _send_ the message. Messages sent with the `send` method
    // are received by a single consumer. Messages sent with the `publish` method are received by all
    // registered consumers.
    eventBus.send("greetings", json);
});
Then, open the vertx-exercises/src/main/java/io/vertx/workshop/exercise/Exercise4ReceiverVerticle.java file and follow the instructions to receive the messages sent by the other verticle. To achieve this, register a consumer on the greetings address and implement the Handler to process the received messages.

Use vertx.eventBus().<JsonObject>consumer(…​) to indicate to the compiler that you expect a JSON message.
// Retrieve the event bus and register a consumer on the "greetings" address. For each message, print it on
// the console. You can retrieve the message body using `body()`. Use the method `encodePrettily`
// on the retrieved Json body to print it nicely.
vertx.eventBus().<JsonObject>consumer("greetings", msg -> {
    System.out.println(msg.body().encodePrettily());
});
To launch this exercise, use the io.vertx.workshop.exercise.Exercise4#main method. If implemented correctly you would see the greeting messages printed on the console. Don’t forget to stop the application before switching to the next exercise.

3.8. Exercise 5 - Request Reply and Composing actions
Let’s now mix the HTTP server and the event bus. The first verticle creates an HTTP server, but to respond to the request, it sends a message to another verticle and waits for a reply. This reply is used as response to the HTTP request. This introduces the request-reply delivery mechanism of the event bus. This exercice is composed of a _main` class (io.vertx.workshop.exercise.Exercise5) and two verticles: io.vertx.workshop.exercise .Exercise5HttpVerticle and io.vertx.workshop.exercise.Exercise5ProcessorVerticle.

Let’s start with the io.vertx.workshop.exercise.Exercise5ProcessorVerticle class. Follow the instructions to receive messages from the greetings and reply to the received messages.

EventBus eventBus = vertx.eventBus();

// Register a consumer and call the `reply` method with a JSON object containing the greeting message. ~
// parameter is passed in the incoming message body (a name). For example, if the incoming message is the
// String "vert.x", the reply contains: `{"message" : "hello vert.x"}`.
// Unlike the previous exercise, the incoming message has a `String` body.
eventBus.<String>consumer("greetings", msg -> {
    JsonObject json = new JsonObject().put("message", "hello " + msg.body());
    msg.reply(json);
});
Then, edit the vertx-exercises/src/main/java/io/vertx/workshop/exercise/Exercise5HttpVerticle.java file. In this verticle, we need to create an HTTP server. The requestHandler extracts the query parameter name (or use world if not set), sends a message on the event bus, and writes the HTTP response when the reply from the event bus is received.

vertx.createHttpServer()
    .requestHandler(req -> {

        // 1 - Retrieve the `name` (query) parameter, set it to `world if null`. You can retrieve the
        // parameter using: `req.getParam()`
        String name = req.getParam("name");
        if (name == null) { name = "world"; }

        // 2 - Send a message on the event bus using the `send` method. Pass a reply handler receiving the
        // response. As the expected object is a Json structure, you can use `vertx.eventBus()
        // .<JsonObject>send(...`).
        // In the reply handler, you receive an `AsyncResult`. This structure describes the outcome from an
        // asynchronous operation: a success (and a result) or a failure (and a cause). If it's a failure
        // (check with the `failed` method), write a 500 HTTP response with the cause (`cause.getMessage()`) as
        // payload. On success, write the body into the HTTP response.
        vertx.eventBus().<JsonObject>send("greetings", name, reply -> {
            if (reply.failed()) {
                req.response().setStatusCode(500).end(reply.cause().getMessage());
            } else {
                req.response().end(reply.result().body().encode());
            }
        });
    })
    .listen(8080);
Launch the exercise using the Exercise5#main method. Check the result by opening your browser to http://localhost:8080 (should display hello world) and http://localhost:8080?name=vert.x (should display hello vert.x).

This exercise shows how to compose asynchronous actions and how to use the AsyncResult structure. But as you can imagine, it quicksly ends up with lots of callbacks. Let’s move to the next example to show how RX Java can help in taming the asynchronous coordination.

3.9. Exercise 6 - Use RX Java 2
This exercise is a rewrite of the previous one using RX Java 2. As mentioned above, RX Java is an implementation of the reactive programming principles for Java. With this development model, we manipulate streams (called Flowable, Observable, Maybe, Single or Completable depending on the number of items and their characteristics). RX Java provides a lots of operators to compose streams together and so write asynchronous orchestration easily. This exercise is a very basic introduction to RX Java.

Open the vertx-exercises/src/main/java/io/vertx/workshop/exercise/Exercise6HttpVerticle.java file and follow the instructions. Notice the import statements containing the reactivex package. This package contains the RX-ified Vert.x API.

 vertx.createHttpServer()
    .requestHandler(req -> {
        String name = req.getParam("name");
        if (name == null) {
            name = "world";
        }

        // Send a message on the event bus using the `send` method. Pass a reply handler receiving the
        // response. As the expected object is a Json structure, you can use `vertx.eventBus()
        // .<JsonObject>send(...`).
        // Unlike in the previous exercise, we use the `rxSend` method to retrieve a `Single` stream. We then
        // _map_ the result to extract the Json structure (encoded as String).
        // In RX, we must `subscribe` to the stream to trigger the processing. Without this nothing happens. There
        // are several `subscribe` methods, but here we recommend the `BiConsumer` format `(res, err) -> ...`
        // If it's a failure (err != null), write a 500 HTTP response with the cause (`err.getMessage()`) as
        // the payload. On success, write the body (`res`) into the HTTP response.

        vertx.eventBus().<JsonObject>rxSend("greetings", name)
            .map(message -> message.body().encode())
            .subscribe((res, err) -> {
                if (err != null) {
                    req.response().setStatusCode(500).end(err.getMessage());
                } else {
                    req.response().end(res);
                }
            });
    })
    .listen(8080);
Launch the exercise using the Exercise6#main method. Check the result by opening your browser to http://localhost:8080 (should display hello world) and http://localhost:8080?name=vert.x (should display hello vert.x).

3.10. Let’s move on
By now, you should have a better understanding of Vert.x and how to use it. But that’s just the beginning. Serious things are coming …​
