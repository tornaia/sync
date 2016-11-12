package com.github.tornaia.sync.server;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.EOFException;
import java.io.IOException;
import java.util.Objects;

@ControllerAdvice
public final class RestApiExceptionHandler {

    private final static Logger LOG = LoggerFactory.getLogger(RestApiExceptionHandler.class);

    @ExceptionHandler(value = Throwable.class)
    public static ModelAndView handle(HttpServletResponse response, Exception e) throws Exception {
        Throwable rootCause = ExceptionUtils.getRootCause(e);
        rootCause = rootCause != null ? rootCause : e;
        String message = rootCause.getMessage();

        if (rootCause instanceof EOFException) {
            if (Objects.equals("Unexpected EOF read on the socket", message)) {
                LOG.info("Client interrupted exception #1");
                return null;
            }
        }

        if (rootCause instanceof IOException) {
            if (Objects.equals("An existing connection was forcibly closed by the remote host", message)) {
                LOG.info("Client interrupted exception #2");
                return null;
            }
        }

        e.printStackTrace();
        throw e;
    }

    private RestApiExceptionHandler() {
    }
}
