#!/usr/bin/env python3
"""Verify both packaged Android architectures, ELF alignment and Go linkage."""
from pathlib import Path
import struct, subprocess, tempfile, zipfile
root = Path(__file__).resolve().parents[1]
with zipfile.ZipFile(root / 'vendor/sing-box-1.12.12/libbox.aar') as archive:
    binaries = {
        ('libbox', abi): archive.read('jni/' + abi + '/libbox.so')
        for abi in ('arm64-v8a', 'armeabi-v7a')
    }
binaries[('xray', 'arm64-v8a')] = (root / 'vendor/xray/xray').read_bytes()
binaries[('xray', 'armeabi-v7a')] = (root / 'vendor/xray/armeabi-v7a/xray').read_bytes()
for (engine, abi), data in binaries.items():
    name = engine + '/' + abi
    is64 = abi == 'arm64-v8a'
    expected_class, expected_machine = (2, 183) if is64 else (1, 40)
    if data[:4] != b'\x7fELF' or data[4:6] != bytes((expected_class, 1)) or struct.unpack_from('<H', data, 18)[0] != expected_machine:
        raise SystemExit(name + ': wrong ELF architecture')
    if is64:
        offset = struct.unpack_from('<Q', data, 32)[0]
        size, count = struct.unpack_from('<HH', data, 54)
    else:
        offset = struct.unpack_from('<I', data, 28)[0]
        size, count = struct.unpack_from('<HH', data, 42)
    loads = 0
    for index in range(count):
        position = offset + index * size
        if is64:
            kind, flags, file_offset, address, physical, file_size, memory_size, align = struct.unpack_from('<IIQQQQQQ', data, position)
        else:
            kind, file_offset, address, physical, file_size, memory_size, flags, align = struct.unpack_from('<IIIIIIII', data, position)
        if kind == 1:
            loads += 1
            if align < 16384 or (file_offset - address) % 16384:
                raise SystemExit(name + ': LOAD segment does not support 16 KB pages')
    if not loads:
        raise SystemExit(name + ': missing LOAD segments')
    with tempfile.NamedTemporaryFile() as file:
        file.write(data)
        file.flush()
        symbols = subprocess.check_output(['readelf', '--wide', '--dyn-syms', file.name], text=True)
    for line in symbols.splitlines():
        if ' UND ' in line and any(marker in line for marker in ['golang.org/', 'github.com/', 'runtime.']):
            raise SystemExit(name + ': unresolved Go symbol: ' + line.strip())
    print('PASS ' + name + ': correct architecture, 16 KB ELF alignment, no unresolved Go package symbols')
