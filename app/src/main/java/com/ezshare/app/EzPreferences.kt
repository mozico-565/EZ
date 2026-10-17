package com.ezshare.app

import android.content.Context
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class EzPreferences(val name:String=Build.MODEL.take(28),val avatar:Int=0,val theme:String="light",val quiet:Boolean=false)
data class HistoryEntry(val id:String=UUID.randomUUID().toString(),val name:String,val bytes:Long,val total:Long,val duration:Double,val sha:String="",val peer:String,val incoming:Boolean,val status:String,val date:Long=System.currentTimeMillis(),val savedAt:String="")
class EzLocalStore(context:Context){
    private val prefs=context.getSharedPreferences("ez-preferences",Context.MODE_PRIVATE)
    fun read()=EzPreferences(prefs.getString("name",Build.MODEL.take(28)).orEmpty(),prefs.getInt("avatar",0),prefs.getString("theme","light").orEmpty(),prefs.getBoolean("quiet",false))
    fun save(p:EzPreferences){prefs.edit().putString("name",p.name).putInt("avatar",p.avatar).putString("theme",p.theme).putBoolean("quiet",p.quiet).apply()}
    fun history():List<HistoryEntry> = runCatching {val a=JSONArray(prefs.getString("history","[]"));(0 until a.length()).map{val j=a.getJSONObject(it);HistoryEntry(j.getString("id"),j.getString("name"),j.getLong("bytes"),j.getLong("total"),j.getDouble("duration"),j.optString("sha"),j.getString("peer"),j.getBoolean("incoming"),j.getString("status"),j.getLong("date"),j.optString("savedAt"))}}.getOrDefault(emptyList())
    fun saveHistory(rows:List<HistoryEntry>){val a=JSONArray();rows.take(100).forEach{r->a.put(JSONObject().put("id",r.id).put("name",r.name).put("bytes",r.bytes).put("total",r.total).put("duration",r.duration).put("sha",r.sha).put("peer",r.peer).put("incoming",r.incoming).put("status",r.status).put("date",r.date).put("savedAt",r.savedAt))};prefs.edit().putString("history",a.toString()).apply()}
}
