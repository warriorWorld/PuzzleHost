package com.harbinger.puzzlehost.service;

import android.app.Service;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.harbinger.puzzlehost.utils.EventBusEvent;
import com.harbinger.puzzlehost.utils.MatchStringUtil;

import org.greenrobot.eventbus.EventBus;

import androidx.annotation.Nullable;

/**
 * Created by Administrator on 2018/7/28.
 */

public class CopyBoardService extends Service implements ClipboardManager.OnPrimaryClipChangedListener {
    ClipboardManager clipboard;
    private String lastOne = "";

    @Override
    public void onCreate() {
        super.onCreate();
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        clipboard.addPrimaryClipChangedListener(this);
    }

    @Override
    public void onPrimaryClipChanged() {
        if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {

            CharSequence addedText = clipboard.getPrimaryClip().getItemAt(0).getText();

            if (addedText != null && !lastOne.equals(addedText.toString())) {
                lastOne = addedText.toString();
                try {
                    if (MatchStringUtil.isImageURL(addedText.toString())) {
                        EventBus.getDefault().post(new EventBusEvent(addedText.toString(), EventBusEvent.COPY_BOARD_URL_EVENT));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        clipboard.removePrimaryClipChangedListener(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }
}
