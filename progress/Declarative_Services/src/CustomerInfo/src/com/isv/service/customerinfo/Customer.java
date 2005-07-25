package com.isv.service.customerinfo; 
public class Customer { 
String custNo = "";
String firstName = "";
String lastName = "";
String symbol = "";
int numShares = 0;
String postalCode = "";

String errorMsg = "";

public void setCustNo(String input)
{
	custNo = input;
}
public void setFirstName(String input)
{
	firstName = input;
}
public void setLastName(String input)
{
	lastName = input;
}
public void setSymbol(String input)
{
	symbol = input;
}
public void setNumShares(int input)
{
	numShares = input;
}
public void setPostalCode(String input)
{
	postalCode = input;
}

public void setErrorMsg(String input) {
	errorMsg = input;
	
}
}

