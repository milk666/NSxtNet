package io.nsxtnet.example;

import io.nsxtnet.http.Request;
import io.nsxtnet.http.Response;

/**
 * @author toddf
 * @since Aug 31, 2010
 */
public class EchoController
{
	class EchoResponse {
		private String action;
		private String msg;

		public String getAction() {
			return action;
		}

		public void setAction(String action) {
			this.action = action;
		}

		public String getMsg() {
			return msg;
		}

		public void setMsg(String msg) {
			this.msg = msg;
		}
	}

	public Object CmdGet(Request request, Response response)
	{
		EchoResponse res = new EchoResponse();
		res.setAction("GET");
		res.setMsg( "Test URI - GET" );

		return res;
	}
	
	public Object CmdPost(Request request, Response response)
	{
		EchoResponse res = new EchoResponse();
		res.setAction("POST");
		res.setMsg( "Test URI - POST" );

		return res;
	}
}
