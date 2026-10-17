package com.ezshare.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*

object EzTokens {
    val Ink=Color(0xff111111);val Accent=Color(0xff2385e8);val Lavender=Color(0xffbaa3f6)
    val Orange=Color(0xffffc568);val Blue=Color(0xffbbd8ff);val Pink=Color(0xfff3b4eb)
    val CategoryPink=Color(0xfff7d9e8);val CategoryPurple=Color(0xffe4daff);val CategoryBlue=Color(0xffd4ebff);val Peach=Color(0xffffe1c8)
    val Page=24.dp;val Card=28.dp;val Touch=48.dp;val NavHeight=74.dp
    const val PageMs=280;const val PressMs=100;const val SpringStiffness=380f;const val SpringDamping=.87f
}
data class EzPalette(val background:Color=Color.White,val surface:Color=Color.White,val soft:Color=Color(0xfff7f7f8),val ink:Color=EzTokens.Ink,val muted:Color=Color(0xff71717a),val line:Color=Color(0xffe9e9ef),val dark:Boolean=false) {
    val lavender get()=if(dark)Color(0xffb5a0e9) else EzTokens.Lavender
    val orange get()=if(dark)Color(0xffddb074) else EzTokens.Orange
    val blue get()=if(dark)Color(0xff9abde4) else EzTokens.Blue
    val pink get()=if(dark)Color(0xffd0a1c7) else EzTokens.Pink
}
val LocalEzPalette=staticCompositionLocalOf{EzPalette()}
val LocalEzQuiet=staticCompositionLocalOf{false}
private val Arabic=FontFamily(Font(R.font.noto_regular,FontWeight.Normal),Font(R.font.noto_medium,FontWeight.Medium),Font(R.font.noto_semibold,FontWeight.SemiBold),Font(R.font.noto_bold,FontWeight.Bold),Font(R.font.noto_extrabold,FontWeight.ExtraBold))
@Composable fun EzTheme(dark:Boolean=false,quiet:Boolean=false,content: @Composable () -> Unit){
    val p=if(dark)EzPalette(Color(0xff191c22),Color(0xff20232a),Color(0xff292d35),Color(0xfff2f2f5),Color(0xffb6bac5),Color(0xff3c414d),true) else EzPalette()
    val style=TextStyle(fontFamily=Arabic,fontSize=14.sp,lineHeight=22.sp,platformStyle=PlatformTextStyle(includeFontPadding=false))
    CompositionLocalProvider(LocalEzPalette provides p,LocalEzQuiet provides quiet,LocalLayoutDirection provides LayoutDirection.Rtl){
        MaterialTheme(colorScheme=if(dark)darkColorScheme(primary=EzTokens.Accent,background=p.background,surface=p.surface,onSurface=p.ink) else lightColorScheme(primary=EzTokens.Accent,background=p.background,surface=p.surface,onSurface=p.ink),typography=Typography(bodyLarge=style,bodyMedium=style,bodySmall=style.copy(fontSize=12.sp),labelLarge=style.copy(fontWeight=FontWeight.SemiBold),titleLarge=style.copy(fontSize=24.sp,fontWeight=FontWeight.Bold),headlineLarge=style.copy(fontSize=33.sp,lineHeight=46.sp,fontWeight=FontWeight.Bold))){ProvideTextStyle(style.copy(color=p.ink),content)}
    }
}
@Composable fun EzIcon(id:Int,description:String?=null,size:Dp=24.dp,tint:Color=LocalEzPalette.current.ink){Icon(painterResource(id),description,Modifier.size(size),tint=tint)}
@Composable fun Modifier.ezPress(onClick:()->Unit,enabled:Boolean=true):Modifier{
    val source=remember{MutableInteractionSource()};val pressed by source.collectIsPressedAsState();val quiet=LocalEzQuiet.current
    val scale by animateFloatAsState(if(pressed&&enabled&&!quiet).96f else 1f,if(quiet)snap() else spring(dampingRatio=EzTokens.SpringDamping,stiffness=EzTokens.SpringStiffness),label="press")
    return graphicsLayer{scaleX=scale;scaleY=scale}.clickable(interactionSource=source,indication=null,enabled=enabled,onClick=onClick)
}
@Composable fun EzButton(text:String,onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,outline:Boolean=false,icon:Int?=null){
    val p=LocalEzPalette.current
    Row(modifier.clip(RoundedCornerShape(30.dp)).background(if(outline)p.surface else if(enabled)EzTokens.Ink else p.line).then(if(outline)Modifier.border(1.dp,p.line,RoundedCornerShape(30.dp)) else Modifier).ezPress(onClick,enabled).heightIn(min=48.dp).padding(horizontal=16.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){
        Text(text,color=if(outline)p.ink else if(enabled)Color.White else p.muted,fontSize=12.sp,fontWeight=FontWeight.SemiBold)
        if(icon!=null){Spacer(Modifier.width(12.dp));EzIcon(icon,size=18.dp,tint=if(outline)p.ink else Color.White)}
    }
}
@Composable fun EzRound(icon:Int,label:String,onClick:()->Unit,blue:Boolean=false){val p=LocalEzPalette.current;Box(Modifier.size(48.dp).clip(RoundedCornerShape(50)).background(if(p.dark)p.soft else Color(0xfffafafa)).border(1.dp,p.line,RoundedCornerShape(50)).ezPress(onClick),contentAlignment=Alignment.Center){EzIcon(icon,label,22.dp,if(blue)EzTokens.Accent else p.ink)}}
@Composable fun EzTag(text:String,icon:Int?=null,color:Color=Color.White.copy(alpha=.25f)) {Row(Modifier.clip(RoundedCornerShape(30.dp)).background(color).padding(horizontal=9.dp,vertical=3.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){if(icon!=null)EzIcon(icon,size=14.dp,tint=EzTokens.Ink);Text(text,fontSize=10.sp,lineHeight=17.sp,color=EzTokens.Ink,fontWeight=FontWeight.Medium)}}
@Composable fun EzBadge(text:String){val p=LocalEzPalette.current;Box(Modifier.clip(RoundedCornerShape(20.dp)).background(if(p.dark)Color(0xff393048) else Color(0xfff1eafe)).padding(horizontal=12.dp,vertical=7.dp)){Text(text,fontSize=10.sp,color=if(p.dark)Color(0xffdfcff9) else Color(0xff705694))};Spacer(Modifier.height(20.dp))}
@Composable fun EzNotice(text:String,warning:Boolean=false){val p=LocalEzPalette.current;Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(if(warning)Color(0xfffff0dc) else if(p.dark)p.soft else Color(0xffeef3fc)).padding(14.dp),horizontalArrangement=Arrangement.spacedBy(9.dp)){EzIcon(R.drawable.ic_warning_circle,size=19.dp,tint=if(warning)Color(0xff6f4110) else p.ink);Text(text,Modifier.weight(1f),fontSize=11.sp,lineHeight=21.sp,color=if(warning)Color(0xff6f4110) else p.ink)}}
@Composable fun EzEmpty(title:String,body:String,icon:Int=R.drawable.ic_folder_open,action:String?=null,onAction:()->Unit={}){val p=LocalEzPalette.current;Column(Modifier.fillMaxWidth().padding(vertical=20.dp).clip(RoundedCornerShape(28.dp)).background(if(p.dark)p.soft else Color(0xfff6f3fc)).padding(horizontal=20.dp,vertical=36.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(15.dp)){EzIcon(icon,size=38.dp);Text(title,fontSize=19.sp,fontWeight=FontWeight.SemiBold);Text(body,fontSize=12.sp,color=p.muted,style=LocalTextStyle.current.copy(textAlign=androidx.compose.ui.text.style.TextAlign.Center));if(action!=null)EzButton(action,onAction,outline=true)}}
@Composable fun EzAvatar(id:Int=0,size:Dp=53.dp){val colors=listOf(EzTokens.CategoryBlue,EzTokens.CategoryPurple,EzTokens.Peach,EzTokens.CategoryPink);val icons=listOf(R.drawable.ic_android_logo,R.drawable.ic_planet,R.drawable.ic_rocket,R.drawable.ic_headphones);Box(Modifier.size(size).clip(RoundedCornerShape(19.dp)).background(colors[id.coerceIn(0,3)]),contentAlignment=Alignment.Center){EzIcon(icons[id.coerceIn(0,3)],size=size*.53f,tint=Color(0xff192633))}}
@Composable fun EzDevice(name:String,subtitle:String,avatar:Int=0){Row(Modifier.fillMaxWidth().padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(13.dp)){EzAvatar(avatar);Column(Modifier.weight(1f)){Text(subtitle,fontSize=10.sp,color=LocalEzPalette.current.muted);Text(name,fontSize=14.sp,fontWeight=FontWeight.SemiBold)}}}
@Composable fun EzSection(title:String,action:String?=null,onAction:()->Unit={},icon:Int?=null){Row(Modifier.fillMaxWidth().padding(top=20.dp,bottom=10.dp).heightIn(min=28.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text(title,fontSize=19.sp,fontWeight=FontWeight.SemiBold);if(action!=null)TextButton(onClick=onAction){Text(action,fontSize=10.sp,color=LocalEzPalette.current.muted);Spacer(Modifier.width(5.dp));EzIcon(R.drawable.ic_arrow_up_left,size=14.dp)} else if(icon!=null)EzIcon(icon,size=20.dp)}}
