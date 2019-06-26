package com.callcenter;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface APIService {
    @GET()
    Call<String> sendReceiverReport(@Url String url);
}