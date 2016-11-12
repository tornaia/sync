package com.github.tornaia.sync.client.win.remote;

import org.apache.http.HttpStatus;

import java.util.List;

import static java.util.Arrays.asList;

public final class SurvivableResponseStatusCodes {

    private static final List<Integer> TEMPORARY_NETWORK_PROBLEM = asList(HttpStatus.SC_BAD_GATEWAY, HttpStatus.SC_GATEWAY_TIMEOUT, HttpStatus.SC_NOT_FOUND, HttpStatus.SC_INTERNAL_SERVER_ERROR);

    public static boolean isSolvableByRepeat(int statusCode) {
        return TEMPORARY_NETWORK_PROBLEM.contains(statusCode);
    }

    private SurvivableResponseStatusCodes() {
    }
}
