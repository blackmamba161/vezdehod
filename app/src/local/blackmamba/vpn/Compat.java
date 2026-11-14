package local.blackmamba.vpn;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
/** Java APIs available on every supported Android version (API 29+). */
final class Compat {
 private Compat() {}
 static String readString(Path path)throws IOException{return new String(Files.readAllBytes(path),StandardCharsets.UTF_8);}
 static void writeString(Path path,CharSequence text)throws IOException{Files.write(path,text.toString().getBytes(StandardCharsets.UTF_8));}
 static byte[] readAll(InputStream input)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1)out.write(buffer,0,count);return out.toByteArray();}
 static boolean blank(String text){for(int i=0;i<text.length();){int cp=text.codePointAt(i);if(!Character.isWhitespace(cp))return false;i+=Character.charCount(cp);}return true;}
 @SafeVarargs static <T> Set<T> set(T... values){return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(values)));}
 static String repeat(String text,int count){if(count<0)throw new IllegalArgumentException("Negative repeat count");StringBuilder result=new StringBuilder();for(int i=0;i<count;i++)result.append(text);return result.toString();}
}
