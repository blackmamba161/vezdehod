#!/usr/bin/env python3
"""Reject unaligned ARM64 ELF inputs and unresolved Go symbols before packaging."""
from pathlib import Path
import struct, subprocess, zipfile
root=Path(__file__).resolve().parents[1]
with zipfile.ZipFile(root/'vendor/sing-box-1.12.12/libbox.aar') as archive:
    binaries={'libbox':archive.read('jni/arm64-v8a/libbox.so')}
binaries['xray']=(root/'vendor/xray/xray').read_bytes()
for name,data in binaries.items():
    if data[:6]!=b'\x7fELF\x02\x01' or struct.unpack_from('<H',data,18)[0]!=183:
        raise SystemExit(f'{name}: expected little-endian ARM64 ELF')
    offset=struct.unpack_from('<Q',data,32)[0]
    size,count=struct.unpack_from('<HH',data,54)
    loads=0
    for index in range(count):
        kind,flags,file_offset,address,physical,file_size,memory_size,align=struct.unpack_from('<IIQQQQQQ',data,offset+index*size)
        if kind==1:
            loads+=1
            if align<16384 or (file_offset-address)%16384:
                raise SystemExit(f'{name}: LOAD segment does not support 16 KB pages')
    if not loads: raise SystemExit(f'{name}: missing LOAD segments')
    import tempfile
    with tempfile.NamedTemporaryFile() as file:
        file.write(data);file.flush()
        symbols=subprocess.check_output(['readelf','--wide','--dyn-syms',file.name],text=True)
    for line in symbols.splitlines():
        if ' UND ' in line and any(marker in line for marker in ['golang.org/','github.com/','runtime.']):
            raise SystemExit(f'{name}: unresolved Go symbol: {line.strip()}')
    print(f'PASS {name}: ARM64, 16 KB ELF alignment, no unresolved Go package symbols')
