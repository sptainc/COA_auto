package com.callcenter.ftcjsc.services;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public class ApiClient {
    public interface APIService {
        @GET()
        Call<String> sendRequest(@Url String url);
    }

    public static final String BASE_URL = "http://ibsmanage.com/Active/";
//    public static final String BASE_URL = "http://acomcorp.vn/Erl/active/";

    public static APIService getAPIService() {
        return RetrofitClient.getClient(BASE_URL).create(APIService.class);
    }
}
