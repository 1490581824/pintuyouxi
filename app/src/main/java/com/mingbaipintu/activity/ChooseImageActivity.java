package com.mingbaipintu.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.mingbaipintu.GameManager;
import com.mingbaipintu.R;
import com.mingbaipintu.Util;

import java.util.HashSet;
import java.util.Set;

public class ChooseImageActivity extends Activity {

    public static final String IMAGE_SOURCE_ID_INDEX = "imageID_Index";
    private int mImagesCount;
    public static int[] mImagesId = {
            R.drawable.ablum,
            R.drawable.p1, R.drawable.p2, R.drawable.p3, R.drawable.p4, R.drawable.p5,
            R.drawable.p6, R.drawable.p7, R.drawable.p8, R.drawable.p9, R.drawable.p10,
            R.drawable.p11, R.drawable.p12, R.drawable.p13, R.drawable.p14, R.drawable.p15,
            R.drawable.p16
    };

    private LruCache mMemoryCache;
    private int mImageViewWidth;
    private int mImageViewHeight;
    private GridView mGridView;
    private Set<BitmapWorkerTask> mTaskCollection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_choose_image);

        mImageViewWidth = (GameManager.getInstance().getmWidthPixel() - 4) / 3;
        mImageViewHeight = 14 * mImageViewWidth / 9;

        /*
        共有15张图，第一张,1~15,和最后一张(为了显示美观)
        getmLevel() 最大返回15，所以mImagesCount最大为16
         */
        mImagesCount = GameManager.getInstance().getmLevel();
        mImagesCount=16;//忽略第一张图*/
        if (mImagesCount > 16) {
            mImagesCount = 16;
        }
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 20;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                // 重写此方法来衡量每张图片的大小，默认返回图片数量。
                return value.getByteCount() / 1024;
            }
        };
        mGridView = (GridView) findViewById(R.id.gridView);
        mGridView.setAdapter(new ImageAdapter(this));
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                intent.putExtra(IMAGE_SOURCE_ID_INDEX, position);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        TextView back = (TextView) findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    class ImageAdapter extends BaseAdapter {
        private Context mContext;

        public ImageAdapter(Context context) {
            this.mContext = context;
            mTaskCollection = new HashSet<>();
        }

        @Override
        public int getCount() {
            return mImagesCount;
        }

        @Override
        public Object getItem(int position) {
            return mImagesId[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //定义一个ImageView,显示在GridView里
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(mContext);
                imageView.setLayoutParams(new GridView.LayoutParams(mImageViewWidth, mImageViewHeight));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                imageView = (ImageView) convertView;
            }
            imageView.setTag(mImagesId[position]);
            String imageKey = String.valueOf(mImagesId[position]);
            Bitmap image = getBitmapFromMemoryCache(imageKey);
            if (image != null) {
                imageView.setImageBitmap(image);
            } else {
                imageView.setImageResource(R.drawable.placeholder);

                BitmapWorkerTask task = new BitmapWorkerTask();
                mTaskCollection.add(task);
                task.execute(mImagesId[position], position);
            }
            Util.showMemoryInformation();
            return imageView;
        }
    }

    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemoryCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    private Bitmap getBitmapFromMemoryCache(String key) {
        return (Bitmap) mMemoryCache.get(key);
    }


    /*
    class  BitmapWorkerTask
     */
    class BitmapWorkerTask extends AsyncTask {
        int mId;
        int mPosition = 0;

        public BitmapWorkerTask() {
        }

        @Override
        protected Bitmap doInBackground(Object[] params) {
            mId = (int) params[0];
            mPosition = (int) params[1];
            Bitmap bitmap = Util.decodeSampledBitmapFromResource(getResources(), mId, mImageViewWidth, mImageViewHeight);
            if (bitmap != null) {
                addBitmapToMemoryCache(String.valueOf((int) params[0]), bitmap);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            ImageView imageView = (ImageView) mGridView.findViewWithTag(mId);
            if (o != null && imageView != null) {
                imageView.setImageBitmap((Bitmap) o);
            }
            mTaskCollection.remove(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMemoryCache.evictAll();
        System.gc();
    }
}


