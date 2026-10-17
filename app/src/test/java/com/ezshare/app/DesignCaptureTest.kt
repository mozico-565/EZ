package com.ezshare.app

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.unit.LayoutDirection
import com.ezshare.core.Metadata
import com.ezshare.core.Progress
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Native Android/Compose rendering fixtures, never runtime sample files or benchmarks. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk=[35],qualifiers="ar-rSA-w412dp-h892dp-xxhdpi")
class DesignCaptureTest{
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private fun render(screen:String="home",dark:Boolean=false,state:TransferUi=TransferUi(),sheet:String="",fontScale:Float=1f){
        compose.activity.enableEdgeToEdge()
        compose.setContent{CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,fontScale)){EzTheme(dark=dark,quiet=true){EzApp(state,EzPreferences(name="جهازي · EZ"),emptyList(),emptyList(),initialDestination=screen,initialSheet=sheet)}}}
        compose.waitForIdle()
    }
    private fun shot(name:String){compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")}
    @Test fun home(){render();compose.onNodeWithText("نشارك؟").assertIsDisplayed();val layouts=mutableListOf<TextLayoutResult>();compose.onNodeWithText("ملفاتك توصل.\nبلمح البصر.").performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(layouts)};assertEquals(2,layouts.single().lineCount);assertFalse(layouts.single().didOverflowWidth);shot("home-light-412")}
    @Test fun homeDark(){render(dark=true);shot("home-dark-412")}
    @Test @Config(qualifiers="ar-rSA-w360dp-h800dp-xhdpi") fun compactHome(){render();shot("home-light-360");compose.onNodeWithText("استقبال").assertIsDisplayed()}
    @Test fun largeFontHome(){render(fontScale=1.5f);shot("home-font150-412")}
    @Test fun files(){render("files");shot("files-empty-412")}
    @Test fun selectedLongFilename(){render("files",state=TransferUi(picked=PickedFile(Uri.parse("content://capture/test"),Metadata("غروب الشمس على شاطئ البحر — النسخة الأصلية عالية الدقة.jpg","image/jpeg",6_400_000_000))));shot("files-selected-long-name-412")}
    @Test fun receive(){render("receive");shot("receive-412")}
    @Test fun discover(){render("discover");shot("discover-empty-412")}
    @Test fun scannerUnavailable(){render("scanner");shot("scanner-coming-soon-412")}
    @Test fun confirmation(){render("confirm",state=TransferUi(request=PairRequest("هاتف الاختبار","482716",Metadata("صورة الاختبار.jpg","image/jpeg",4200000),true)));shot("confirm-412")}
    @Test fun transferFixture(){render("transfer",state=TransferUi(filename="ملف اختبار بصري.jpg",peer="جهاز اختبار بصري",busy=true,progress=Progress(2_100_000,4_200_000,1_000_000_000,0.0)));shot("transfer-ui-fixture-412")}
    @Test fun transferFailure(){render("transfer",state=TransferUi(filename="فيديو لم يكتمل.mp4",error="انقطع الاتصال. لم يكتمل الملف.",status="توقفت العملية",peer="جهاز اختبار",progress=Progress(1200,4200000,1_000_000_000,0.0)));shot("transfer-failure-412")}
    @Test fun history(){render("history");shot("history-empty-412")}
    @Test fun settings(){render("settings");shot("settings-light-412")}
    @Test fun settingsDark(){render("settings",dark=true);shot("settings-dark-412")}
    @Test fun shareSheet(){render(sheet="share");shot("share-sheet-412")}
    @Test fun navigationAndSearchWork(){render();compose.onNodeWithContentDescription("الملفات").performClick();compose.waitForIdle();compose.onNodeWithText("أشياء تستحق\nالمشاركة.").assertExists();compose.onNode(hasSetTextAction()).performTextInput("لا يوجد");compose.onNodeWithText("لا توجد نتائج").assertExists();compose.onNodeWithContentDescription("مسح البحث").performClick();compose.onNodeWithContentDescription("السجل").performClick();compose.onNodeWithText("أول مشاركة تبدأ منك").assertExists()}
    @Test fun iconUsesApprovedAspect(){val c=org.robolectric.RuntimeEnvironment.getApplication();val d=c.resources.getDrawable(R.drawable.launcher_foreground,c.theme);assertEquals(d.intrinsicWidth,d.intrinsicHeight);assertEquals("EZ",c.applicationInfo.loadLabel(c.packageManager).toString())}
    @Test fun beforeAlphaHome(){compose.activity.enableEdgeToEdge();compose.setContent{MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xff2385e8),background=Color.White,surface=Color.White,onSurface=Color(0xff111111))){CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl,LocalDensity provides Density(LocalDensity.current.density,1f)){LegacyAlphaScreen()}}};compose.waitForIdle();shot("before-alpha-home-412")}
}
