#!/usr/bin/env python3
from pathlib import Path
import json,sys,re
root=Path(__file__).resolve().parents[1]
p=root/'translations/catalog.json';data=json.loads(p.read_text());keys=[line.split('|',1)[0] for line in (root/'translations/en.tsv').read_text().splitlines()]
lang=sys.argv[1];lines=(root/'translations'/f'{lang}.txt').read_text().splitlines()
assert len(lines)==len(keys),(lang,len(lines),len(keys))
for key,line in zip(keys,lines):
 value=line.replace('\\n','\n')
 assert sorted(re.findall(r'%(?:\.\d+)?[sdf]',value))==sorted(re.findall(r'%(?:\.\d+)?[sdf]',data[key]['en'])),(key,value)
 # Preserve deliberate boundary spaces of concatenated message fragments.
 en=data[key]['en']
 if en.startswith(' ') and not value.startswith(' '):value=' '+value
 if en.endswith(' ') and not value.endswith(' '):value+=' '
 data[key][lang]=value
p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n');print(lang,len(lines))
