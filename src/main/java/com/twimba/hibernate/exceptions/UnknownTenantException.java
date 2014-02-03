package com.twimba.hibernate.exceptions;

public class UnknownTenantException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public UnknownTenantException(String message) {
		super(message);
	}

}
