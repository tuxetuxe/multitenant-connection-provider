package com.twimba.hibernate.exceptions;

public class InvalidTenantConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidTenantConfigurationException(String message) {
        super(message);
    }

}
