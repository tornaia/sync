package com.github.tornaia.sync.client.win.remote;

import org.apache.http.HttpStatus;

import java.util.List;

import static java.util.Arrays.asList;

public final class NormalResponseStatusCodes {

    private static final List<Integer> VALID_STATUS_CODES = asList(HttpStatus.SC_OK);

    public static boolean isValid(int statusCode) {
        return VALID_STATUS_CODES.contains(statusCode);
    }

    private NormalResponseStatusCodes() {
    }
}
