package com.javora.commerce.common.validation;

public final class ValidationMessages {

	public static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d).+$";
	public static final String PASSWORD_MESSAGE = "Password must contain at least one letter and one number";
	public static final String PHONE_PATTERN = "^\\+?[0-9]{8,15}$";
	public static final String PHONE_MESSAGE = "Phone number must contain 8 to 15 digits";

	private ValidationMessages() {
	}

}
