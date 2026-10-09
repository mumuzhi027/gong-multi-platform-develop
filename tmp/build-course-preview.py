from pathlib import Path
import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).parent
font = ImageFont.truetype('C:/Windows/Fonts/msyh.ttc', 24)
small = ImageFont.truetype('C:/Windows/Fonts/msyh.ttc', 19)
board = Image.new('RGB', (1120, 1120), '#ffffff')
draw = ImageDraw.Draw(board)
draw.text((24, 15), '模拟器实机截图 · 临时示例课程', font=font, fill='#171717')
for index, (mode, title, background, text) in enumerate([
    ('light', '白天模式', '#f1f1f1', '#171717'),
    ('dark', '黑夜模式', '#1c1c1c', '#ffffff'),
]):
    y = 65 + index * 520
    draw.rectangle((12, y, 1108, y + 505), fill=background)
    draw.text((30, y + 12), title + ' · 首页', font=font, fill=text)
    draw.text((768, y + 12), '桌面小组件（静态）', font=small, fill=text)
    home = Image.open(ROOT / f'flow-home-{mode}.png').crop((0, 330, 1080, 1030)).resize((704, 456), Image.Resampling.LANCZOS)
    widget = Image.open(ROOT / f'flow-widget-{mode}.png').crop((47, 384, 525, 960)).resize((332, 400), Image.Resampling.LANCZOS)
    board.paste(home, (30, y + 48))
    board.paste(widget, (764, y + 70))
board.save(ROOT / 'home-widget-preview.png')

cap = cv2.VideoCapture(str(ROOT / 'flow-home-motion-dark.mp4'))
frames = []
for tick in range(72):
    cap.set(cv2.CAP_PROP_POS_MSEC, 500 + tick * 83.333)
    ok, frame = cap.read()
    if not ok:
        break
    shot = Image.fromarray(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB))
    frames.append(shot.crop((0, 330, 1080, 990)).resize((756, 462), Image.Resampling.LANCZOS))
assert len(frames) >= 48
frames[0].save(ROOT / 'home-flow-preview.gif', save_all=True, append_images=frames[1:], duration=83, loop=0)

changed = []
for frame in frames[::18][1:]:
    first = np.asarray(frames[0]).astype(np.int16)
    later = np.asarray(frame).astype(np.int16)
    changed.append(int(np.count_nonzero(np.max(np.abs(first - later), axis=2) > 30)))
assert all(count > 100 for count in changed), changed
strip = Image.new('RGB', (1080, 250), '#1c1c1c')
for index, frame in enumerate(frames[::18]):
    strip.paste(frame.crop((238, 130, 714, 400)).resize((270, 153)), (index * 270, 40))
strip.save(ROOT / 'flow-motion-frames.png')
print('Animation verified across four moments:', changed, 'changed pixels.')
print('Saved home-widget-preview.png and home-flow-preview.gif')
