"""Package the approved EZ bitmap without changing its letterforms or aspect ratio."""
from pathlib import Path
from PIL import Image, ImageDraw
root=Path(__file__).resolve().parents[1];res=root/'app/src/main/res'
mark=Image.open(res/'drawable/ez_logo.png').convert('RGBA')
# Remove only the white paper behind the approved mark for adaptive/monochrome layers.
a=mark.copy();px=a.load()
for y in range(a.height):
 for x in range(a.width):
  r,g,b,_=px[x,y];alpha=255-min(r,g,b);px[x,y]=(r,g,b,alpha)
layer=Image.new('RGBA',(432,432));w=240;h=round(w*mark.height/mark.width)
layer.alpha_composite(a.resize((w,h),Image.Resampling.LANCZOS),((432-w)//2,(432-h)//2))
p=res/'drawable-xxxhdpi';p.mkdir(exist_ok=True);layer.save(p/'launcher_foreground.png')
mono=Image.new('RGBA',layer.size,(17,17,17,0));mono.putalpha(layer.getchannel('A'));mono.save(p/'launcher_monochrome.png')
for density,size in [('mdpi',48),('hdpi',72),('xhdpi',96),('xxhdpi',144),('xxxhdpi',192)]:
 p=res/('mipmap-'+density);p.mkdir(exist_ok=True)
 flat=Image.new('RGBA',(432,432),'white');flat.alpha_composite(layer)
 flat.resize((size,size),Image.Resampling.LANCZOS).convert('RGB').save(p/'ic_launcher.png')
 flat.resize((size,size),Image.Resampling.LANCZOS).convert('RGB').save(p/'ic_launcher_round.png')
for version in ['v26','v33']:
 p=res/('mipmap-anydpi-'+version);p.mkdir(exist_ok=True)
 xml='<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android"><background android:drawable="@color/launcher_background"/><foreground android:drawable="@drawable/launcher_foreground"/>' + ('<monochrome android:drawable="@drawable/launcher_monochrome"/>' if version=='v33' else '')+'</adaptive-icon>'
 for n in ['ic_launcher','ic_launcher_round']:(p/(n+'.xml')).write_text(xml)
# Static packaging previews, not physical-device screenshots.
board=Image.new('RGB',(1200,500),'#f4f1fc');d=ImageDraw.Draw(board)
for row,bg in enumerate(['#fafafa','#1a1d25']):
 d.rectangle((0,row*250,1200,(row+1)*250),fill=bg)
 for col,shape in enumerate(['circle','rounded square','square']):
  x=110+col*380;y=row*250+38;tile=Image.new('RGBA',(160,160),'white');tile.alpha_composite(layer.crop((72,72,360,360)).resize((160,160),Image.Resampling.LANCZOS));mask=Image.new('L',(160,160));md=ImageDraw.Draw(mask)
  if shape=='circle':md.ellipse((0,0,159,159),fill=255)
  elif shape=='rounded square':md.rounded_rectangle((0,0,159,159),radius=38,fill=255)
  else:md.rectangle((0,0,159,159),fill=255)
  board.paste(tile,(x,y),mask);d.text((x+65,y+180),'EZ',fill='#111' if row==0 else '#fff')
(root/'design/launcher-preview.png').parent.mkdir(exist_ok=True);board.save(root/'design/launcher-preview.png')
