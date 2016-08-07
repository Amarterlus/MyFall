package com.jyl.mywaterfall;

import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import rx.Observable;

/**
 * Created by Jyl on 2016/8/2.
 */
public interface MeiziApi {
    @Headers("Cache-Control: public, max-age=3600")
    @GET("data/福利/100/{round}")
    Observable<BeautyBean> getBeauty(@Path("round") int round);
}
