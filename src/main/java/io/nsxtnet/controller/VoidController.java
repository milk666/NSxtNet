package io.nsxtnet.controller;

import io.nsxtnet.command.NSxtNetHttpCommand;
import io.nsxtnet.http.Request;
import io.nsxtnet.http.Response;
import io.nsxtnet.model.NSxtNetHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author toddf
 * @since Aug 31, 2010
 */
public class VoidController implements NSxtNetRestController {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(VoidController.class);

    @Override
    public Object postMethod(Request request, Response response) {
        return new VoidCommand(request, response).execute();
    }

    @Override
    public Object getMethod(Request request, Response response) {
        return new VoidCommand(request, response).execute();
    }

    public class VoidCommand extends NSxtNetHttpCommand {

        public VoidCommand(Request request, Response response) {
            super("Void", request, response);
            // TODO Auto-generated constructor stub
        }

        @Override
        protected NSxtNetHttpResponse run() {
            return makeResponseNotImplemented();
        }
    }
}
