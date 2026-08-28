from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path('Murimblock_GUI_Kit')
S = 4
P = {
    'transparent': (0,0,0,0), 'paper': (238,231,215,255), 'paper_hi': (250,245,232,255),
    'paper_shadow': (207,196,174,255), 'ink': (25,29,30,255), 'ink2': (45,50,50,255),
    'line': (73,76,72,255), 'muted': (128,128,117,255), 'gold': (190,143,46,255),
    'gold_hi': (231,190,87,255), 'qi': (54,175,205,255), 'qi_hi': (115,218,232,255),
    'disabled': (77,80,78,255), 'white': (246,240,224,255)
}

FONT = ImageFont.truetype('C:/Windows/Fonts/consola.ttf', 7)
FONT_B = ImageFont.truetype('C:/Windows/Fonts/consolab.ttf', 7)
FONT_T = None  # sentinel: large hand-built 5x7 pixel alphabet

GLYPHS = {
 'A':['01110','10001','10001','11111','10001','10001','10001'], 'B':['11110','10001','10001','11110','10001','10001','11110'],
 'C':['01111','10000','10000','10000','10000','10000','01111'], 'D':['11110','10001','10001','10001','10001','10001','11110'],
 'E':['11111','10000','10000','11110','10000','10000','11111'], 'F':['11111','10000','10000','11110','10000','10000','10000'],
 'G':['01111','10000','10000','10111','10001','10001','01111'], 'H':['10001','10001','10001','11111','10001','10001','10001'],
 'I':['11111','00100','00100','00100','00100','00100','11111'], 'J':['00111','00010','00010','00010','10010','10010','01100'],
 'K':['10001','10010','10100','11000','10100','10010','10001'], 'L':['10000','10000','10000','10000','10000','10000','11111'],
 'M':['10001','11011','10101','10101','10001','10001','10001'], 'N':['10001','11001','10101','10011','10001','10001','10001'],
 'O':['01110','10001','10001','10001','10001','10001','01110'], 'P':['11110','10001','10001','11110','10000','10000','10000'],
 'Q':['01110','10001','10001','10001','10101','10010','01101'], 'R':['11110','10001','10001','11110','10100','10010','10001'],
 'S':['01111','10000','10000','01110','00001','00001','11110'], 'T':['11111','00100','00100','00100','00100','00100','00100'],
 'U':['10001','10001','10001','10001','10001','10001','01110'], 'V':['10001','10001','10001','10001','10001','01010','00100'],
 'W':['10001','10001','10001','10101','10101','10101','01010'], 'X':['10001','10001','01010','00100','01010','10001','10001'],
 'Y':['10001','10001','01010','00100','00100','00100','00100'], 'Z':['11111','00001','00010','00100','01000','10000','11111'],
 '0':['01110','10001','10011','10101','11001','10001','01110'], '1':['00100','01100','00100','00100','00100','00100','01110'],
 '2':['01110','10001','00001','00010','00100','01000','11111'], '3':['11110','00001','00001','01110','00001','00001','11110'],
 '4':['00010','00110','01010','10010','11111','00010','00010'], '5':['11111','10000','10000','11110','00001','00001','11110'],
 '6':['01110','10000','10000','11110','10001','10001','01110'], '7':['11111','00001','00010','00100','01000','01000','01000'],
 '8':['01110','10001','10001','01110','10001','10001','01110'], '9':['01110','10001','10001','01111','00001','00001','01110'],
 '?':['01110','10001','00001','00010','00100','00000','00100'], '/':['00001','00010','00010','00100','01000','01000','10000'],
 '.':['00000','00000','00000','00000','00000','00100','00100'], ':':['00000','00100','00100','00000','00100','00100','00000'],
 '-':['00000','00000','00000','11111','00000','00000','00000'], ' ':['00000']*7
}

def canvas(w,h,bg='transparent'):
    return Image.new('RGBA',(w,h),P[bg])

def pxtext(im, xy, text, font=FONT, fill=None, anchor='la'):
    fill = fill or P['ink']
    if font is FONT_T:
        scale=2; text=text.upper(); mw=max(1,len(text)*6*scale-scale); mh=7*scale; x,y=xy
        if 'm' in anchor: x-=mw//2
        elif 'r' in anchor: x-=mw
        if anchor.endswith('m'): y-=mh//2
        elif anchor.endswith('b'): y-=mh
        d=ImageDraw.Draw(im)
        for i,ch in enumerate(text):
            for gy,row in enumerate(GLYPHS.get(ch,GLYPHS['?'])):
                for gx,bit in enumerate(row):
                    if bit=='1':
                        xx=int(x+i*6*scale+gx*scale); yy=int(y+gy*scale)
                        d.rectangle((xx,yy,xx+scale-1,yy+scale-1),fill=fill)
        return
    text=text.upper(); mw=max(1,len(text)*5-1); mh=7; x,y=xy
    if 'm' in anchor: x-=mw//2
    elif 'r' in anchor: x-=mw
    if anchor.endswith('m'): y-=mh//2
    elif anchor.endswith('b'): y-=mh
    d=ImageDraw.Draw(im); xmap=(0,1,2,2,3)
    for i,ch in enumerate(text):
        for gy,row in enumerate(GLYPHS.get(ch,GLYPHS['?'])):
            for gx,bit in enumerate(row):
                if bit=='1': d.point((int(x+i*5+xmap[gx]),int(y+gy)),fill=fill)

def line(draw,pts,fill='line',width=1): draw.line(pts,fill=P[fill],width=width)

def pixel_frame(im, box, fill='paper', outline='ink', inner=True):
    d=ImageDraw.Draw(im); x0,y0,x1,y1=box
    d.rectangle(box,fill=P[fill],outline=P[outline],width=2)
    if inner: d.rectangle((x0+3,y0+3,x1-3,y1-3),outline=P['line'])
    # contained 5 px stepped corner ornaments
    c=P['line']
    for sx,sy in ((1,1),(-1,1),(1,-1),(-1,-1)):
        ox=x0+3 if sx==1 else x1-3; oy=y0+3 if sy==1 else y1-3
        d.line([(ox,oy+sy*5),(ox,oy),(ox+sx*5,oy)],fill=c)
        d.line([(ox+sx*2,oy+sy*5),(ox+sx*2,oy+sy*2),(ox+sx*5,oy+sy*2)],fill=c)

def diamond(d,cx,cy,color='line'):
    d.polygon([(cx,cy-2),(cx+2,cy),(cx,cy+2),(cx-2,cy)],outline=P[color])

def header(im,title):
    d=ImageDraw.Draw(im)
    pxtext(im,(135,12),title,FONT_T,P['ink'],'ma')
    line(d,[(15,19),(58,19)],'line'); line(d,[(212,19),(235,19)],'line')
    diamond(d,64,19); diamond(d,206,19)
    line(d,[(7,28),(262,28)],'line')
    close_button(im,(248,7),'normal')

def close_button(im,xy,state='normal'):
    x,y=xy; d=ImageDraw.Draw(im)
    bg='ink2' if state=='normal' else 'ink'; edge='line' if state=='normal' else 'gold'
    d.rectangle((x,y,x+13,y+13),fill=P[bg],outline=P[edge])
    d.rectangle((x+2,y+2,x+11,y+11),outline=P['paper_shadow'])
    line(d,[(x+4,y+4),(x+9,y+9)],'white'); line(d,[(x+9,y+4),(x+4,y+9)],'white')

def nav_button(im,box,text,active=False):
    d=ImageDraw.Draw(im); x0,y0,x1,y1=box
    fill='ink' if active else 'paper_hi'; edge='gold' if active else 'line'
    d.rectangle(box,fill=P[fill],outline=P[edge])
    d.line([(x0+2,y0),(x0,y0+2)],fill=P['paper']); d.line([(x1-2,y0),(x1,y0+2)],fill=P['paper'])
    d.line([(x0+2,y1),(x0,y1-2)],fill=P['paper']); d.line([(x1-2,y1),(x1,y1-2)],fill=P['paper'])
    d.line([(x0+4,y0+2),(x1-4,y0+2)],fill=P['gold'] if active else P['paper_shadow'])
    pxtext(im,((x0+x1)//2,y0+4),text,FONT_B,P['white'] if active else P['ink'],'ma')

def nav(im,active):
    labels=['Profil','Techniques','Cultivation','Infos']
    for i,(x,t) in enumerate(zip((8,72,136,200),labels)):
        nav_button(im,(x,158,x+61,173),t,i==active)

def qi_mark(im,xy,size=14):
    x,y=xy; d=ImageDraw.Draw(im); c=P['qi']
    d.ellipse((x,y,x+size-1,y+size-1),outline=c,width=1)
    d.rectangle((x+3,y+3,x+size-4,y+size-4),outline=P['qi_hi'])
    d.polygon([(x+size//2,y+4),(x+size-4,y+size//2),(x+size//2,y+size-4),(x+4,y+size//2)],outline=c)
    d.point((x+size//2,y+size//2),fill=P['qi_hi'])

def qi_bar(im,xy,w=100,pct=.725):
    x,y=xy; d=ImageDraw.Draw(im)
    d.rectangle((x,y,x+w-1,y+7),fill=P['ink'],outline=P['line'])
    d.rectangle((x+2,y+2,x+w-3,y+5),fill=P['ink2'])
    fw=int((w-4)*pct)
    if fw > 0:
        d.rectangle((x+2,y+2,x+1+fw,y+5),fill=P['qi'])
        d.line((x+3,y+2,x+fw,y+2),fill=P['qi_hi'])

def separator(im,x0,x1,y):
    d=ImageDraw.Draw(im); mid=(x0+x1)//2
    line(d,[(x0,y),(mid-4,y)],'line'); line(d,[(mid+4,y),(x1,y)],'line'); diamond(d,mid,y)

def profile_mockup():
    im=canvas(270,179,'paper'); pixel_frame(im,(1,1,268,177)); header(im,'MURIMBLOCK')
    d=ImageDraw.Draw(im)
    # 3D player viewport
    pixel_frame(im,(94,34,176,101),'paper_hi','line')
    d.ellipse((111,87,159,95),outline=P['gold'])
    line(d,[(135,53),(135,79)],'muted'); line(d,[(122,79),(148,79)],'muted')
    for yy in range(54,79,4): d.point((135,yy),fill=P['paper_hi'])
    pxtext(im,(135,66),'ZONE JOUEUR 3D',FONT,P['muted'],'ma')
    pxtext(im,(135,105),'EnzoBertrand',FONT_B,P['ink'],'ma')
    pxtext(im,(135,115),'ROYAUME',FONT,P['muted'],'ma')
    pxtext(im,(135,124),'Qi Refining III',FONT_B,P['ink'],'ma')
    separator(im,109,161,132)
    qi_mark(im,(66,137),13); pxtext(im,(83,135),'QI',FONT_B,P['muted'],'la')
    qi_bar(im,(83,143),104,.725); pxtext(im,(192,140),'725 / 1000',FONT,P['ink'],'la')
    line(d,[(7,154),(262,154)],'line')
    nav(im,0)
    return im

def tome_row(im,box,text,state='normal'):
    d=ImageDraw.Draw(im); x0,y0,x1,y1=box
    fill='ink' if state in ('normal','selected') else 'ink2'; edge='gold' if state=='selected' else 'line'
    d.rectangle(box,fill=P[fill],outline=P[edge])
    d.rectangle((x0+2,y0+2,x1-2,y1-2),outline=P['gold_hi'] if state=='selected' else P['disabled'])
    if state=='locked':
        d.rectangle((x0+5,y0+6,x0+10,y0+11),outline=P['muted']); d.arc((x0+6,y0+2,x0+10,y0+8),180,360,fill=P['muted'])
    pxtext(im,((x0+x1)//2,y0+4),text,FONT_B,P['white'] if state!='locked' else P['muted'],'ma')

def technique_slot(im,box,state='normal',mark='slash'):
    d=ImageDraw.Draw(im); x0,y0,x1,y1=box
    edge='gold' if state=='selected' else 'disabled' if state in ('locked','unknown') else 'line'
    d.rectangle(box,fill=P['ink'],outline=P[edge]); d.rectangle((x0+2,y0+2,x1-2,y1-2),outline=P['ink2'])
    if mark=='slash':
        d.polygon([(x0+6,y1-5),(x0+9,y1-6),(x1-5,y0+5),(x1-8,y0+11),(x0+12,y1-7)],fill=P['white'])
    elif state=='locked':
        d.rectangle((x0+8,y0+10,x0+15,y0+17),outline=P['muted']); d.arc((x0+9,y0+5,x0+14,y0+12),180,360,fill=P['muted'])
    else: pxtext(im,((x0+x1)//2,y0+6),'?',FONT_B,P['muted'],'ma')

def techniques_mockup():
    im=canvas(270,179,'paper'); pixel_frame(im,(1,1,268,177)); header(im,'TECHNIQUES')
    d=ImageDraw.Draw(im); line(d,[(77,35),(77,149)],'line'); line(d,[(184,35),(184,149)],'line')
    pxtext(im,(42,34),'TOMES',FONT_B,P['ink'],'ma'); separator(im,9,70,44)
    tome_row(im,(9,49,69,63),'Tome Epee I','selected'); tome_row(im,(9,68,69,82),'Tome Epee II')
    tome_row(im,(9,87,69,101),'???','locked'); tome_row(im,(9,106,69,120),'???','locked')
    pxtext(im,(130,34),'TECHNIQUES',FONT_B,P['ink'],'ma'); separator(im,84,177,44)
    technique_slot(im,(86,51,109,74),'selected'); technique_slot(im,(116,51,139,74)); technique_slot(im,(146,51,169,74),'locked','lock')
    technique_slot(im,(101,82,124,105)); technique_slot(im,(131,82,154,105),'unknown','?')
    pxtext(im,(226,34),'DETAILS',FONT_B,P['ink'],'ma'); separator(im,190,262,44)
    pxtext(im,(190,51),'Slash frontal',FONT_B,P['ink'],'la'); pxtext(im,(190,65),'Arc de lame',FONT,P['ink'],'la'); pxtext(im,(190,73),'rapide.',FONT,P['ink'],'la')
    pxtext(im,(190,84),'Cout Qi',FONT,P['muted'],'la'); pxtext(im,(257,84),'25',FONT_B,P['ink'],'ra')
    pxtext(im,(190,98),'Cooldown',FONT,P['muted'],'la'); pxtext(im,(257,98),'4 s',FONT_B,P['ink'],'ra')
    nav_button(im,(196,116,257,131),'EQUIPER',True)
    line(d,[(7,154),(262,154)],'line'); nav(im,1)
    return im

def save_pair(folder,name,im):
    one=ROOT/'Minecraft_1x'/folder/f'{name}.png'; hi=ROOT/folder/f'{name}.png'
    one.parent.mkdir(parents=True,exist_ok=True); hi.parent.mkdir(parents=True,exist_ok=True)
    im.save(one)
    im.resize((im.width*S,im.height*S),Image.Resampling.NEAREST).save(hi)

def asset_button(state):
    im=canvas(62,16); nav_button(im,(0,0,61,15),'',state in ('selected','active')); return im

def asset_slot(state):
    im=canvas(24,24); technique_slot(im,(0,0,23,23),state,'lock' if state=='locked' else '?' if state=='unknown' else 'slash'); return im

def asset_tome(state):
    im=canvas(61,15); tome_row(im,(0,0,60,14),'',state); return im

def asset_panel(w,h,dark=False):
    im=canvas(w,h); pixel_frame(im,(0,0,w-1,h-1),'ink' if dark else 'paper'); return im

def icon_asset(kind):
    im=canvas(26,26); d=ImageDraw.Draw(im); c=P['white']
    if kind=='profile':
        d.rectangle((10,5,15,10),fill=c); d.rectangle((7,13,18,21),fill=c)
    elif kind in ('book_closed','book_open'):
        d.rectangle((4,5,12,20),outline=c); d.rectangle((13,5,21,20),outline=c); line(d,[(13,6),(13,20)],'white')
        if kind=='book_open': line(d,[(6,8),(10,8)],'white'); line(d,[(15,8),(19,8)],'white')
    elif kind=='scroll':
        d.rectangle((7,4,18,21),outline=c); line(d,[(9,9),(16,9)],'white'); line(d,[(9,13),(16,13)],'white')
    elif kind=='cultivation':
        d.polygon([(13,5),(10,12),(13,18),(16,12)],outline=c); d.polygon([(4,13),(13,21),(22,13),(18,21),(8,21)],outline=c)
    elif kind=='info':
        d.ellipse((4,4,21,21),outline=c); d.rectangle((12,10,13,17),fill=c); d.point((12,7),fill=c)
    elif kind=='lock':
        d.rectangle((7,11,18,21),outline=c); d.arc((8,4,17,14),180,360,fill=c)
    elif kind=='unknown':
        pxtext(im,(13,5),'?',FONT_B,c,'ma')
    return im

def decor_asset(kind):
    if kind=='separator':
        im=canvas(160,11); separator(im,1,158,5); return im
    if kind=='diamond':
        im=canvas(12,12); diamond(ImageDraw.Draw(im),6,6); return im
    if kind=='corner':
        im=canvas(24,24); d=ImageDraw.Draw(im); line(d,[(2,20),(2,2),(20,2)],'line',2); line(d,[(6,20),(6,6),(20,6)],'line'); return im
    if kind=='brush_circle':
        im=canvas(60,60); d=ImageDraw.Draw(im); d.arc((7,7,52,52),25,320,fill=P['ink'],width=4); d.arc((11,11,48,48),45,300,fill=P['line'],width=1); return im
    if kind=='mountains':
        im=canvas(160,55); d=ImageDraw.Draw(im); d.polygon([(0,54),(30,34),(46,43),(79,12),(105,42),(126,26),(159,54)],fill=(45,50,50,70)); line(d,[(0,54),(30,34),(46,43),(79,12),(105,42),(126,26),(159,54)],'line'); return im
    if kind=='bamboo':
        im=canvas(60,125); d=ImageDraw.Draw(im); line(d,[(17,122),(22,80),(23,45),(21,4)],'ink2',2); line(d,[(21,84),(39,64)],'ink2'); line(d,[(22,52),(8,34)],'ink2'); line(d,[(23,32),(41,17)],'ink2')
        for pts in [((31,71),(49,68),(39,77)),((10,38),(0,34),(15,45)),((35,23),(52,20),(42,29)),((16,90),(2,88),(14,98))]: d.polygon(pts,fill=P['ink2'])
        return im

def generate_assets():
    save_pair('Mockups','Profile_GUI',profile_mockup()); save_pair('Mockups','Techniques_GUI',techniques_mockup())
    for state in ('normal','hover','pressed','disabled','selected'):
        save_pair('Buttons',f'button_{state}',asset_button('selected' if state=='selected' else 'normal'))
    for state in ('normal','hover','active','disabled'):
        save_pair('Tabs',f'tab_{state}',asset_button(state))
    for state in ('normal','hover','selected','locked','unknown'):
        save_pair('Techniques',f'technique_slot_{state}',asset_slot(state))
    for state in ('normal','selected','locked','unknown'):
        save_pair('Tomes',f'tome_slot_{state}',asset_tome(state))
    im=canvas(14,14); close_button(im,(0,0)); save_pair('Buttons','close_normal',im)
    im=canvas(14,14); close_button(im,(0,0),'hover'); save_pair('Buttons','close_hover',im)
    im=canvas(14,14); qi_mark(im,(0,0),14); save_pair('Qi','qi_icon',im)
    im=canvas(104,8); qi_bar(im,(0,0),104,0); save_pair('Qi','qi_bar_background',im)
    im=canvas(100,4); ImageDraw.Draw(im).rectangle((0,0,99,3),fill=P['qi']); ImageDraw.Draw(im).line((0,0,99,0),fill=P['qi_hi']); save_pair('Qi','qi_bar_fill',im)
    im=canvas(100,4); ImageDraw.Draw(im).rectangle((0,0,99,3),fill=P['gold']); ImageDraw.Draw(im).line((0,0,99,0),fill=P['gold_hi']); save_pair('Qi','qi_bar_fill_gold',im)
    save_pair('Panels','panel_base',asset_panel(160,105)); save_pair('Panels','panel_details',asset_panel(105,125,True)); save_pair('Panels','player_viewport',asset_panel(100,105))
    save_pair('Panels','gui_profile_background',asset_panel(270,179)); save_pair('Panels','gui_techniques_background',asset_panel(270,179))
    for name in ('profile','book_closed','book_open','scroll','cultivation','info','lock','unknown'):
        save_pair('Icons',f'icon_{name}',icon_asset(name))
    for name in ('separator','diamond','corner','brush_circle','mountains','bamboo'):
        save_pair('Decorations',f'decor_{name}',decor_asset(name))

if __name__=='__main__':
    generate_assets()
