package com.callcenter.ftcjsc.services;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public class ApiClient {
    public interface APIService {
        @GET()
        Call<String> sendRequest(@Url String url);

        @Streaming
        @GET
        Call<ResponseBody> downUpFile(@Url String fileUrl);
    }

    public static final String BASE_URL = "http://ibsmanage.com/Active/";
//    public static final String BASE_URL = "http://acomcorp.vn/Erl/active/";

    public static APIService getAPIService(Boolean isTest) {
        return RetrofitClient.getClient( isTest ? "http://ipv4.download.thinkbroadband.com/" : BASE_URL).create(APIService.class);
    }
}
