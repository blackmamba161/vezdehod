package local.blackmamba.vpn;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.IDN;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class SubscriptionDeviceId {
 private static final int SECRET_BYTES=32;
 private static final String HEX="0123456789abcdef";

 static String origin(URL url) throws IOException {
  String scheme=url.getProtocol().toLowerCase(Locale.ROOT),host=url.getHost();
  if(!scheme.equals("https"))throw new IOException(L10n.s(R.string.msg_6fc143271e));
  int port=url.getPort()==-1?443:url.getPort();
  if(host.isEmpty()||url.getUserInfo()!=null||port<=0||port>65535)throw new IOException(L10n.s(R.string.msg_2a16f698ef));
  if(!host.startsWith("["))try{host=IDN.toASCII(host,IDN.USE_STD3_ASCII_RULES);}catch(IllegalArgumentException e){throw new IOException(L10n.s(R.string.msg_2a16f698ef));}
  return scheme+"://"+host.toLowerCase(Locale.ROOT)+":"+port;
 }

 static String forOrigin(File noBackupDir,String origin) throws IOException,GeneralSecurityException {
  Mac mac=Mac.getInstance("HmacSHA256");
  mac.init(new SecretKeySpec(secret(noBackupDir),"HmacSHA256"));
  byte[] digest=mac.doFinal(origin.getBytes(StandardCharsets.UTF_8));
  StringBuilder id=new StringBuilder(32);
  for(int i=0;i<16;i++){id.append(HEX.charAt((digest[i]&255)>>>4));id.append(HEX.charAt(digest[i]&15));}
  return id.toString();
 }

 // Imports and auto-refresh share the same process, so concurrent first requests must share one seed.
 private static synchronized byte[] secret(File noBackupDir) throws IOException {
  Path path=new File(noBackupDir,"subscription-hwid").toPath();
  if(Files.exists(path)){
   byte[] stored=Files.readAllBytes(path);
   if(stored.length!=SECRET_BYTES)throw new IOException("Invalid subscription HWID secret");
   return stored;
  }
  byte[] generated=new byte[SECRET_BYTES];new SecureRandom().nextBytes(generated);
  Path temp=Files.createTempFile(path.getParent(),"subscription-hwid-",".tmp");
  try{
   try(FileOutputStream out=new FileOutputStream(temp.toFile())){out.write(generated);out.getFD().sync();}
   Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE);
   return generated;
  }finally{Files.deleteIfExists(temp);}
 }
}
