from pathlib import Path
import json,re,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1];data=json.loads((root/'translations/catalog.json').read_text());langs=['ru','en','zh','es','hi','ar','pt','fr','bn','de','ja','id']
for key,row in data.items():
 assert set(row)==set(langs),(key,set(row))
 for lang in langs:
  value=row[lang];assert value.strip(),(key,lang,'empty')
  assert sorted(re.findall(r'%(?:\.\d+)?[sdf]',value))==sorted(re.findall(r'%(?:\.\d+)?[sdf]',row['en'])),(key,lang,'format')
  assert value.count('\n')==row['en'].count('\n'),(key,lang,'newlines')
  if lang!='ru':assert not re.search('[А-Яа-яЁё]',value),(key,lang,'untranslated')
for lang in langs:
 folder='values' if lang=='en' else 'values-'+{'zh':'zh-rCN','id':'in'}.get(lang,lang)
 tree=ET.parse(root/'app/res'/folder/'strings.xml');assert {n.attrib['name'] for n in tree.getroot()}==set(data),(lang,'resources')
for path in (root/'app/src/local/blackmamba/vpn').glob('*.java'):
 if path.stem in ['SelfTest','LocalizedFallback','L10n']:continue
 for literal in re.findall(r'"(?:\\.|[^"\\])*"',path.read_text()):
  if re.search('[А-Яа-яЁё]',literal):assert literal in ['"Хельсинки"','"Белые списки"'],(path.name,literal)
print(f'PASS {len(data)} messages × {len(langs)} languages; placeholders, line breaks, resources, no Russian UI literals')
