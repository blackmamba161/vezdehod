from pathlib import Path
import json,re,xml.sax.saxutils as xml
root=Path(__file__).resolve().parents[1]
repo=json.loads((root/'release-config.json').read_text()).get('repository','')
if repo and not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9-]{0,38}/[A-Za-z0-9_.-]{1,100}',repo):
 raise SystemExit('Invalid GitHub owner/repository in release-config.json')
(root/'app/res/values/update_config.xml').write_text('<resources><string name="update_repository" translatable="false">'+xml.escape(repo)+'</string></resources>\n')
