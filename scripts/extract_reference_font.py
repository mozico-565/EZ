"""Rebuild static TTF weights from the Noto Arabic/Latin WOFF2 embedded in the approved HTML.
Requires fonttools and system libbrotlidec. Original OFL applies; no glyphs redrawn.
Usage: python scripts/extract_reference_font.py /path/EZ-Prototype.html
"""
from pathlib import Path
import base64,ctypes,ctypes.util,re,sys,types,tempfile
from io import BytesIO
lib=ctypes.CDLL(ctypes.util.find_library('brotlidec'))
lib.BrotliDecoderDecompress.argtypes=[ctypes.c_size_t,ctypes.c_void_p,ctypes.POINTER(ctypes.c_size_t),ctypes.c_void_p];lib.BrotliDecoderDecompress.restype=ctypes.c_int
def decompress(d):
    out=ctypes.create_string_buffer(20_000_000);n=ctypes.c_size_t(len(out));src=ctypes.create_string_buffer(d)
    if lib.BrotliDecoderDecompress(len(d),src,ctypes.byref(n),out)!=1:raise ValueError('Invalid Brotli font data')
    return out.raw[:n.value]
sys.modules['brotli']=types.SimpleNamespace(decompress=decompress)
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools.merge import Merger
s=Path(sys.argv[1]).read_text();fonts=[]
for blob in re.findall(r'data:font/woff2;base64,([A-Za-z0-9+/=]+)',s):
    f=TTFont(BytesIO(base64.b64decode(blob)))
    if 'fvar' not in f:continue
    cmap=f.getBestCmap()
    if ord('س') in cmap or (len(cmap)==210 and ord('A') in cmap):fonts.append(f)
assert len(fonts)==2
out=Path(__file__).resolve().parents[1]/'app/src/main/res/font';out.mkdir(exist_ok=True)
with tempfile.TemporaryDirectory() as temp:
    for name,w in [('regular',400),('medium',550),('semibold',650),('bold',700),('extrabold',750)]:
        files=[]
        for i,font in enumerate(fonts):
            f=instantiateVariableFont(font,{'wght':w},inplace=False);f.flavor=None;p=Path(temp)/f'{i}.ttf';f.save(p);files.append(str(p))
        f=Merger().merge(files);f.save(out/f'noto_{name}.ttf')
print('Prepared five exact reference weights')
