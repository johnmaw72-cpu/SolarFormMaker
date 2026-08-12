package com.infinitygreenpower.solarform;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

public class SimpleFileProvider extends ContentProvider {
    @Override public boolean onCreate(){ return true; }
    private File fileFromUri(Uri uri){
        String name = uri.getLastPathSegment();
        return new File(getContext().getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS), name);
    }
    @Override public String getType(Uri uri){ return "application/pdf"; }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(fileFromUri(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder){
        File f=fileFromUri(uri); MatrixCursor c=new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        c.addRow(new Object[]{f.getName(),f.length()}); return c;
    }
    @Override public int delete(Uri uri,String s,String[] a){return 0;}
    @Override public int update(Uri uri,ContentValues v,String s,String[] a){return 0;}
    @Override public Uri insert(Uri uri,ContentValues v){return null;}
}
