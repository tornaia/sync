package com.github.tornaia.sync.client.win.remote;

import org.apache.http.HttpStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;

public final class SurvivableResponseStatusCodes {

    private static final List<Integer> BACKEND_DEPLOYMENT_ONGOING = asList(HttpStatus.SC_INTERNAL_SERVER_ERROR, HttpStatus.SC_BAD_GATEWAY);
    private static final List<Integer> TEMPORARY_NETWORK_PROBLEM = asList(HttpStatus.SC_BAD_GATEWAY);

    public static final Set<Integer> SOLVABLE_BY_REPEAT_STATUS_CODES;

    static {
        SOLVABLE_BY_REPEAT_STATUS_CODES = new HashSet<>();
        SOLVABLE_BY_REPEAT_STATUS_CODES.addAll(BACKEND_DEPLOYMENT_ONGOING);
        SOLVABLE_BY_REPEAT_STATUS_CODES.addAll(TEMPORARY_NETWORK_PROBLEM);
    }

    public static boolean isSolvableByRepeat(int statusCode) {
        return SOLVABLE_BY_REPEAT_STATUS_CODES.contains(statusCode);
    }

    private SurvivableResponseStatusCodes() {
    }
}
