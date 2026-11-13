package local.blackmamba.vpn;
import android.content.*;import android.database.*;import android.net.Uri;import android.os.ParcelFileDescriptor;import android.provider.OpenableColumns;import java.io.*;
/** Exposes only the internally staged APK and only with a temporary read URI grant. */
public final class UpdateProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 private File file(Uri uri)throws FileNotFoundException{if(!"/apk".equals(uri.getPath())||uri.getQuery()!=null)throw new FileNotFoundException();File f=new File(getContext().getFilesDir(),"update-ready.apk");if(!f.isFile())throw new FileNotFoundException();return f;}
 public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException();return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);}
 public String getType(Uri uri){return "application/vnd.android.package-archive";}
 public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){try{File f=file(uri);String[] columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor out=new MatrixCursor(columns);Object[] values=new Object[columns.length];for(int i=0;i<columns.length;i++)values[i]=columns[i].equals(OpenableColumns.DISPLAY_NAME)?"Vezdehod.apk":columns[i].equals(OpenableColumns.SIZE)?f.length():null;out.addRow(values);return out;}catch(FileNotFoundException e){return null;}}
 public Uri insert(Uri uri,ContentValues v){throw new UnsupportedOperationException();}public int update(Uri uri,ContentValues v,String s,String[] args){throw new UnsupportedOperationException();}public int delete(Uri uri,String s,String[] args){throw new UnsupportedOperationException();}
}
