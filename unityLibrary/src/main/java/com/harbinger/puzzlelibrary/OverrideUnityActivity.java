package com.harbinger.puzzlelibrary;
import android.os.Bundle;
import com.unity3d.player.UnityPlayerActivity;

public abstract class OverrideUnityActivity extends UnityPlayerActivity {
    public static OverrideUnityActivity instance = null;
	  
    abstract protected void vibrate();
	
	abstract protected void loadNextImage();
	
	abstract protected void camera();
	
	abstract protected void showLoading();
	
	abstract protected void dismissLoading();
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
    }	
}
