package local.blackmamba.vpn;
import android.app.LocaleManager;
import android.content.*;
import android.content.res.*;
import android.os.*;
import java.util.*;

/** Packaged Android resources. Language changes never touch VPN profiles or routes. */
public final class L10n {
 static volatile Context app;private static volatile String cachedTag="";private static volatile Resources cached;private static volatile Locale resolved;
 static final String[] TAGS={"","ru","en","zh-Hans","es","hi","ar","pt","fr","bn","de","ja","id"};
 static final String[] NAMES={"","Русский","English","简体中文","Español","हिन्दी","العربية","Português","Français","বাংলা","Deutsch","日本語","Bahasa Indonesia"};
 static void init(Context c){app=c.getApplicationContext();invalidate();}
 static void invalidate(){cached=null;cachedTag="";resolved=null;}
 static String selection(Context c){if(Build.VERSION.SDK_INT>=33)return c.getSystemService(LocaleManager.class).getApplicationLocales().toLanguageTags();return c.getSharedPreferences("language",0).getString("tag","");}
 static Locale chosen(LocaleList device,String selected){LocaleList list=selected.isEmpty()?device:LocaleList.forLanguageTags(selected);for(int i=0;i<list.size();i++){Locale candidate=list.get(i);String code=candidate.getLanguage();if(code.equals("in"))code="id";for(String tag:TAGS)if(!tag.isEmpty()&&Locale.forLanguageTag(tag).toLanguageTag().split("-")[0].equals(code))return Locale.forLanguageTag(tag);}return Locale.ENGLISH;}
 static Locale locale(){Locale ready=resolved;if(ready!=null)return ready;Context c=app;if(c==null)return Locale.forLanguageTag("ru");LocaleList device=Build.VERSION.SDK_INT>=33?c.getSystemService(LocaleManager.class).getSystemLocales():Resources.getSystem().getConfiguration().getLocales();return resolved=chosen(device,selection(c));}
 static Context wrap(Context c){if(Build.VERSION.SDK_INT>=33)return c;String tag=c.getSharedPreferences("language",0).getString("tag","");if(tag.isEmpty())return c;android.content.res.Configuration cfg=new android.content.res.Configuration(c.getResources().getConfiguration());cfg.setLocales(LocaleList.forLanguageTags(tag));cfg.setLayoutDirection(Locale.forLanguageTag(tag));return c.createConfigurationContext(cfg);}
 static synchronized void select(Context c,String tag){if(!Arrays.asList(TAGS).contains(tag))throw new IllegalArgumentException("Unknown language");invalidate();if(Build.VERSION.SDK_INT>=33)c.getSystemService(LocaleManager.class).setApplicationLocales(LocaleList.forLanguageTags(tag));else c.getSharedPreferences("language",0).edit().putString("tag",tag).apply();}
 public static String s(int id){Context c=app;if(c==null)return LocalizedFallback.text(id);String tag=locale().toLanguageTag();Resources resources=cached;if(resources==null||!cachedTag.equals(tag)){synchronized(L10n.class){android.content.res.Configuration cfg=new android.content.res.Configuration(c.getResources().getConfiguration());Locale loc=Locale.forLanguageTag(tag);cfg.setLocales(new LocaleList(loc));cfg.setLayoutDirection(loc);resources=c.createConfigurationContext(cfg).getResources();cachedTag=tag;cached=resources;}}return resources.getString(id);}
 static String format(int id,Object... values){return String.format(locale(),s(id),values);}
 static boolean rtl(){return android.text.TextUtils.getLayoutDirectionFromLocale(locale())==android.view.View.LAYOUT_DIRECTION_RTL;}
 private L10n(){}
}
