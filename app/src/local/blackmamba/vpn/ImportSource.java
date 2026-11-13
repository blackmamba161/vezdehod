package local.blackmamba.vpn;
import java.net.URI;
import java.util.Locale;

final class ImportSource {
 static String normalize(String raw)throws Exception{
  String s=raw==null?"":raw.replace("\uFEFF","").trim();
  if(s.isEmpty())throw new Exception(L10n.s(R.string.msg_40a747d563));
  if(s.length()>65536||s.matches("(?s).*\\s.*"))throw new Exception(L10n.s(R.string.msg_680a8e4f43));
  URI uri;try{uri=new URI(s);}catch(Exception e){throw new Exception(L10n.s(R.string.msg_102b915c5e));}
  String scheme=uri.getScheme()==null?"":uri.getScheme().toLowerCase(Locale.ROOT);
  if(scheme.equals("https")){if(uri.getHost()==null||uri.getUserInfo()!=null)throw new Exception(L10n.s(R.string.msg_2a16f698ef));}
  else if(!java.util.Arrays.asList("vless","vmess","trojan","ss","hysteria2","hy2").contains(scheme))throw new Exception(L10n.s(R.string.msg_c01d472ca8));
  if(s.substring(s.indexOf(':')+1).replace("/","").isEmpty())throw new Exception(L10n.s(R.string.msg_cacc66427a));
  return scheme+s.substring(s.indexOf(':'));
 }
 static boolean subscription(String s){return s.regionMatches(true,0,"https://",0,8);}
}
