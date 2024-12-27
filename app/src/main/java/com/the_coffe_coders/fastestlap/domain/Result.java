package com.the_coffe_coders.fastestlap.domain;

import com.the_coffe_coders.fastestlap.api.DriverStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.driver.DriverAPIResponse;

public abstract class Result {
    private Result() {}

    public boolean isSuccess() {
        return !(this instanceof Error);
    }

    /**
     * Class that represents a successful action during the interaction
     * with a Web Service or a local database.
     */
    public static final class DriverSuccess extends Result {
        private final DriverAPIResponse driverAPIResponse;
        public DriverSuccess(DriverAPIResponse driverStandingsAPIResponse) {
            this.driverAPIResponse = driverStandingsAPIResponse;
        }
        public DriverAPIResponse getData() {
            return driverAPIResponse;
        }
    }
/*
    public static final class UserSuccess extends Result {
        private final User user;
        public UserSuccess(User user) {
            this.user = user;
        }
        public User getData() {
            return user;
        }
    }

    /**
     * Class that represents an error occurred during the interaction
     * with a Web Service or a local database.
     */
    public static final class Error extends Result {
        private final String message;
        public Error(String message) {
            this.message = message;
        }
        public String getMessage() {
            return message;
        }
    }
}