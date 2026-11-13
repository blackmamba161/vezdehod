package local.blackmamba.vpn;
import org.json.*;
import java.net.URI;
import java.util.regex.*;

/** Public release metadata; never accepts executable URLs outside the configured repository. */
public final class UpdateRelease {
 public static final long MAX_APK=128L*1024*1024;
 static final Pattern ASSET=Pattern.compile("Vezdehod-([0-9]+\\.[0-9]+\\.[0-9]+(?:-[A-Za-z0-9.]+)?)-([1-9][0-9]*)-arm64-v8a\\.apk");
 public final String version,url,sha256,notes,name;public final long code,size;
 public UpdateRelease(JSONObject o)throws Exception{
  version=o.getString("version");url=o.getString("url");sha256=o.getString("sha256");notes=o.optString("notes");name=o.getString("name");code=o.getLong("code");size=o.getLong("size");
  Matcher m=ASSET.matcher(name);if(!m.matches()||!m.group(1).equals(version)||Long.parseLong(m.group(2))!=code||!sha256.matches("[a-f0-9]{64}")||size<=0||size>MAX_APK)throw new IllegalArgumentException("Invalid release metadata");
 }
 public JSONObject json()throws Exception{return new JSONObject().put("version",version).put("url",url).put("sha256",sha256).put("notes",notes).put("name",name).put("code",code).put("size",size);}
 public static boolean repositoryValid(String repo){return repo!=null&&repo.matches("[A-Za-z0-9][A-Za-z0-9-]{0,38}/[A-Za-z0-9_.-]{1,100}")&&!repo.endsWith("/.")&&!repo.endsWith("/..");}
 public static boolean trustedUrl(String url,String repo){try{URI u=new URI(url);return repositoryValid(repo)&&"https".equals(u.getScheme())&&"github.com".equals(u.getHost())&&u.getPort()==-1&&u.getUserInfo()==null&&u.getFragment()==null&&u.getQuery()==null&&u.getPath().startsWith("/"+repo+"/releases/download/")&&!u.getPath().contains("/../");}catch(Exception e){return false;}}
 public static UpdateRelease select(JSONArray releases,String repo,boolean beta,long installed)throws Exception{
  if(!repositoryValid(repo))throw new IllegalArgumentException("Invalid repository");UpdateRelease best=null;
  for(int i=0;i<releases.length();i++){JSONObject r=releases.getJSONObject(i);if(r.optBoolean("draft")||(!beta&&r.optBoolean("prerelease")))continue;
   JSONArray assets=r.optJSONArray("assets");for(int j=0;assets!=null&&j<assets.length();j++){JSONObject a=assets.getJSONObject(j);Matcher m=ASSET.matcher(a.optString("name"));if(!m.matches())continue;
    long code=Long.parseLong(m.group(2));if(code<=installed||(best!=null&&code<=best.code))continue;
    String url=a.optString("browser_download_url"),digest=a.optString("digest");if(!trustedUrl(url,repo)||!digest.matches("sha256:[a-f0-9]{64}"))throw new IllegalArgumentException("Unverified release asset");
    String notes=r.optString("body");if(notes.length()>16000)notes=notes.substring(0,16000);
    best=new UpdateRelease(new JSONObject().put("version",m.group(1)).put("code",code).put("size",a.getLong("size")).put("name",a.getString("name")).put("url",url).put("sha256",digest.substring(7)).put("notes",notes));
   }
  }return best;
 }
}
