package yaz.test;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.openapi.RouterBuilderOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public class Application {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static void main(String[] args) {
        new Application().run();
    }

    private void run() {
        final var vertx = Vertx.vertx();

        RouterBuilder.create(vertx, "openapi.yaml")
                .map(routerBuilder -> {


                    final var allowedHeaders = new HashSet<String>();
                    allowedHeaders.add("Authorization");
                    allowedHeaders.add("Content-Type");

                    final var allowedMethods = new HashSet<HttpMethod>();
                    allowedMethods.add(HttpMethod.OPTIONS);
                    allowedMethods.add(HttpMethod.GET);
                    allowedMethods.add(HttpMethod.POST);
                    allowedMethods.add(HttpMethod.DELETE);
                    allowedMethods.add(HttpMethod.PUT);

                    routerBuilder.rootHandler(LoggerHandler.create(true, LoggerHandler.DEFAULT_FORMAT));
                    routerBuilder.rootHandler(TimeoutHandler.create(180000));
                    routerBuilder.rootHandler(CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));
                    routerBuilder.bodyHandler(BodyHandler.create().setBodyLimit(40000000).setDeleteUploadedFilesOnEnd(true).setHandleFileUploads(true));

                    /*Consumer<Operation> operationConsumer = operation -> {
                        operation.handler(LoggerHandler.create(true, LoggerHandler.DEFAULT_FORMAT));
                        operation.handler(TimeoutHandler.create(180000));
                        operation.handler(CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));
                        operation.handler(BodyHandler.create().setBodyLimit(40000000).setDeleteUploadedFilesOnEnd(true).setHandleFileUploads(true));
                    };*/


                    final var operationsRegistered = new LinkedHashSet<String>();
                    routerBuilder.operations().forEach(operation -> {

                        //operationConsumer.accept(operation);
                        operation.handler(ctx -> {
                                    final var jsonObject = new JsonObject().put("hello", "world");
                                    ctx.json(jsonObject);
                                })
                                .failureHandler(ctx -> {
                                    logger.error("FAILING", ctx.failure());
                                    ctx.json(new JsonObject().put("failure", ctx.failure().getMessage()));
                                });

                        operationsRegistered.add(operation.getOpenAPIPath() + " " + operation.getHttpMethod().name());

                    });

                    final var routerBuilderOptions = new RouterBuilderOptions()
                            .setMountResponseContentTypeHandler(true);// Mount ResponseContentTypeHandler automatically

                    routerBuilder.setOptions(routerBuilderOptions);

                    routerBuilder.mountServicesFromExtensions();

                    final var join = operationsRegistered.stream().sorted().collect(Collectors.joining("\n"));

                    logger.info("OPERATIONS REGISTERED\n" + join);


                    final var router = routerBuilder.createRouter();


                    router.get("/swagger/swagger.json").handler(StaticHandler.create().setCachingEnabled(false).setMaxAgeSeconds(1).setSendVaryHeader(true).setEnableRangeSupport(true));
                    router.get("/swagger/swagger.json/").handler(StaticHandler.create().setCachingEnabled(false).setMaxAgeSeconds(1).setSendVaryHeader(true).setEnableRangeSupport(true));
                    router.get("/*").handler(StaticHandler.create().setCachingEnabled(true).setSendVaryHeader(true).setEnableRangeSupport(true));

                    return router;
                })
                .map(router -> {

                    return vertx.createHttpServer(new HttpServerOptions().setPort(8000))
                            .requestHandler(router);
                })
                .flatMap(HttpServer::listen)
                .onSuccess(server -> {
                    logger.info("HTTP_SERVER {}", server.actualPort());
                })
                .onFailure(throwable -> {

                    logger.error("ERROR", throwable);
                    System.exit(-1);
                });
    }
}
