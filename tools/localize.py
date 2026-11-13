#!/usr/bin/env python3
"""One-time literal migration and deterministic generation of packaged Android strings."""
from pathlib import Path
import re,json,hashlib,sys
root=Path(__file__).resolve().parents[1]
pattern=re.compile(r'"(?:\\.|[^"\\])*"')
catalog_path=root/'translations/catalog.json'
catalog=json.loads(catalog_path.read_text()) if catalog_path.exists() else {}
skip={'Хельсинки','Белые списки'}
if '--migrate' in sys.argv:
 for path in (root/'app/src/local/blackmamba/vpn').glob('*.java'):
  if path.stem in {'SelfTest','L10n','LocalizedFallback'}:continue
  def replace(m):
   try: value=json.loads(m.group())
   except ValueError:return m.group()
   if not re.search('[А-Яа-яЁё]',value) or value in skip:return m.group()
   key='app_name' if value=='Вездеход' else 'msg_'+hashlib.sha1(value.encode()).hexdigest()[:10]
   catalog.setdefault(key,{'ru':value})
   return 'L10n.s(R.string.'+key+')'
  path.write_text(pattern.sub(replace,path.read_text()))
 catalog.setdefault('app_name',{'ru':'Вездеход','en':'Vezdehod'})
 catalog_path.write_text(json.dumps(catalog,ensure_ascii=False,indent=2)+'\n')
langs=['ru','en','zh','es','hi','ar','pt','fr','bn','de','ja','id']
if '--generate' in sys.argv:
 import xml.sax.saxutils as xml
 def android(s):return xml.escape(s.replace('\\','\\\\').replace('"','\\"').replace("'","\\'").replace('\n','\\n').replace('\t','\\t'))
 for lang in langs:
  folder='values' if lang=='en' else 'values-'+('in' if lang=='id' else 'zh-rCN' if lang=='zh' else lang)
  target=root/'app/res'/folder;target.mkdir(exist_ok=True)
  lines=['<?xml version="1.0" encoding="utf-8"?>','<resources>']
  for key,row in catalog.items():
   if lang not in row:raise RuntimeError('Missing '+lang+': '+key)
   lines.append('    <string name="'+key+'" formatted="false">"'+android(row[lang])+'"</string>')
  lines.append('</resources>');(target/'strings.xml').write_text('\n'.join(lines)+'\n')
 fallback=['package local.blackmamba.vpn;','/** Russian fallback for pure JVM tests before Application initialization. */','final class LocalizedFallback { static String text(int id){switch(id){']
 for key,row in catalog.items():fallback.append('case R.string.'+key+':return '+json.dumps(row['ru'],ensure_ascii=False)+';')
 fallback.append('default:throw new IllegalArgumentException("Unknown string resource: "+id);}}}')
 (root/'app/src/local/blackmamba/vpn/LocalizedFallback.java').write_text('\n'.join(fallback)+'\n')
 (root/'app/res/xml/locales_config.xml').write_text('<?xml version="1.0" encoding="utf-8"?>\n<locale-config xmlns:android="http://schemas.android.com/apk/res/android">\n'+''.join(' <locale android:name="'+('zh-Hans' if l=='zh' else l)+'"/>\n' for l in langs)+'</locale-config>\n')
print('Catalog:',len(catalog),'messages')
