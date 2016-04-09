package com.twimba.hibernate.exceptions;

public class MultiTenantConnectionProviderException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MultiTenantConnectionProviderException(String message, Throwable e) {
        super(message, e);
    }

}
