package com.ppworks.ppnews.module.photo.model;

import com.ppworks.ppnews.callback.RequestCallback;
import rx.Subscription;

/**
 * ClassName: IPhotoListInteractor<p>
 * Author: Tomoya-Hoo<p>
 * Fuction: 图片列表Model层接口<p>
 * CreateDate: 2016/4/21 3:48<p>
 * UpdateUser: <p>
 * UpdateDate: <p>
 */
public interface IPhotoListInteractor<T> {

    Subscription requestPhotoList(RequestCallback<T> callback, String id, int startPage);

}
