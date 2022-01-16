package com.harbinger.puzzlehost.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;

/**
 * http://www.mangareader.net/
 */
public class FileSpider {
    private final String TAG = "FileSpider";
    private String webUrl = "file://";

    private FileSpider() {
    }

    private static volatile FileSpider instance = null;

    public static FileSpider getInstance() {
        if (instance == null) {
            //线程锁定
            synchronized (FileSpider.class) {
                //双重锁定
                if (instance == null) {
                    instance = new FileSpider();
                }
            }
        }
        return instance;
    }

    public void deleteFile(String file) {
        if (file.contains("file://")) {
            file = file.substring(7, file.length());
        }
        File f = new File(file);
        deleteFile(f);
    }

    /**
     * 递归删除文件和文件夹 因为file.delete();只能删除空文件夹或文件 所以需要这么递归循环删除
     *
     * @param file 要删除的根目�?
     */
    public static void deleteFile(File file) {
        if (file.isFile() && file.exists()) {
            file.delete();
            return;
        }
        if (file.isDirectory()) {
            File[] childFile = file.listFiles();
            if (childFile == null || childFile.length == 0) {
                file.delete();
                return;
            }
            for (File f : childFile) {
                deleteFile(f);
            }
            file.delete();
        }
    }

    public String getChildFolderName(int episode, int folderSize) {
        String res;
        int start = ((int) (episode / folderSize)) * folderSize;
        int end = start + folderSize - 1;
        res = start + "-" + end;
        return res;
    }

    public byte[] File2byte(String filePath) {
        byte[] buffer = null;
        try {
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }

    public File byte2File(byte[] buf, String filePath, String fileName) {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        File file = null;
        try {
            File dir = new File(filePath);
            if (!dir.exists() && dir.isDirectory()) {
                dir.mkdirs();
            }
            file = new File(filePath + File.separator + fileName);
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(buf);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return file;
        }
    }

    public String getPath(Context context, Uri uri) throws URISyntaxException {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {"_data"};
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it  Or Log it.
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public Object readFileFromLocal(String fileName) {
        FileInputStream fileInputStream = null;
        ObjectInputStream objectInputStream = null;
        try {
            fileInputStream = new FileInputStream(fileName);
            objectInputStream = new ObjectInputStream(fileInputStream);
            Object obj = objectInputStream.readObject();

            return obj;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileInputStream != null) fileInputStream.close();
                if (objectInputStream != null) objectInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public Object readObjFromSDCard(String filePath) {
        Object obj;
        File sdFile = new File(filePath);
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            fis = new FileInputStream(sdFile); //获得输入流
            ois = new ObjectInputStream(fis);
            obj = ois.readObject();
            return obj;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) fis.close();
                if (ois != null) ois.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 复制文件目录
     *
     * @param srcDir  要复制的源目录 eg:/mnt/sdcard/DB
     * @param destDir 复制到的目标目录 eg:/mnt/sdcard/db/
     * @return
     */
    public boolean copyDir(String srcDir, String destDir) {
        File sourceDir = new File(srcDir);
        //判断文件目录是否存在
        if (!sourceDir.exists()) {
            return false;
        }
        //判断是否是目录
        if (sourceDir.isDirectory()) {
            File[] fileList = sourceDir.listFiles();
            File targetDir = new File(destDir);
            //创建目标目录
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            //遍历要复制该目录下的全部文件
            for (int i = 0; i < fileList.length; i++) {
                if (fileList[i].isDirectory()) {//如果如果是子目录进行递归
                    copyDir(fileList[i].getPath() + "/",
                            destDir + fileList[i].getName() + "/");
                } else {//如果是文件则进行文件拷贝
                    copyFile(fileList[i].getPath(), destDir + fileList[i].getName());
                }
            }
            return true;
        } else {
            copyFileToDir(srcDir, destDir);
            return true;
        }
    }


    /**
     * 复制文件（非目录）
     *
     * @param srcFile  要复制的源文件
     * @param destFile 复制到的目标文件
     * @return
     */
    public boolean copyFile(String srcFile, String destFile) {
        try {
            InputStream streamFrom = new FileInputStream(srcFile);
            OutputStream streamTo = new FileOutputStream(destFile);
            byte buffer[] = new byte[1024];
            int len;
            while ((len = streamFrom.read(buffer)) > 0) {
                streamTo.write(buffer, 0, len);
            }
            streamFrom.close();
            streamTo.close();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }


    /**
     * 把文件拷贝到某一目录下
     *
     * @param srcFile
     * @param destDir
     * @return
     */
    public boolean copyFileToDir(String srcFile, String destDir) {
        File fileDir = new File(destDir);
        if (!fileDir.exists()) {
            fileDir.mkdir();
        }
        String destFile = destDir + "/" + new File(srcFile).getName();
        try {
            InputStream streamFrom = new FileInputStream(srcFile);
            OutputStream streamTo = new FileOutputStream(destFile);
            byte buffer[] = new byte[1024];
            int len;
            while ((len = streamFrom.read(buffer)) > 0) {
                streamTo.write(buffer, 0, len);
            }
            streamFrom.close();
            streamTo.close();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }


    /**
     * 移动文件目录到某一路径下
     *
     * @param srcFile
     * @param destDir
     * @return
     */
    public boolean moveFile(String srcFile, String destDir) {
        //复制后删除原目录
        if (copyDir(srcFile, destDir)) {
            deleteFile(new File(srcFile));
            return true;
        }
        return false;
    }

    /**
     * 获取指定文件大小
     */
    public long getFileSize(File file) throws Exception {
        long size = 0;
        if (file.exists()) {
            FileInputStream fis = null;
            fis = new FileInputStream(file);
            size = fis.available();
        } else {
            Log.d("spider", "文件不存在!");
        }
        return size;
    }

    /**
     * 获取指定文件夹
     */
    public long getFileSizes(File f) throws Exception {
        long size = 0;
        File flist[] = f.listFiles();
        for (int i = 0; i < flist.length; i++) {
            if (flist[i].isDirectory()) {
                size = size + getFileSizes(flist[i]);
            } else {
                size = size + getFileSize(flist[i]);
            }
        }
        return size;
    }

    /**
     * 转换文件大小
     */
    public String toFileSize(long fileS) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        String wrongSize = "0B";
        if (fileS == 0) {
            return wrongSize;
        }
        if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024) + "KB";
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + "MB";
        } else {
            fileSizeString = df.format((double) fileS / 1073741824) + "GB";
        }
        return fileSizeString;
    }

    public ArrayList<String> getFilteredImages(Context context, String filter, String block) {
        return getFilteredImages(context, filter, block, 0);
    }

    public ArrayList<String> getFilteredImages(Context context, String filter, String block, int limit) {
        ArrayList<String> result = new ArrayList<>();
        String[] filters = filter.split(",");
        String[] blocks=null;
        if (!TextUtils.isEmpty(block)) {
            //因为split即使字符串是空的他也会有一个空值在里边 所以这里故意让空字符串的数组为null方便判断
            blocks = block.split(",");
        }
        Cursor c = null;
        try {
            c = context.getContentResolver().query(MediaStore.Files.getContentUri("external"),
                    new String[]{"_id", MediaStore.Files.FileColumns.DATA}, null, null, null);
            int dataindex = c.getColumnIndex(MediaStore.Files.FileColumns.DATA);

            while (c.moveToNext()) {
                String path = c.getString(dataindex);
                if (isImg(path) && isContainsKeyWords(path, filters)&&!isContainsKeyWords(path,blocks)) {
                    result.add(path);
                }
                if (limit != 0 && result.size() > limit) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }

        Collections.shuffle(result);
        return result;
    }

    public boolean isImg(String path) {
        path = path.toLowerCase();
        if (path.endsWith(".png")) {
            return true;
        }
        if (path.endsWith(".gif")) {
            return true;
        }
        if (path.endsWith(".jpg")) {
            return true;
        }
        if (path.endsWith(".jpeg")) {
            return true;
        }
        return false;
    }

    public boolean isContainsKeyWords(String text, String[] keys) {
        if (keys == null || keys.length == 0) {
            Log.d(TAG, "keys are empty");
            return false;
        }
        Log.d(TAG, "keys count:"+keys.length);
        text = text.toLowerCase();
        for (String key : keys) {
            Log.d(TAG, "key:"+key);
            if (text.contains(key.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
