package com.movideo.nextgen.encoder.drm.castlabs;

import java.util.Map;

import org.apache.http.HttpResponse;
import org.json.JSONObject;

import com.movideo.nextgen.common.http.HttpHelper;
import com.movideo.nextgen.encoder.common.EncoderException;

public class CastlabsHttpHelper extends HttpHelper {

    public static JSONObject makeHttpCall(String url, String payload, String method, Map<String, String> headers)
	    throws CastlabsException {

	try {
	    return httpService(url, method, headers, payload);
	} catch (EncoderException e) {
	    // TODO Auto-generated catch block
	    throw new CastlabsException(e.getStatus(), e.getMessage(), e.getOriginalException());
	}

    }

    public static HttpResponse getRawHttpResponse(String url, String payload, String method,
	    Map<String, String> headers) throws CastlabsException {
	try {
	    return rawHttpService(url, method, headers, payload);
	} catch (EncoderException e) {
	    // TODO Auto-generated catch block
	    throw new CastlabsException(e.getStatus(), e.getMessage(), e.getOriginalException());
	}
    }

}
