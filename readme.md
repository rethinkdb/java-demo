# Java Chat Demo

This simple chat application demonstrates how to use the RethinkDB Java driver. The backend is written with with [Vert.x][vertx], a Java framework that is well-suited for realtime web applications. The frontend is built with [Vue.js][vue], a lightweight MVC framework that supports simple data binding.

Vert.x applications are composed of microservices, each implemented in a class called a Verticle. The framework provides a built-in [event bus][eventbus] that you can use to pass messages between verticles. The Vert.x event bus also has a WebSocket bridge, implemented on top of SockJS, that you can use to propagate messages between the frontend and the backend.

This application uses a RethinkDB changefeed and the Vert.x event bus to broadcast new messages to all of the connected clients over SockJS. It also exposes an HTTP POST endpoint that client applications can use to send new messages.

[vertx]: http://vertx.io/
[eventbus]: http://vertx.io/docs/vertx-core/java/#event_bus
[vue]: http://vuejs.org/
