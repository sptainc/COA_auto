package com.callcenter;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public class ApiUtils {
    public interface APIService {
        @GET()
        Call<String> sendReceiverReport(@Url String url);
    }

    public static final String BASE_URL = "http://acomcorp.vn/Erl/active/";

    public static APIService getAPIService() {
        return RetrofitClient.getClient(BASE_URL).create(APIService.class);

    }
}
