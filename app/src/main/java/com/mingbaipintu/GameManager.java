package com.mingbaipintu;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.mingbaipintu.activity.ChooseImageActivity;
import com.mingbaipintu.activity.MainActivity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by CCX on 2018/05/28.
 */
public class GameManager {
    private static GameManager mGameManager=null;
    private UIManager mUIManager;
    private LocalBroadcastManager mLocalBroadcastManager=null;
    private int mCurrentMaxDiff;
    private boolean mIsCustom;
    private int mLevel;
    private int mDiff;
    public  Bitmap[] mBitmapChips = new Bitmap[100];
    private MainActivity mMainActivity;
    private Bitmap mCurrentImage;
    private int mWidthPixel;
    private int mHeightPixel;
    private boolean mIsGaming;

    private GameManager() {
        mUIManager=UIManager.getInstance();
        mLocalBroadcastManager=LocalBroadcastManager.getInstance(MyApplication.getContextObject());//本地广播管理器
        mIsCustom=false;
        mLevel=1;
        mIsGaming=false;
    }

    public static GameManager getInstance() {
        if (mGameManager == null) {
            mGameManager = new GameManager();
        }
        return mGameManager;
    }
    public boolean ismIsGaming()
    {
        return mIsGaming;
    }//??
    public void setScreenPixel(int widthPixel,int heightPixel)
    {
        this.mWidthPixel=widthPixel;
        this.mHeightPixel=heightPixel;
    }
    public int getmWidthPixel()
    {
        return mWidthPixel;
    }
    public int getmHeightPixel()
    {
        return mHeightPixel;
    }
    public void setMainActivityContext(MainActivity activity)
    {
        mMainActivity=activity;
    }
    public Activity getMainActivity()
    {
        return mMainActivity;
    }
    public void setmIsCustom(boolean isCustom)
    {
        mIsCustom=isCustom;
    }
    public boolean IsCustom()
    {
        return mIsCustom;
    }
    public void setmDiff(int diff)
    {
        this.mDiff=diff;
    }
    public int getmCurrentMaxDiff() {
        return mCurrentMaxDiff;
    }

    public void setmLevel(int level) {
        if(level>15)
            level=15;
        this.mLevel = level;
        caculatemDiff();
    }
    public int getmLevel()
    {
        return mLevel;
    }
    public LocalBroadcastManager getmLocalBroadcastManager() {
        return mLocalBroadcastManager;
    }
    public void gameBegin()
    {
        mIsGaming=true;
        mUIManager.showGamingView();
        mUIManager.setTitleText("00:00");
        UpdateTitleTimer.getInstance().startTimer(0);
    }
    public void gameReady()
    {
        mIsGaming=false;
        UpdateTitleTimer.getInstance().concelTimer();
        mUIManager.releaseBitmapResourse(mDiff);
        if(mIsCustom) {
            mUIManager.setTitleText("");
        }else
        {
            mUIManager.setTitleText("第" + mLevel + "关(共15关)");
        }
        mUIManager.showGameView(mCurrentImage);
    }

    public int getmDiff() {
        return mDiff;
    }
    public void gameWin()
    {
        if(mIsGaming) {
            mIsGaming=false;
            int time = UpdateTitleTimer.getInstance().concelTimer();
            if (mIsCustom) {
                mUIManager.setGameWinMarkText(time, "");
                mUIManager.setGameWinAgainButtonVisibility(true);
                mUIManager.setGameWinNextButtonText("换一张");
            } else {
                if (mLevel == 15) {
                    mUIManager.setGameWinNextButtonText("换一张");
                    mIsCustom=true;
                }
                else {
                    mUIManager.setGameWinNextButtonText("下一关");
                }
                mUIManager.setGameWinAgainButtonVisibility(false);
                mUIManager.setGameWinMarkText(time, "第" + mLevel + "关");
                mLevel++;
                int oldDiff = mDiff;
                caculatemDiff();
                if (oldDiff != mDiff) {
                    mUIManager.initGamingView(mDiff);
                }
            }
            mUIManager.showGameWinView(mCurrentImage);
        }
    }
    //根据关卡设置难度，每3关一个难度
    public void caculatemDiff() {
        if(mLevel<16) {
            switch ((mLevel - 1) / 3) {
                case 0:
                    setmDiff(3);
                    mCurrentMaxDiff =3;
                    break;
                case 1:
                    setmDiff(4);
                    mCurrentMaxDiff = 4;
                    break;
                case 2:
                    setmDiff(5);
                    mCurrentMaxDiff = 5;
                    break;
               case 3:
                  setmDiff(7);
                   mCurrentMaxDiff = 7;
                   break;
                case 4:
                    setmDiff(9);
                   mCurrentMaxDiff = 9;
                    break;
            }
        }else
        {
            setmDiff(3);
            mCurrentMaxDiff = 3;
        }
    }

    //拆分图片
    public  void splitBitmap() {
        int width = mCurrentImage.getWidth();
        int height = mCurrentImage.getHeight();
        int w = width / mDiff;
        int h = height / mDiff;//根据难度设置子图的宽高

        for (int i = 0; i < mDiff; ++i) {
            for (int j = 0; j < mDiff; j++) {
                recycleBitmap(mBitmapChips[i * mDiff + j]);//回收图片占用的资源
                mBitmapChips[i * mDiff + j] = Bitmap.createBitmap(mCurrentImage, j * w, i * h, w, h);
            }
        }
        System.gc();// garbage collection,垃圾回收
    }
    public Bitmap getBitmapChip(int index)
    {
        return mBitmapChips[index];
    }

    //设置当前的图片
    public void setCurrentImageFromResource(int index)
    {
        Resources res = MyApplication.getContextObject().getResources();
        int resourceId = ChooseImageActivity.mImagesId[index];
        int offY=mUIManager.getmTitleOffY();
        Bitmap choosedImage = Util.decodeSampledBitmapFromResource(res,resourceId,mWidthPixel,mHeightPixel-offY);
        if(choosedImage!=null) {
            choosedImage = Util.normalizeImage(choosedImage, mWidthPixel, mHeightPixel -  offY);
            recycleBitmap(mCurrentImage);
            mCurrentImage = Util.cropBitmap(choosedImage, mWidthPixel, mHeightPixel -  offY);
            recycleBitmap(choosedImage);
        }else
        {
            Toast.makeText(mMainActivity, "图片没找到！", Toast.LENGTH_LONG).show();
            return;
        }
    }

    //获取本地图片
    public void setCurrentImageFromUri(Uri uri)
    {
        int offY=mUIManager.getmTitleOffY();
        Bitmap choosedImage=null;
        InputStream is = null;
        try {
            is = MyApplication.getContextObject().getContentResolver().openInputStream(uri);
            choosedImage = Util.decodeSampledBitmapFromStream(is, mWidthPixel, mHeightPixel - offY);
            is.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(choosedImage!=null) {
            recycleBitmap(mCurrentImage);
            choosedImage = Util.normalizeImage(choosedImage, mWidthPixel, mHeightPixel -  offY);
            mCurrentImage = Util.cropBitmap(choosedImage, mWidthPixel, mHeightPixel -  offY);
            recycleBitmap(choosedImage);
        }else
        {
            Toast.makeText(mMainActivity, "图片没找到！", Toast.LENGTH_LONG).show();
            return;
        }
    }
    public void startChooseActivity() {
        Intent chooseImageIntent = new Intent(mMainActivity, ChooseImageActivity.class);
        mMainActivity.startActivityForResult(chooseImageIntent, MainActivity.CHOOSE_IMAGE);
    }
    public void recycleBitmap(Bitmap bitmap) {
        // 先判断是否已经回收
        if (bitmap != null && !bitmap.isRecycled()) {
            // 回收并且置为null
            bitmap.recycle();
            bitmap = null;
        }
    }
}

