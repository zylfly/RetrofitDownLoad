package com.zyl.zylretrofitclients.download;

import android.os.Handler;
import android.util.Log;

import com.zyl.zylretrofitclients.bus.RxBus;
import com.zyl.zylretrofitclients.bus.RxSubscriptions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.ResponseBody;
import rx.Subscription;
import rx.functions.Action1;

/**
 *  Created by zyl.
 */

public abstract class ProgressCallBack<T> {

    private String destFileDir; // 本地文件存放路径
    private String destFileName; // 文件名
    private Subscription mSubscription;
    private final Handler handler;

    public ProgressCallBack(String destFileDir, String destFileName) {
        this.destFileDir = destFileDir;
        this.destFileName = destFileName;
        handler = new Handler();
        subscribeLoadProgress();
    }

    public abstract void onSuccess(T t);

    public abstract void progress(long progress, long total);

    public void onStart() {
    }

    public void onCompleted() {
    }

    public abstract void onError(Throwable e);

    public void saveFile(ResponseBody body) {
        InputStream is = null;
        byte[] buf = new byte[2048];
        int len;
        FileOutputStream fos = null;
        try {
            is = body.byteStream();
            File dir = new File(destFileDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, destFileName);
            fos = new FileOutputStream(file);
            while ((len = is.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
            fos.flush();
            unsubscribe();
            //onCompleted();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) is.close();
                if (fos != null) fos.close();
            } catch (IOException e) {
                Log.e("saveFile", e.getMessage());
            }
        }
    }

    /**
     * 订阅加载的进度条
     */
    public void subscribeLoadProgress() {
        mSubscription = RxBus.getDefault().toObservable(DownLoadStateBean.class)
                .subscribe(new Action1<DownLoadStateBean>() {
                    @Override
                    public void call(final DownLoadStateBean progressLoadBean) {
                        // 回调到主线程更新UI
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                progress(progressLoadBean.getBytesLoaded(), progressLoadBean.getTotal());
                            }
                        });
                    }
                });
        //将订阅者加入管理站
        RxSubscriptions.add(mSubscription);
    }

    /**
     * 取消订阅，防止内存泄漏
     */
    public void unsubscribe() {
        RxSubscriptions.remove(mSubscription);
    }
}