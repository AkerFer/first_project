#!/usr/bin/env python3
"""Package independent website and source distributions without signing keys or runtime data."""
from pathlib import Path
import shutil,zipfile
ROOT=Path(__file__).resolve().parents[1]
VERSION='1.1'
web_out=ROOT/'build'/'web'
if web_out.exists():shutil.rmtree(web_out)
shutil.copytree(ROOT/'web',web_out)
(ROOT/'releases'/'web').mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(ROOT/'releases'/'web'/f'PlanWithMe-web-{VERSION}.zip','w',zipfile.ZIP_DEFLATED) as archive:
 for p in sorted((ROOT/'web').rglob('*')):
  if p.is_file():archive.write(p,p.relative_to(ROOT/'web'))
source_zip=ROOT/'dist'/'PlanWithMe-source.zip'
with zipfile.ZipFile(source_zip,'w',zipfile.ZIP_DEFLATED) as archive:
 for folder in ['web','android','scripts','tests','docs']:
  for p in sorted((ROOT/folder).rglob('*')):
   rel=p.relative_to(ROOT)
   if p.is_file() and not {'build','dist','__pycache__'}&set(rel.parts) and p.suffix not in {'.jks','.keystore','.pyc'}:archive.write(p,rel)
 for name in ['README.md','.gitignore','package.json','package-lock.json']:archive.write(ROOT/name,name)
print('Website:',ROOT/'releases'/'web'/f'PlanWithMe-web-{VERSION}.zip')
print('Source:',source_zip)
