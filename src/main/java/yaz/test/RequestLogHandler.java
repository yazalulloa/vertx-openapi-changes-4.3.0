package yaz.test;

import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.impl.LoggerHandlerImpl;

public interface RequestLogHandler extends LoggerHandler {


    static LoggerHandler create() {

        return new LoggerHandlerImpl(true, LoggerHandler.DEFAULT_FORMAT);
    }
}
