package mikolajgrygiel.jedzmyrazem;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Iterator;


public class ServiceHandler {

    //region Properties
    private String response = null;
    public final static int GET = 1;
    public final static int POST = 2;
    public final static int DELETE = 3;
    //endregion


    //region Constructor & init
    public ServiceHandler() {

    }
    //endregion



    //region Common methods

    /**
     * Making service call
     * @url - url to make request
     * @method - http request method
     * */
    public String makeServiceCall(String url, int method) {
        return this.makeServiceCall(url, method, null, null);
    }

    /**
     * Making service call
     * @url - url to make request
     * @method - http request method
     * @params - http request params
     * */


    public String makeServiceCall(String url, int method,
                                  JSONObject params, String paramsString) {
        try {
            // http client

            HttpEntity httpEntity = null;
            HttpResponse httpResponse = null;


            // Checking http request method type
            if (method == POST) {
                HttpPost httpPost = new HttpPost(url);
                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Content-type", "application/json");
                // adding post params
                if (params != null) {
                    String params_string = params.toString();
                    //httpPost.setEntity(new UrlEncodedFormEntity(params));
                    httpPost.setEntity(new StringEntity(params_string));
                }

                httpResponse = LoginActivity.httpClient.execute(httpPost);
            } else if (method == GET) {
                if (paramsString != null) {
                    url += "?" + paramsString;
                }
                HttpGet httpGet = new HttpGet(String.valueOf(new URL(url)));
                httpGet.setHeader("Accept", "application/json");
                httpGet.setHeader("Content-type", "application/json");
                httpResponse = LoginActivity.httpClient.execute(httpGet);
            } else if (method == DELETE){
                HttpDelete httpDelete = new HttpDelete(url);
                httpDelete.setHeader("Accept", "application/json");
                httpDelete.setHeader("Content-type", "application/json");
                httpResponse = LoginActivity.httpClient.execute(httpDelete);
            }
            try {
                httpEntity = httpResponse.getEntity();
                response = EntityUtils.toString(httpEntity);
            }
            catch (NullPointerException e){
                e.printStackTrace();
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;

    }
    //endregion
}