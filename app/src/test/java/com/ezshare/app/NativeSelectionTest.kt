package com.ezshare.app

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.ezshare.core.Metadata
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk=[35],qualifiers="ar-rSA-w412dp-h892dp-xxhdpi")
class NativeSelectionTest{
 @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
 @Test fun selectionPersistsAcrossCategoriesAndStartsRealDiscoveryCallback(){
  val image=LocalFile(Uri.parse("content://fixture/image"),Metadata("صورة.jpg","image/jpeg",1024))
  val doc=LocalFile(Uri.parse("content://fixture/document"),Metadata("مستند.pdf","application/pdf",1_000_000_000))
  var selection by mutableStateOf(emptyList<PickedFile>());var discovers=0
  compose.setContent{EzTheme(quiet=true){EzApp(TransferUi(selected=selection,picked=selection.firstOrNull()),EzPreferences(),emptyList(),emptyList(),initialDestination="files",catalog=CatalogState(listOf(image,doc)),onToggle={f->selection=if(selection.any{it.uri==f.uri})selection.filter{it.uri!=f.uri} else selection+PickedFile(f.uri,f.metadata)},onDiscover={discovers++})}}
  compose.onNodeWithText("صورة.jpg").performScrollTo().performClick()
  compose.onNodeWithText("1 ملفات محددة").assertExists()
  compose.onNodeWithText("المستندات",useUnmergedTree=true).performScrollTo().performClick()
  compose.onNodeWithText("مستند.pdf").performScrollTo().performClick()
  compose.onNodeWithText("2 ملفات محددة").assertExists()
  assertEquals(2,selection.size)
  compose.onNodeWithText("إرسال الملفات").assertIsEnabled().performClick()
  compose.onNodeWithText("مين نشارك معه؟").assertExists();assertEquals(1,discovers)
 }
}
