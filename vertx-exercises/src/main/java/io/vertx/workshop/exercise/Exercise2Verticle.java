package io.vertx.workshop.exercise;

import io.vertx.core.AbstractVerticle;

/**
 * Create a HTTP server in the `start` method.
 */
public class Exercise2Verticle extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        // You can access the vert.x instance your deployed on using the `vertx` (inherited) field

        // Create a HTTP server
        // Instead of "hello", display the name of the thread serving the request (using Thread.currentThread()
        // .getName())
        // TODO
    	 vertx.createHttpServer()
         .requestHandler(req -> {
        	 sleep();
         req.response().end(Thread.currentThread().getName());
         })
         .listen(8091);
        
    }


    /**
     * Method used in the Exercise 3.
     */
    private void sleep() {
        try {
            Thread.sleep(3000L);
        } catch (InterruptedException e) {
            // Ignore me
        }
    }
}
