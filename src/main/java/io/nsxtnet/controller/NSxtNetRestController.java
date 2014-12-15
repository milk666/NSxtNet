package io.nsxtnet.controller;

import io.nsxtnet.http.Request;
import io.nsxtnet.http.Response;

public interface NSxtNetRestController {
	
	// POST Http Method
	public Object postMethod(Request request, Response response);

	// GET Http Method
	public Object getMethod(Request request, Response response);

}
