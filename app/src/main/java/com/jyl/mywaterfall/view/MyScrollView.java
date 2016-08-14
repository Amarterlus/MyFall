package com.jyl.mywaterfall.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.jyl.mywaterfall.AppConfig;
import com.jyl.mywaterfall.BeautyBean;
import com.jyl.mywaterfall.MeiziApi;
import com.jyl.mywaterfall.R;
import com.jyl.mywaterfall.utils.ImageLoader;
import com.jyl.mywaterfall.utils.ToastUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by teacher on 2016/8/6.
 */
public class MyScrollView extends NestedScrollView implements View.OnTouchListener {

    //每一页加载的图片数量
    private static int PAGE_SIZE = 20;
    private static int page      = 0;

    private LinearLayout first_column;
    private LinearLayout second_column;
    //每一列的宽度
    private int          columnWidth;
    //是否第一次加载
    private boolean isFirstLoad = false;
    private final ImageLoader      mImageLoader;
    //三列的列高
    private       int              firstHeight;
    private       int              secondHeight;
    private       int              screenHeight;
    //临时记录竖直方向滚动的距离
    private       int              changeY;
    private       View             child;
    //任务集合
    private       Set<MyAsyncTask> taskCollections;
    //图片集合
    private       List<ImageView>  imageLists;

    private List<String> mData;
    private int index = 0;

    public MyScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mImageLoader = ImageLoader.getInstance(context);
        setOnTouchListener(this);

        taskCollections = new HashSet<>();
        imageLists = new ArrayList<>();
        mData = new ArrayList<>();
    }


    //2.布局，因为要获取每一列的宽度，需要再onMeasure方法执行之后
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, final int b) {
        super.onLayout(changed, l, t, r, b);

        if (!isFirstLoad) {
            first_column = (LinearLayout) findViewById(R.id.left_column);
            second_column = (LinearLayout) findViewById(R.id.right_column);

            columnWidth = first_column.getWidth();
            //获取屏幕的高度（scrollView高度）
            screenHeight = getHeight();

            child = getChildAt(0);

            initData(index);

        }
    }

    private void initData(int index) {
        Observable<BeautyBean> beauty = AppConfig.sRetrofit.create(MeiziApi.class).getBeauty(index);
        beauty.subscribeOn(Schedulers.io())
                .flatMap(new Func1<BeautyBean, Observable<BeautyBean.ResultsBean>>() {
                    @Override
                    public Observable<BeautyBean.ResultsBean> call(BeautyBean bean) {
                        return Observable.from(bean.getResults());
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<BeautyBean.ResultsBean>() {
                    @Override
                    public void onCompleted() {
                        //初始化加载
                        loadMoreImages(mData);
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(BeautyBean.ResultsBean bean) {

                        String url = bean.getUrl();
                        if (!mData.contains(url)) {
                            mData.add(url);
                        }
                    }
                });
    }

    private void loadMoreImages(List<String> data) {
        //分页加载
        int startIndex = PAGE_SIZE * page;// 0 * 20 = 0;
        int endIndex = (page + 1) * PAGE_SIZE;// 1 * 20 =20;
        if (startIndex < data.size()) {
            if (endIndex > data.size()) {
                endIndex = data.size();
            }
            //遍历加载
            for (int i = startIndex; i < endIndex; i++) {
                //当前图片的路径
                String imageUrl = data.get(i);
                //开启线程下载
                MyAsyncTask task = new MyAsyncTask();
                task.execute(imageUrl);
                //添加下载记录
                taskCollections.add(task);
            }
            //加载页数叠加
            ToastUtil.showToast(getContext(),"loading...第" + page + "页");
            page++;
        } else {
//            Toast.makeText(getContext(), "no more pic...", Toast.LENGTH_SHORT).show();
            initData(++index);
        }
        isFirstLoad = true;

    }


    //不能返回true，不然ontouchevent方法不执行，不能滚动了
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            //等到滑动停止时，判断滑动到底部(递归判断)
            Message message = mHandler.obtainMessage();
            message.obj = this;
            mHandler.sendMessageDelayed(message, 5);
        }

        return false;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MyScrollView mScrollView = (MyScrollView) msg.obj;
            if (changeY == mScrollView.getScrollY()) {
                //滑动停止了,判断滑动到底部
                //所有任务都停止了，才进行加载更多
                if (mScrollView.getScrollY() + screenHeight >= child.getHeight() && taskCollections.size() == 0) {
                    //加载更多
                    mScrollView.loadMoreImages(mData);
                }

                //判断是否在屏幕内
                checkVisibility();

            } else {
                changeY = mScrollView.getScrollY();
                Message message = mHandler.obtainMessage();
                message.obj = mScrollView;
                mHandler.sendMessageDelayed(message, 5);
            }

        }
    };

    private void checkVisibility() {
        //遍历所有图片，判断是否在屏幕内
        for (int i = 0; i < imageLists.size(); i++) {
            ImageView iv = imageLists.get(i);
            int top = (int) iv.getTag(R.string.image_top);
            int bottom = (int) iv.getTag(R.string.image_bottom);
            if (bottom > getScrollY() && top < getScrollY() + screenHeight) {
                //在屏幕里面，设置原图
                String url = (String) iv.getTag(R.string.image_url);
                Bitmap bitmap = mImageLoader.getFromMemoryCache(url);
                if (bitmap != null) {
                    iv.setImageBitmap(bitmap);
                } else {
                    //从本地或者网络
                    MyAsyncTask myAsyncTask = new MyAsyncTask(iv);
                    myAsyncTask.execute(url);
                }

            } else {
                //在屏幕之外,设置一张默认的图片
                iv.setImageResource(R.drawable.empty_photo);
            }

        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return super.onTouchEvent(ev);
    }

    class MyAsyncTask extends AsyncTask<String, Void, Bitmap> {

        String    url;
        ImageView mIv;

        public MyAsyncTask() {
        }

        public MyAsyncTask(ImageView iv) {
            mIv = iv;
        }

        //子线程之中执行
        @Override
        protected Bitmap doInBackground(String... params) {
            url = params[0];
            //执行下载
            Bitmap bitmap = mImageLoader.loadImage(url, columnWidth);

            return bitmap;
        }

        //在UI线程中，子线程之后
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (bitmap != null) {
                if (mIv != null) {
                    mIv.setImageBitmap(bitmap);
                } else {
                    //因为精度丢失，需要重新测算
                    double ratio = bitmap.getWidth() / (columnWidth * 1.0);
                    int imageHeight = (int) (bitmap.getHeight() / ratio);

                    ImageView imageView = new ImageView(getContext());
                    imageView.setImageBitmap(bitmap);
                    //图片参数
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(columnWidth, imageHeight);
                    imageView.setLayoutParams(params);
                    imageView.setPadding(5, 5, 5, 5);
                    imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                    //保存url
                    imageView.setTag(R.string.image_url, url);



                    //往最短的一列进行添加
                    findShortestColumn(imageHeight, imageView).addView(imageView);

                    //1.你要先把view缩小
                    imageView.setScaleY(.8f);
                    imageView.setScaleX(.8f);

                    //使用动画使用我们的view恢复
                    ViewCompat.animate(imageView).scaleY(1.0f).scaleX(1.0f).setDuration(500).setInterpolator(new
                            OvershootInterpolator()).start();
                    //加载结束，从集合中移除
                    taskCollections.remove(this);
                    //将imageview放到集合中
                    imageLists.add(imageView);

                }
            }
        }
    }

    private LinearLayout findShortestColumn(int imageHeight, ImageView iv) {
        if (firstHeight <= secondHeight) {
            iv.setTag(R.string.image_top, firstHeight);
            firstHeight += imageHeight;
            iv.setTag(R.string.image_bottom, firstHeight);
            return first_column;
        } else {
            iv.setTag(R.string.image_top, secondHeight);
            secondHeight += imageHeight;
            iv.setTag(R.string.image_bottom, secondHeight);
            return second_column;
        }
    }
}
