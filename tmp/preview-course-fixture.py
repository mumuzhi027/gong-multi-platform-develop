"""Temporary emulator-only examples; restore the cache after screenshot capture."""
import json
import sqlite3
import subprocess
import sys
from pathlib import Path

ADB = 'C:/Users/LENOVO/AppData/Local/Android/Sdk/platform-tools/adb.exe'
PACKAGE = 'com.sky31.gongmultiplatform'
WORK = Path(__file__).parent / 'course-preview-cache'

def adb(*args):
    return subprocess.run([ADB, '-s', 'emulator-5554', *args], check=True, capture_output=True).stdout

def push_database(path):
    adb('push', str(path), '/data/local/tmp/gong-preview.db')
    adb('shell', 'run-as', PACKAGE, 'cp', '/data/local/tmp/gong-preview.db', 'databases/gong_room.db')
    adb('shell', 'run-as', PACKAGE, 'rm', '-f', 'databases/gong_room.db-wal', 'databases/gong_room.db-shm')
    adb('shell', 'rm', '/data/local/tmp/gong-preview.db')

adb('shell', 'am', 'force-stop', PACKAGE)
if sys.argv[1] == 'prepare':
    WORK.mkdir(exist_ok=False)
    raw = WORK / 'gong_room.db'
    raw.write_bytes(adb('exec-out', 'run-as', PACKAGE, 'cat', 'databases/gong_room.db'))
    wal = subprocess.run([ADB, '-s', 'emulator-5554', 'exec-out', 'run-as', PACKAGE, 'cat', 'databases/gong_room.db-wal'], capture_output=True)
    if wal.returncode == 0 and wal.stdout:
        Path(str(raw) + '-wal').write_bytes(wal.stdout)
    with sqlite3.connect(raw) as source, sqlite3.connect(WORK / 'original.db') as original:
        source.backup(original)
    with sqlite3.connect(WORK / 'original.db') as original, sqlite3.connect(WORK / 'preview.db') as preview:
        original.backup(preview)
        examples = [
            dict(name='错峰示例课（一）', teacher='示例', classroom='逸夫楼-308', weeks='1-30', start_time=1, duration=2, day='Wednesday', is_custom=True),
            dict(name='错峰示例课（二）', teacher='示例', classroom='信息科技大楼-203', weeks='1-30', start_time=3, duration=2, day='Wednesday', is_custom=True),
        ]
        preview.execute('UPDATE courseEntity SET courses=? WHERE uid=1', (json.dumps({'courses': {'Wednesday': examples}, 'deletedRemoteCourses': []}, ensure_ascii=False),))
    preview.close()
    original.close()
    source.close()
    push_database(WORK / 'preview.db')
    print('Temporary examples ready; original cache saved for restoration.')
elif sys.argv[1] == 'apply':
    push_database(WORK / 'preview.db')
    print('Examples applied after local SQLite checkpoint.')
elif sys.argv[1] == 'restore':
    assert (WORK / 'original.db').is_file()
    push_database(WORK / 'original.db')
    for path in WORK.iterdir():
        path.unlink()
    WORK.rmdir()
    print('Original cache restored; temporary database copies removed.')
else:
    raise ValueError('Use prepare or restore')
